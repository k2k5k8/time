package com.cch.momentmark.ui.recyclebin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.data.settings.MomentMarkSettingsStorePort
import com.cch.momentmark.domain.model.RecycleBinItem
import com.cch.momentmark.domain.model.RecycleBinItemType
import com.cch.momentmark.domain.model.toRecycleBinItem
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val RecycleRetention: Duration = Duration.ofDays(30)

data class RecycleBinUiState(
    val items: List<RecycleBinItem> = emptyList(),
    val autoPurgeEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * P0 封印生命周期的协调层。Moment/Task 保持独立表，本 VM 只做回收站投影与操作分派。
 * 自动净化只在 VM 随 App 创建时执行一次，绝不注册后台任务。
 */
class RecycleBinViewModel(
    private val momentRepository: MomentRepositoryPort,
    private val taskRepository: TaskRepositoryPort,
    private val settingsStore: MomentMarkSettingsStorePort,
    private val clock: Clock,
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                momentRepository.observeDeleted(),
                taskRepository.observeDeleted(),
                settingsStore.autoPurgeEnabled,
            ) { moments, tasks, autoPurgeEnabled ->
                RecycleBinUiState(
                    items = (moments.map { it.toRecycleBinItem() } + tasks.map { it.toRecycleBinItem() })
                        .sortedByDescending(RecycleBinItem::deletedAt),
                    autoPurgeEnabled = autoPurgeEnabled,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch {
            if (settingsStore.autoPurgeEnabled.first()) purgeExpiredAtStartup()
        }
    }

    fun restore(item: RecycleBinItem) = mutate {
        when (item.type) {
            RecycleBinItemType.MOMENT -> momentRepository.restore(item.id)
            RecycleBinItemType.TASK -> taskRepository.restore(item.id)
        }
    }

    fun restore(type: RecycleBinItemType, id: String) = mutate {
        when (type) {
            RecycleBinItemType.MOMENT -> momentRepository.restore(id)
            RecycleBinItemType.TASK -> taskRepository.restore(id)
        }
    }

    fun permanentlyDelete(item: RecycleBinItem) = mutate { permanentlyDeleteInternal(item) }

    fun purgeAll() = mutate {
        for (item in _uiState.value.items) {
            permanentlyDeleteInternal(item)
        }
    }

    fun setAutoPurgeEnabled(enabled: Boolean) = mutate {
        settingsStore.setAutoPurgeEnabled(enabled)
    }

    /** 供详情确认弹窗复用，成功后可直接展示全局撤销条。 */
    fun sealMoment(id: String) = mutate { momentRepository.softDelete(id) }

    fun sealTask(id: String) = mutate { taskRepository.softDelete(id) }

    private suspend fun purgeExpiredAtStartup() {
        val cutoff = clock.instant().minus(RecycleRetention)
        momentRepository.purgeDeletedBefore(cutoff.toEpochMilli())
        taskRepository.purgeDeletedBefore(cutoff.toEpochMilli())
    }

    private suspend fun permanentlyDeleteInternal(item: RecycleBinItem) {
        when (item.type) {
            RecycleBinItemType.MOMENT -> momentRepository.permanentlyDelete(item.id)
            RecycleBinItemType.TASK -> taskRepository.permanentlyDelete(item.id)
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { throwable ->
                _uiState.update { state -> state.copy(errorMessage = throwable.message ?: "封印之地操作失败") }
            }
        }
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }
}

class RecycleBinViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RecycleBinViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return RecycleBinViewModel(
            momentRepository = container.momentRepository,
            taskRepository = container.taskRepository,
            settingsStore = container.settingsStore,
            clock = container.clock,
            onClearedCallback = container::close,
        ) as T
    }
}
