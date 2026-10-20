package com.cch.momentmark.ui.daybook

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.CalendarDaySummary
import com.cch.momentmark.domain.model.CalendarSummaryRange
import com.cch.momentmark.domain.model.projectCalendarSummary
import com.cch.momentmark.ui.components.DateWheelSelection
import com.cch.momentmark.ui.components.dateWheelYearRange
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class PendingTaskAction { TODAY, TOMORROW, COMPLETE }

data class DaybookUiState(
    val selectedDate: LocalDate,
    val today: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.from(selectedDate),
    val tasks: List<Task> = emptyList(),
    val taskDates: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = true,
    val taskRequestRevision: Long = 0,
    val errorMessage: String? = null,
    val dayIntent: Int = selectedDate.dayOfMonth,
    val yearRange: IntRange = dateWheelYearRange(today, selectedDate),
    val tasksDate: LocalDate = selectedDate,
    val datePickerSelection: DateWheelSelection? = null,
    val hasLoadedTasks: Boolean = !isLoading,
    val loadError: String? = null,
    val monthPreviews: Map<LocalDate, DaybookTaskPreview> = emptyMap(),
    val allTaskDates: Set<LocalDate> = taskDates,
    val calendarMonths: Map<YearMonth, CalendarMonthState> = emptyMap(),
    val pendingTasks: List<Task> = emptyList(),
    val moveUndoAvailable: Boolean = false,
    val pendingLoaded: Boolean = !isLoading,
    val pendingLoadError: String? = null,
    val pendingBusy: Set<String> = emptySet(),
    val completionBusy: Set<String> = emptySet(),
    val pendingErrors: Map<String, String> = emptyMap(),
    val repeatSeries: List<com.cch.momentmark.data.repository.TaskRepeatRepository.Series> = emptyList(),
    val hasLoadedRepeatSeries: Boolean = !isLoading,
)

data class CalendarMonthState(val days: Map<LocalDate, CalendarDaySummary>? = null, val loading: Boolean = true, val error: String? = null)

private fun CalendarSummaryRange.monthStates() = days.entries.groupBy { YearMonth.from(it.key) }
    .mapValues { (_, entries) -> CalendarMonthState(entries.associate { it.toPair() }, loading = false) }

data class DaybookTaskPreview(val tasks: List<Task>?, val error: String? = null)

/** 日子簿只读 Task Repository；切日不会迁移或修改任何待办。 */
@OptIn(ExperimentalCoroutinesApi::class)
class DaybookViewModel(
    private val repository: TaskRepositoryPort,
    private val clock: Clock,
    private val fixtureToday: LocalDate? = null,
    private val fixtureTasks: List<Task>? = null,
    private val onClearedCallback: (() -> Unit)? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val today = fixtureToday ?: LocalDate.now(clock)
    private val visibleMonth = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow(
        fixtureToday ?: savedStateHandle.get<String>("daybook.date")
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today,
    )
    private data class DateRequest(val date: LocalDate, val revision: Long = 0)
    private val dateRequest = MutableStateFlow(DateRequest(selectedDate.value))
    private val calendarRevision = MutableStateFlow(0L)
    private val calendarRefreshAt = MutableStateFlow<java.time.Instant?>(null)
    private var projectedDates: Set<LocalDate> = emptySet()
    private var activeDates: Set<LocalDate> = emptySet()
    private var recordYears: Set<Int> = emptySet()
    private val _uiState = MutableStateFlow(
        DaybookUiState(
            selectedDate = selectedDate.value,
            today = today,
            visibleMonth = YearMonth.from(selectedDate.value),
            isLoading = fixtureTasks == null,
            dayIntent = savedStateHandle.get<Int>("daybook.intent")?.takeIf { it in 1..31 }
                ?: selectedDate.value.dayOfMonth,
            datePickerSelection = savedStateHandle.get<String>("daybook.picker.date")?.let { raw ->
                runCatching { DateWheelSelection(LocalDate.parse(raw), savedStateHandle.get<Int>("daybook.picker.intent") ?: LocalDate.parse(raw).dayOfMonth) }.getOrNull()
            },
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        if (fixtureToday != null && fixtureTasks != null) {
            _uiState.update { state ->
                state.copy(
                    selectedDate = fixtureToday,
                    today = fixtureToday,
                    visibleMonth = YearMonth.from(fixtureToday),
                    calendarMonths = projectCalendarSummary(YearMonth.from(fixtureToday), fixtureTasks, emptyList(), emptyList(), emptySet(), emptySet(), emptyMap(), clock).monthStates(),
                    tasks = fixtureTasks.filter { it.dueLocalDate == fixtureToday },
                    taskDates = fixtureTasks
                        .map(Task::dueLocalDate)
                        .filter { YearMonth.from(it) == state.visibleMonth }
                        .toSet(),
                    isLoading = false,
                    hasLoadedTasks = true,
                    allTaskDates = fixtureTasks.map(Task::dueLocalDate).toSet(),
                    monthPreviews = listOf(-1L, 1L).associate { delta ->
                        val adjacent = DateWheelSelection(fixtureToday, state.dayIntent)
                            .inMonth(YearMonth.from(fixtureToday).plusMonths(delta)).date
                        adjacent to DaybookTaskPreview(fixtureTasks.filter { it.dueLocalDate == adjacent })
                    },
                    errorMessage = null,
                )
            }
        } else {
            observePendingTasks()
            visibleMonth.value = _uiState.value.visibleMonth
            viewModelScope.launch { repository.observeRepeatSeries().collect { series -> _uiState.update { it.copy(repeatSeries = series, hasLoadedRepeatSeries = true) } } }
            viewModelScope.launch {
                combine(visibleMonth, calendarRevision) { month, revision -> month to revision }.flatMapLatest { request ->
                    repository.observeCalendarSummary(request.first)
                        .map { CalendarResult(request, it) }
                        .onStart { emit(CalendarResult(request)) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(CalendarResult(request, error = error.message ?: "日历摘要加载失败"))
                        }
                }.collect { result ->
                    if (result.request != (visibleMonth.value to calendarRevision.value)) return@collect
                    val month = result.request.first
                    val months = (-1L..1L).map { month.plusMonths(it) }
                    if (result.range != null) {
                        calendarRefreshAt.value = result.range.nextRefreshAt
                        projectedDates = result.range.days.filterValues { it.taskCount > 0 || it.movedCount > 0 }.keys
                    }
                    _uiState.update { state -> state.copy(
                        calendarMonths = months.associateWith { target ->
                            if (result.range != null) CalendarMonthState(result.range.days.filterKeys { YearMonth.from(it) == target }, false)
                            else (state.calendarMonths[target] ?: CalendarMonthState()).copy(loading = result.error == null, error = result.error)
                        },
                        taskDates = projectedDates.filter { YearMonth.from(it) == month }.toSet(),
                        allTaskDates = activeDates + projectedDates,
                    ) }
                }
            }
            viewModelScope.launch {
                // Same repository/DAO ordering as the current day. Never query or write from drawing.
                dateRequest.flatMapLatest { request ->
                    val anchor = DateWheelSelection(request.date, _uiState.value.dayIntent)
                    val dates = listOf(-1L, 1L).map { anchor.inMonth(YearMonth.from(request.date).plusMonths(it)).date }
                    fun preview(date: LocalDate) = repository.observeForDate(date)
                        .map { DaybookTaskPreview(it) }
                        .onStart { emit(DaybookTaskPreview(null)) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(DaybookTaskPreview(null, error.message ?: "任务预览加载失败"))
                        }
                    combine(preview(dates[0]), preview(dates[1])) { previous, next ->
                        request to mapOf(dates[0] to previous, dates[1] to next)
                    }
                }.collect { (request, previews) ->
                    if (request == dateRequest.value) _uiState.update { it.copy(monthPreviews = previews) }
                }
            }
            viewModelScope.launch {
                dateRequest.flatMapLatest { request ->
                    repository.observeForDate(request.date).map { DateResult(request, it) }.catch { error ->
                        if (error is CancellationException) throw error
                        emit(DateResult(request, emptyList(), error.message ?: "任务加载失败，请重试"))
                    }
                }.collect { result ->
                    _uiState.update { state ->
                        if (result.request != dateRequest.value || result.request.date != state.selectedDate) state else state.copy(
                            tasks = if (result.error == null) result.tasks else state.tasks,
                            tasksDate = if (result.error == null) result.request.date else state.tasksDate,
                            hasLoadedTasks = state.hasLoadedTasks || result.error == null,
                            loadError = result.error,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
            }
            viewModelScope.launch {
                combine(repository.observeActive(), repository.observeDeleted()) { active, deleted -> active to deleted }
                    .collect { (active, deleted) ->
                        activeDates = active.map(Task::dueLocalDate).toSet()
                        recordYears = (active + deleted).map { it.dueLocalDate.year }.toSet()
                        _uiState.update { state -> state.copy(
                            yearRange = dateWheelYearRange(state.today, state.selectedDate, recordYears),

                            taskDates = (activeDates + projectedDates).filter { YearMonth.from(it) == state.visibleMonth }.toSet(),
                            allTaskDates = activeDates + projectedDates,
                        ) }
                    }
            }
        }
    }

    private fun observePendingTasks() = viewModelScope.launch {
        repository.observePending().catch { error ->
            if (error is CancellationException) throw error
            _uiState.update { it.copy(pendingLoadError = error.message ?: "待安排加载失败") }
        }.collect { tasks -> _uiState.update { it.copy(pendingTasks = tasks, pendingLoaded = true, pendingLoadError = null) } }
    }

    fun retryPendingTasks() {
        if (_uiState.value.pendingLoadError == null) return
        _uiState.update { it.copy(pendingLoadError = null, pendingLoaded = false) }
        observePendingTasks()
    }

    /** A pending command is bound to its confirmed day and source record, never a browsing draft. */
    fun actOnPending(task: Task, action: PendingTaskAction, expectedToday: LocalDate) {
        val state = _uiState.value
        if (task.id in state.pendingBusy) return
        fun ready() = expectedToday == LocalDate.now(clock) &&
            _uiState.value.let { it.today == expectedToday && it.selectedDate == expectedToday &&
                it.tasksDate == expectedToday && !it.isLoading && it.loadError == null &&
                it.pendingLoaded && it.pendingLoadError == null }
        if (!ready()) return
        _uiState.update { it.copy(pendingBusy = it.pendingBusy + task.id, pendingErrors = it.pendingErrors - task.id) }
        viewModelScope.launch {
            try {
                val current = repository.findById(task.id) ?: error("任务已不存在，请重新查看")
                require(ready() && current.dueLocalDate == task.dueLocalDate &&
                    current.dueLocalDate < expectedToday && current.repeatRuleId == null &&
                    !current.isCompleted && current.deletedAt == null && current.movedTo == null && !current.isSkipped) {
                    "日期或任务已变化，请重新查看待安排"
                }
                if (action == PendingTaskAction.COMPLETE) repository.setCompleted(current.id, true)
                else {
                    val target = if (action == PendingTaskAction.TODAY) expectedToday else expectedToday.plusDays(1)
                    check(repository.moveTask(current.id, target)) { "任务未能移日，请重试" }
                    _uiState.update { it.copy(moveUndoAvailable = true) }
                }
                // Hold the row lock until the persisted result is observed. No optimistic removal.
                repository.observePending().first { rows -> rows.none { it.id == current.id && it.dueLocalDate < expectedToday } }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(pendingErrors = it.pendingErrors + (task.id to (error.message ?: "保存失败，请重试"))) }
            } finally {
                _uiState.update { it.copy(pendingBusy = it.pendingBusy - task.id) }
            }
        }
    }

    fun selectDate(date: LocalDate) = confirmDate(date, date.dayOfMonth)

    fun confirmDate(date: LocalDate, dayIntent: Int) {
        require(dayIntent in 1..31)
        require(date.dayOfMonth == dayIntent.coerceAtMost(YearMonth.from(date).lengthOfMonth()))
        val changed = date != selectedDate.value
        val intentChanged = dayIntent != _uiState.value.dayIntent
        val preview = _uiState.value.monthPreviews[date]?.tasks
        savedStateHandle["daybook.date"] = date.toString()
        savedStateHandle["daybook.intent"] = dayIntent
        _uiState.update { state -> state.copy(
            selectedDate = date, visibleMonth = YearMonth.from(date), dayIntent = dayIntent,
            taskRequestRevision = if (changed || intentChanged) dateRequest.value.revision + 1 else state.taskRequestRevision,
            yearRange = dateWheelYearRange(state.today, date, recordYears),
            taskDates = (fixtureTasks?.map(Task::dueLocalDate)?.toSet() ?: (activeDates + projectedDates))
                .filter { YearMonth.from(it) == YearMonth.from(date) }.toSet(),
            isLoading = if (changed) fixtureTasks == null else state.isLoading,
            errorMessage = if (changed) null else state.errorMessage,
            loadError = if (changed) null else state.loadError,
            tasks = if (changed && preview != null) preview else state.tasks,
            tasksDate = if (changed && preview != null) date else state.tasksDate,
            hasLoadedTasks = state.hasLoadedTasks || preview != null,
        ) }
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
        // A→B→A may be conflated to A; a new revision still restarts the latest query.
        if (changed || intentChanged) dateRequest.value = DateRequest(date, dateRequest.value.revision + 1)
        if (fixtureTasks != null) {
            _uiState.update { state ->
                state.copy(
                    calendarMonths = projectCalendarSummary(YearMonth.from(date), fixtureTasks, emptyList(), emptyList(), emptySet(), emptySet(), emptyMap(), clock).monthStates(),
                    tasks = fixtureTasks.filter { it.dueLocalDate == date },
                    tasksDate = date,
                    hasLoadedTasks = true,
                    isLoading = false,
                    allTaskDates = fixtureTasks.map(Task::dueLocalDate).toSet(),
                    monthPreviews = listOf(-1L, 1L).associate { delta ->
                        val adjacent = DateWheelSelection(date, dayIntent).inMonth(YearMonth.from(date).plusMonths(delta)).date
                        adjacent to DaybookTaskPreview(fixtureTasks.filter { it.dueLocalDate == adjacent })
                    },
                )
            }
        }
    }

    fun retryTasks() {
        if (_uiState.value.loadError == null || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, loadError = null, taskRequestRevision = dateRequest.value.revision + 1) }
        dateRequest.value = DateRequest(selectedDate.value, dateRequest.value.revision + 1)
    }

    fun changeMonth(delta: Long) {
        val targetMonth = runCatching { _uiState.value.visibleMonth.plusMonths(delta) }.getOrNull() ?: return
        val selection = DateWheelSelection(_uiState.value.selectedDate, _uiState.value.dayIntent).inMonth(targetMonth)
        confirmDate(selection.date, selection.intendedDay)
    }

    private data class CalendarResult(val request: Pair<YearMonth, Long>, val range: CalendarSummaryRange? = null, val error: String? = null)

    fun retryCalendar() { calendarRevision.value += 1 }

    fun refreshToday() {
        if (calendarRefreshAt.value?.let { clock.instant() >= it } == true) {
            calendarRefreshAt.value = null
            retryCalendar()
        }
        val now = fixtureToday ?: LocalDate.now(clock)
        _uiState.update { it.copy(today = now, yearRange = dateWheelYearRange(now, it.selectedDate, recordYears)) }
    }

    fun naturalDayCheckDelayMillis(): Long {
        val zones = _uiState.value.repeatSeries.map { it.rule.zoneId } + clock.zone
        val midnightDelay = zones.minOf { zone ->
            val current = clock.instant().atZone(zone)
            java.time.Duration.between(clock.instant(), current.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()).toMillis()
        }
        val riskDelay = calendarRefreshAt.value?.let { java.time.Duration.between(clock.instant(), it).toMillis() } ?: 60_000L
        return minOf(midnightDelay, riskDelay).coerceIn(1, 60_000)
    }
    /** A newly loaded deadline can be earlier than the sleep already in progress. */
    suspend fun awaitNaturalDayCheck() {
        val scheduled = calendarRefreshAt.value
        kotlinx.coroutines.withTimeoutOrNull(naturalDayCheckDelayMillis()) {
            calendarRefreshAt.first { it != scheduled }
        }
    }

    private var checkedDays: List<LocalDate>? = null
    suspend fun checkNaturalDay(force: Boolean = true) {
        refreshToday()
        if (force && fixtureTasks == null) retryCalendar()
        if (fixtureTasks != null) return
        val days = (_uiState.value.repeatSeries.map { it.rule.zoneId } + clock.zone).distinct().map { LocalDate.now(clock.withZone(it)) }
        if (!force && days == checkedDays) return
        try { repository.reconcileRepeats(); checkedDays = days } catch (error: Throwable) {
            if (error is CancellationException) throw error
            _uiState.update { it.copy(errorMessage = error.message ?: "重复任务更新失败，请重试") }
        }
    }
    private fun repeatAction(action: suspend () -> Unit) { _uiState.update { it.copy(errorMessage = null) }; viewModelScope.launch {
        try { action(); _uiState.update { it.copy(errorMessage = null) } } catch (error: Throwable) {
            if (error is CancellationException) throw error
            _uiState.update { it.copy(errorMessage = error.message ?: "重复任务保存失败") }
        }
    } }
    fun moveTask(task: Task, date: LocalDate) = repeatAction {
        val changed = repository.moveTask(task.id, date)
        _uiState.update { it.copy(moveUndoAvailable = changed) }
    }
    fun undoMove() = repeatAction { repository.undoTaskMove(); _uiState.update { it.copy(moveUndoAvailable = false) } }
    fun autoDefer(task: Task, enabled: Boolean) = repeatAction { repository.setAutoDefer(task.id, enabled) }
    fun makeUp(task: Task) = repeatAction { repository.makeUp(task.id) }
    fun skipRepeat(task: Task, skipped: Boolean) = repeatAction { repository.skipRepeat(task.id, skipped) }
    fun pauseRepeat(id: String, stop: Boolean) = repeatAction { repository.pauseRepeat(id, stop) }
    fun resumeRepeat(id: String) = repeatAction { repository.resumeRepeat(id) }
    fun editRepeatFuture(rule: com.cch.momentmark.domain.model.TaskRepeatRule, onSaved: () -> Unit = {}) = repeatAction { repository.editRepeatFuture(rule); onSaved() }

    fun selectToday() { refreshToday(); selectDate(_uiState.value.today) }

    /** Saved UI state only: a draft never changes selectedDate or performs a task write. */
    fun updateDatePicker(selection: DateWheelSelection?) {
        if (selection == null) {
            savedStateHandle.remove<String>("daybook.picker.date")
            savedStateHandle.remove<Int>("daybook.picker.intent")
        } else {
            savedStateHandle["daybook.picker.date"] = selection.date.toString()
            savedStateHandle["daybook.picker.intent"] = selection.intendedDay
        }
        _uiState.update { it.copy(datePickerSelection = selection) }
    }

    private data class DateResult(val request: DateRequest, val tasks: List<Task>, val error: String? = null)

    fun toggleCompleted(task: Task) = requestCompletion(task) { }

    fun requestCompletion(task: Task, onResult: (Boolean) -> Unit) {
        if (task.id in _uiState.value.completionBusy) return
        _uiState.update { it.copy(errorMessage = null, completionBusy = it.completionBusy + task.id) }
        viewModelScope.launch {
            var succeeded = false
            try {
                repository.setCompleted(task.id, !task.isCompleted)
                succeeded = true
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(errorMessage = throwable.message ?: "任务状态保存失败") }
            } finally {
                _uiState.update { it.copy(completionBusy = it.completionBusy - task.id) }
                onResult(succeeded)
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

class DaybookViewModelFactory(
    context: Context,
    private val fixtureToday: LocalDate? = null,
    private val fixtureTasks: List<Task>? = null,
) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return build(modelClass, SavedStateHandle())
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        build(modelClass, extras.createSavedStateHandle())

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> build(modelClass: Class<T>, state: SavedStateHandle): T {
        require(modelClass.isAssignableFrom(DaybookViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer.get(applicationContext)
        return DaybookViewModel(
            repository = container.taskRepository,
            clock = container.clock,
            fixtureToday = fixtureToday,
            fixtureTasks = fixtureTasks,
            savedStateHandle = state,
        ) as T
    }
}
