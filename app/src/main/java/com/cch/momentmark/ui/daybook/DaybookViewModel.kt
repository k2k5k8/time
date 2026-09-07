package com.cch.momentmark.ui.daybook

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DaybookUiState(
    val selectedDate: LocalDate,
    val today: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.from(selectedDate),
    val tasks: List<Task> = emptyList(),
    val taskDates: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** 日子簿只读 Task Repository；切日不会迁移或修改任何待办。 */
@OptIn(ExperimentalCoroutinesApi::class)
class DaybookViewModel(
    private val repository: TaskRepositoryPort,
    private val clock: Clock,
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val _uiState = MutableStateFlow(DaybookUiState(selectedDate = selectedDate.value, today = selectedDate.value))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedDate.flatMapLatest { date ->
                repository.observeForDate(date).combine(repository.observeActive()) { tasks, activeTasks ->
                    tasks to activeTasks
                }
            }.collect { (tasks, activeTasks) ->
                val month = _uiState.value.visibleMonth
                _uiState.update { state ->
                    state.copy(
                        selectedDate = selectedDate.value,
                        tasks = tasks,
                        taskDates = activeTasks.asSequence()
                            .map(Task::dueLocalDate)
                            .filter { YearMonth.from(it) == month }
                            .toSet(),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, visibleMonth = YearMonth.from(date), isLoading = true) }
        selectedDate.value = date
    }

    fun changeMonth(delta: Long) {
        val targetMonth = _uiState.value.visibleMonth.plusMonths(delta)
        val oldDay = _uiState.value.selectedDate.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth())
        selectDate(targetMonth.atDay(oldDay))
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            try {
                repository.setCompleted(task.id, !task.isCompleted)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(errorMessage = throwable.message ?: "任务状态保存失败") }
            }
        }
    }

    fun seal(task: Task, onSealed: (Task) -> Unit) {
        viewModelScope.launch {
            try {
                repository.softDelete(task.id)
                onSealed(task)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(errorMessage = throwable.message ?: "放弃任务失败") }
            }
        }
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }
}

class DaybookViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DaybookViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return DaybookViewModel(
            repository = container.taskRepository,
            clock = container.clock,
            onClearedCallback = container::close,
        ) as T
    }
}
