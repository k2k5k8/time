package com.cch.momentmark.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.repository.TimeEventMapper.toDomain
import com.cch.momentmark.data.repository.TimeEventRepositoryPort
import com.cch.momentmark.data.settings.MomentMarkGroupStorePort
import com.cch.momentmark.data.settings.MomentMarkSettingsStorePort
import com.cch.momentmark.data.settings.EventDetailStore
import com.cch.momentmark.data.SampleEvents
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.theme.ThemeMode
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MomentMarkAppViewModel(
    private val eventRepository: TimeEventRepositoryPort,
    private val settingsStore: MomentMarkSettingsStorePort,
    private val groupStore: MomentMarkGroupStorePort,
    private val seedEvents: List<TimeEvent> = SampleEvents.all,
    val detailStore: EventDetailStore? = null,
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MomentMarkAppUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeAppState()
    }

    private fun observeAppState() {
        viewModelScope.launch {
            try {
                eventRepository.seedIfEmpty(seedEvents)
                combine(
                    eventRepository.observeActive(),
                    eventRepository.observeDeleted(),
                    settingsStore.themeMode,
                    groupStore.groups,
                ) { entities, deletedEntities, themeMode, groups ->
                    MomentMarkAppUiState(
                        events = entities.map { it.toDomain() },
                        deletedEvents = deletedEntities.map { it.toDomain() },
                        themeMode = themeMode,
                        groups = groups,
                        isLoading = false,
                    )
                }
                    .catch { throwable ->
                        if (throwable is CancellationException) throw throwable
                        _uiState.update { it.withError(throwable) }
                    }
                    .collect { state -> _uiState.value = state }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.withError(throwable) }
            }
        }
    }

    fun saveEvent(event: TimeEvent): Job = launchMutation {
        eventRepository.save(event)
    }

    fun setPinned(id: String, pinned: Boolean): Job = launchMutation {
        eventRepository.setPinned(id, pinned, System.currentTimeMillis())
    }

    fun archiveEvent(id: String): Job = launchMutation {
        eventRepository.setArchived(id, archived = true, updatedAt = System.currentTimeMillis())
    }

    fun softDeleteEvent(id: String): Job = launchMutation {
        val deletedAt = System.currentTimeMillis()
        eventRepository.softDelete(id, deletedAt = deletedAt, updatedAt = deletedAt)
    }

    fun restoreDeleted(id: String): Job = launchMutation {
        eventRepository.restoreDeleted(id, updatedAt = System.currentTimeMillis())
    }

    fun permanentlyDelete(id: String): Job = launchMutation {
        eventRepository.permanentlyDelete(id)
    }

    fun purgeDeleted(): Job = launchMutation {
        eventRepository.purgeDeleted()
    }

    fun setThemeMode(mode: ThemeMode): Job = launchMutation {
        settingsStore.setThemeMode(mode)
    }

    fun createGroup(name: String): Job = launchMutation {
        val updated = (groupStore.groups.first() + name).distinct()
        groupStore.saveGroups(updated)
    }

    fun renameGroup(oldName: String, newName: String): Job = launchMutation {
        val updated = groupStore.groups.first()
            .map { if (it == oldName) newName else it }
            .distinct()
        eventRepository.renameGroup(oldName, newName, System.currentTimeMillis())
        groupStore.saveGroups(updated)
    }

    fun deleteGroup(name: String): Job = launchMutation {
        val updated = groupStore.groups.first().filterNot { it == name }
        eventRepository.clearGroup(name, System.currentTimeMillis())
        groupStore.saveGroups(updated)
    }

    private fun launchMutation(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
            _uiState.update { it.copy(errorMessage = null) }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            _uiState.update { it.withError(throwable) }
        }
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }

    private fun MomentMarkAppUiState.withError(throwable: Throwable): MomentMarkAppUiState = copy(
        isLoading = false,
        errorMessage = throwable.message ?: throwable::class.simpleName ?: "加载失败",
    )
}
