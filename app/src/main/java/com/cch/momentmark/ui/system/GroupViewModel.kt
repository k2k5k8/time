package com.cch.momentmark.ui.system

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.Group
import com.cch.momentmark.data.repository.GroupRepositoryPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupUiState(val groups: List<Group> = emptyList(), val errorMessage: String? = null)

class GroupViewModel(private val repository: GroupRepositoryPort, private val onCleared: (() -> Unit)? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState = _uiState.asStateFlow()
    init { viewModelScope.launch { repository.observeAll().collect { groups -> _uiState.update { it.copy(groups = groups) } } } }
    fun create(name: String) = viewModelScope.launch { repository.create(name).onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } } }
    fun rename(id: String, name: String) = viewModelScope.launch { repository.rename(id, name).onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } } }
    fun dissolve(id: String) = viewModelScope.launch { repository.dissolve(id).onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } } }
    fun reorder(ids: List<String>) = viewModelScope.launch { repository.reorder(ids).onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } } }
    override fun onCleared() { onCleared?.invoke(); super.onCleared() }
}

class GroupViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GroupViewModel::class.java))
        val container = AppContainer(appContext)
        @Suppress("UNCHECKED_CAST")
        return GroupViewModel(container.groupRepository, container::close) as T
    }
}
