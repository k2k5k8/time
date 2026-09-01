package com.cch.momentmark.ui.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskFormUiState(
    /** null = 新建；非空 = 编辑已有 Task，保存必须保留任务完成与生命周期事实。 */
    val editingTaskId: String? = null,
    val title: String = "",
    val dueDateText: String,
    /** 由注入 Clock 派生，仅用于快捷日期的选中态；不写入 Task。 */
    val quickTodayDateText: String,
    val quickTomorrowDateText: String,
    val quickSaturdayDateText: String,
    val dueTimeText: String = "",
    val note: String = "",
    val taskType: TaskType? = TaskType.SIDE,
    val difficulty: Int? = null,
    val groupId: String = "",
    val showOnHome: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedDueDate: LocalDate? = null,
)

/** 唯一的新任务写入边界；默认今天，不允许从首页/新时刻调用。 */
class TaskFormViewModel(
    private val repository: TaskRepositoryPort,
    private val clock: Clock,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {
    private fun emptyState(): TaskFormUiState {
        val today = LocalDate.now(clock)
        val thisSaturday = today.plusDays(
            ((java.time.DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7).toLong(),
        )
        return TaskFormUiState(
            dueDateText = today.toString(),
            quickTodayDateText = today.toString(),
            quickTomorrowDateText = today.plusDays(1).toString(),
            quickSaturdayDateText = thisSaturday.toString(),
        )
    }

    private val _uiState = MutableStateFlow(emptyState())
    val uiState = _uiState.asStateFlow()

    /** 每次进入表单都清空新建状态或按 ID 预填编辑事实字段。 */
    fun start(taskId: String?) {
        if (taskId == null) {
            _uiState.value = emptyState()
            return
        }
        _uiState.value = emptyState().copy(editingTaskId = taskId, isLoading = true)
        viewModelScope.launch {
            try {
                val task = repository.findById(taskId)
                if (task == null || task.deletedAt != null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "这个任务不存在，或已被封印。") }
                    return@launch
                }
                _uiState.value = emptyState().copy(
                    editingTaskId = task.id,
                    title = task.title,
                    dueDateText = task.dueLocalDate.toString(),
                    dueTimeText = task.dueInstant?.atZone(task.zoneId ?: clock.zone)?.toLocalTime()?.toString().orEmpty(),
                    note = task.note,
                    taskType = task.taskType,
                    difficulty = task.difficulty,
                    groupId = task.groupId.orEmpty(),
                    showOnHome = task.showOnHome,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "任务加载失败，请重试") }
            }
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun updateDate(value: String) = _uiState.update { it.copy(dueDateText = value, errorMessage = null) }
    fun updateTime(value: String) = _uiState.update { it.copy(dueTimeText = value, errorMessage = null) }
    fun updateNote(value: String) = _uiState.update { it.copy(note = value, errorMessage = null) }
    fun updateGroup(value: String) = _uiState.update { it.copy(groupId = value, errorMessage = null) }
    fun selectType(value: TaskType) = _uiState.update { it.copy(taskType = value, errorMessage = null) }
    fun selectDifficulty(value: Int?) {
        require(value == null || value in 1..3) { "difficulty must be null or within 1..3" }
        _uiState.update { it.copy(difficulty = value, errorMessage = null) }
    }
    fun toggleShowOnHome() = _uiState.update { it.copy(showOnHome = !it.showOnHome) }

    fun selectToday() = updateDate(LocalDate.now(clock).toString())
    fun selectTomorrow() = updateDate(LocalDate.now(clock).plusDays(1).toString())
    fun selectThisSaturday() {
        val today = LocalDate.now(clock)
        val days = (java.time.DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7
        updateDate(today.plusDays(days.toLong()).toString())
    }

    fun submit() {
        val state = _uiState.value
        val date = runCatching { LocalDate.parse(state.dueDateText.trim()) }.getOrNull()
        val error = validate(state, date)
        if (error != null || date == null) {
            _uiState.update { it.copy(errorMessage = error ?: "日期格式为 YYYY-MM-DD。") }
            return
        }
        val dueInstant = state.dueTimeText.trim().takeIf(String::isNotEmpty)?.let { raw ->
            LocalDateTime.of(date, LocalTime.parse(raw)).atZone(clock.zone).toInstant()
        }
        val editingId = state.editingTaskId
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val existing = if (editingId == null) null else repository.findById(editingId)
                if (editingId != null && (existing == null || existing.deletedAt != null)) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "这个任务不存在，或已被封印。") }
                    return@launch
                }
                val now = Instant.ofEpochMilli(clock.millis())
                repository.save(
                    Task(
                        id = existing?.id ?: idGenerator(),
                        title = state.title.trim(),
                        dueLocalDate = date,
                        dueInstant = dueInstant,
                        zoneId = dueInstant?.let { clock.zone } ?: existing?.zoneId,
                        note = state.note.trim(),
                        taskType = state.taskType,
                        difficulty = state.difficulty,
                        groupId = state.groupId.trim().ifEmpty { null },
                        isCompleted = existing?.isCompleted ?: false,
                        completedAt = existing?.completedAt,
                        showOnHome = state.showOnHome,
                        deletedAt = existing?.deletedAt,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = existing?.updatedAt ?: now,
                    ),
                )
                _uiState.update { it.copy(isSaving = false, savedDueDate = date) }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message ?: "接受任务失败，请重试") }
            }
        }
    }

    fun consumeSaved() {
        if (_uiState.value.savedDueDate != null) _uiState.value = emptyState()
    }

    private fun validate(state: TaskFormUiState, date: LocalDate?): String? {
        if (state.title.isBlank()) return "请填写任务名称。"
        if (date == null) return "日期格式为 YYYY-MM-DD。"
        if (state.dueTimeText.isNotBlank() && runCatching { LocalTime.parse(state.dueTimeText.trim()) }.isFailure) {
            return "截止时间格式为 HH:MM。"
        }
        return null
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }
}

class TaskFormViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskFormViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return TaskFormViewModel(
            repository = container.taskRepository,
            clock = container.clock,
            onClearedCallback = container::close,
        ) as T
    }
}
