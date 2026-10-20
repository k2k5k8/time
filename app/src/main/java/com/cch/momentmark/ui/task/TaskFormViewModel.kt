package com.cch.momentmark.ui.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.cch.momentmark.ui.components.DayOffsetSelection
import com.cch.momentmark.ui.components.TaskDateInputSelection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.BuildConfig
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.*
import java.time.DayOfWeek
import java.time.ZoneId
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

enum class TaskArrangement { ANYTIME, PERIOD, TIME }
enum class TaskAdvancedEditor { DEADLINE, REPEAT }

/** 计划所属日、执行安排与独立截止；高级规则按需启用。 */
data class TaskFormUiState(
    /** null = 新建；非空 = 编辑已有 Task，保存必须保留任务完成与生命周期事实。 */
    val editingTaskId: String? = null,
    val title: String = "",
    // The supplied date and last confirmed arrangement are already valid.
    val dateConfirmed: Boolean = true,
    /** 展开的编辑分支；不参与有效安排或保存校验。 */
    val arrangementEditor: TaskArrangement? = null,
    val extrasAvailable: Boolean = false,
    val extrasOpen: Boolean = false,
    val advancedOpen: Boolean = false,
    val activeEditor: TaskAdvancedEditor? = null,
    /** UI 可见日期（"2026.09.18"），提交时解析回 LocalDate；与时间字段独立。 */
    val dueDateText: String,
    /** 由注入 Clock 派生，仅用于快捷日期选中态；不写入 Task。 */
    val quickTodayDateText: String,
    val quickTomorrowDateText: String,
    val quickSaturdayDateText: String,
    val quickAfterTomorrowDateText: String = quickTomorrowDateText,
    /** 入口浏览日独立于计划日；所有退出路径恢复该日及其月份。 */
    val originDate: LocalDate? = null,
    val dayOffsetDraft: DayOffsetSelection? = null,
    val dateInputDraft: TaskDateInputSelection? = null,
    /** 独立截止时间；启用截止时必须显式填写。 */
    val dueTimeText: String = "",
    val deadlineDateText: String = dueDateText,
    val deadlineEnabled: Boolean = false,
    val deadlineDraftStarted: Boolean = false,
    /** 截止最后确认的作用域；切换单次/重复后需明确转换，不能静默复制。 */
    val deadlineRepeating: Boolean = false,
    val planPeriod: TaskPlanPeriod = TaskPlanPeriod.ANYTIME,
    val planTimeText: String = "",
    val important: Boolean = false,
    val autoDefer: Boolean = false,
    val repeating: Boolean = false,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    /** 每周即使选满七天仍保留其编辑模式，随表单会话恢复。 */
    val customWeekdays: Boolean = false,
    val repeatEndText: String = "",
    val zoneId: ZoneId? = null,
    val repeatRuleId: String? = null,
    val note: String = "",
    /** null = 未选难度；有效值 1..3。其它字段（任务类型 / 分组 / 显示在首页）由设计稿第 ④ 屏未展示，
     *  本轮仅保留在数据层，由 ViewModel 用默认值落库，UI 不展示。 */
    val taskType: TaskType? = TaskType.SIDE,
    val difficulty: Int? = null,
    val groupId: String = "",
    val showOnHome: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** 成功后锁定此表单会话，直到下一次 start；事件消费不会重新开放提交。 */
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val savedDueDate: LocalDate? = null,
    val savedTaskId: String? = null,
) : java.io.Serializable {
    val arrangement: TaskArrangement
        get() = when {
            planTimeText.isNotBlank() -> TaskArrangement.TIME
            planPeriod != TaskPlanPeriod.ANYTIME -> TaskArrangement.PERIOD
            else -> TaskArrangement.ANYTIME
        }
    /** 候选分支优先显示，保存仍只使用最后确认的安排。 */
    val selectedArrangement: TaskArrangement
        get() = arrangementEditor ?: arrangement
}

/** 日子簿新建/单日编辑写入边界；日期与跨日判断使用注入 Clock。 */
class TaskFormViewModel(
    private val repository: TaskRepositoryPort,
    private val clock: Clock,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val onClearedCallback: (() -> Unit)? = null,
    private val saveRepeatRule: suspend (TaskRepeatRule) -> Unit = { error("重复保存不可用") },
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private fun initialState(): TaskFormUiState {
        val today = LocalDate.now(clock)
        val thisSaturday = today.plusDays(
            ((java.time.DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7).toLong(),
        )
        val base = TaskFormUiState(
            originDate = today,
            dueDateText = formatUiDate(today),
            zoneId = clock.zone,
            quickTodayDateText = formatUiDate(today),
            quickTomorrowDateText = formatUiDate(today.plusDays(1)),
            quickSaturdayDateText = formatUiDate(thisSaturday),
            quickAfterTomorrowDateText = formatUiDate(today.plusDays(2)),
        )
        // fixture 由 TaskFormViewModelFactory 在 DEBUG 构建下读取 setprop 后注入；
        // 这里只判断「是否注入过」，让 JVM 单测（BuildConfig.DEBUG=false）也能覆盖 fixture 行为。
        return when (debugSeedFixture) {
            "sample" -> base.copy(
                dueDateText = "2026.09.18",
                dueTimeText = "10:00",
                deadlineEnabled = true,
                difficulty = 1,
            )
            // "empty" 与任何其他值都走默认（生产等价）。
            else -> base
        }
    }

    /**
     * 设计稿截屏 fixture。生产构建下保持 ""（生产行为）；仅当 [BuildConfig.DEBUG] 时由
     * [TaskFormViewModelFactory] 在创建时一次性写入。
     *
     * 取值：
     * - `empty`  —— 空表单（今天日期 + 空时间 + 难度 ★☆☆），与生产等价
     * - `sample` —— 预置 design.html 第 ④ 屏的**结构**：`2026.09.18` / `10:00` / 难度 1
     *
     * ⚠ 任务名称「抢演唱会门票」与任务描述段落只是 HTML 稿的展示效果，**不在 fixture 中预填**
     * （用户 2026-09-14 明确要求删除）；`title` / `note` 恒为数据类默认的空串。
     */
    var debugSeedFixture: String = ""

    private val _uiState = MutableStateFlow(savedStateHandle.get<TaskFormUiState>("task-form-session") ?: initialState())
    val uiState = _uiState.asStateFlow()
    private var sessionRevision = 0L
    private var creationId: String? = null
    var hasSession: Boolean = savedStateHandle.contains("task-form-session") && !_uiState.value.isLoading
        private set

    init {
        viewModelScope.launch { uiState.collect { savedStateHandle["task-form-session"] = it.copy(isSaving = false) } }
    }

    fun refreshDateShortcuts() {
        val today = LocalDate.now(clock)
        _uiState.update { it.copy(quickTodayDateText = formatUiDate(today),
            quickTomorrowDateText = formatUiDate(today.plusDays(1)),
            quickAfterTomorrowDateText = formatUiDate(today.plusDays(2))) }
    }
    fun chooseRelativeDate(days: Long) { refreshDateShortcuts(); updateDate(formatUiDate(LocalDate.now(clock).plusDays(days))) }
    fun openDayOffset() {
        if (!canEditDate()) return
        val today = LocalDate.now(clock)
        val current = parseUiDate(_uiState.value.dueDateText) ?: today
        _uiState.update { if (it.editingTaskId == null) it.copy(dayOffsetDraft = DayOffsetSelection.initial(today, current))
            else it.copy(dateInputDraft = TaskDateInputSelection.initial(today, current)) }
    }
    fun selectDayOffset(days: Long) = _uiState.update { state ->
        if (canEditDate()) state.copy(dayOffsetDraft = state.dayOffsetDraft?.withDays(days)) else state
    }
    private fun canEditDate() = _uiState.value.let { it.repeatRuleId == null && !it.isLoading && !it.isSaving && !it.isSaved }
    fun updateDayOffsetInput(value: String) {
        if (canEditDate()) _uiState.update { it.copy(dayOffsetDraft = it.dayOffsetDraft?.withInput(value)) }
    }
    fun updateDateInput(field: String, value: String) {
        if (!canEditDate()) return
        _uiState.update { state -> state.copy(dateInputDraft = state.dateInputDraft?.let {
            when (field) { "year" -> it.copy(year = value); "month" -> it.copy(month = value); "day" -> it.copy(day = value); else -> it }
        }) }
    }
    fun selectDatePreset(days: Long) {
        if (canEditDate()) _uiState.update { it.copy(dateInputDraft = it.dateInputDraft?.let { draft -> draft.preset(days) ?: draft }) }
    }
    fun dismissDateInput() { _uiState.update { it.copy(dateInputDraft = null) }; refreshDateShortcuts() }
    fun confirmDateInput() {
        if (!canEditDate()) return
        val date = _uiState.value.dateInputDraft?.date ?: return
        updateDate(formatUiDate(date)); dismissDateInput()
    }
    fun dismissDayOffset() { _uiState.update { it.copy(dayOffsetDraft = null) }; refreshDateShortcuts() }
    fun confirmDayOffset() {
        if (!canEditDate()) return
        val date = _uiState.value.dayOffsetDraft?.confirmedDate ?: return
        updateDate(formatUiDate(date))
        dismissDayOffset()
    }

    /** 每次进入表单都清空新建状态或按 ID 预填编辑事实字段。 */
    fun start(taskId: String?, selectedDate: LocalDate = LocalDate.now(clock)) {
        val revision = ++sessionRevision
        creationId = null
        hasSession = true
        if (taskId == null) {
            _uiState.value = initialState().copy(originDate = selectedDate)
            return
        }
        _uiState.value = initialState().copy(editingTaskId = taskId, isLoading = true, originDate = selectedDate)
        viewModelScope.launch {
            try {
                val task = repository.findById(taskId)
                if (revision != sessionRevision) return@launch
                if (task == null || task.deletedAt != null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "这个任务不存在，或已被封印。") }
                    return@launch
                }
                _uiState.value = initialState().copy(
                    editingTaskId = task.id,
                    originDate = selectedDate,
                    title = task.title,
                    dateConfirmed = true,
                    extrasAvailable = true,
                    extrasOpen = task.note.isNotBlank() || task.difficulty != null,
                    dueDateText = formatUiDate(task.dueLocalDate),
                    dueTimeText = task.dueInstant?.atZone(task.zoneId ?: clock.zone)?.toLocalTime()?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")).orEmpty(),
                    deadlineDateText = task.dueInstant?.atZone(task.zoneId ?: clock.zone)?.toLocalDate()?.let(::formatUiDate) ?: formatUiDate(task.dueLocalDate),
                    deadlineEnabled = task.dueInstant != null,
                    planPeriod = task.plan.period,
                    planTimeText = task.plan.time?.toString().orEmpty(),
                    important = task.important,
                    autoDefer = task.autoDefer,
                    zoneId = task.zoneId ?: clock.zone,
                    repeatRuleId = task.repeatRuleId,
                    note = task.note,
                    taskType = task.taskType,
                    difficulty = task.difficulty,
                    groupId = task.groupId.orEmpty(),
                    showOnHome = task.showOnHome,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (revision != sessionRevision) return@launch
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "任务加载失败，请重试") }
            }
        }
    }

    /** Defaults belong only to the caller's wheel draft, never to stored form fields. */
    fun currentMinute(): LocalTime = LocalTime.now(clock.withZone(_uiState.value.zoneId ?: clock.zone)).withSecond(0).withNano(0)
    fun initialTimeSelection(deadline: Boolean): LocalTime {
        val state = _uiState.value
        val value = if (deadline) state.dueTimeText else state.planTimeText
        runCatching { LocalTime.parse(value) }.getOrNull()?.let { return it.withSecond(0).withNano(0) }
        if (!deadline) when (state.planPeriod) {
            TaskPlanPeriod.MORNING -> return LocalTime.of(9, 0)
            TaskPlanPeriod.MIDDAY -> return LocalTime.of(14, 0)
            TaskPlanPeriod.EVENING -> return LocalTime.of(19, 0)
            TaskPlanPeriod.ANYTIME -> Unit
        }
        val date = if (deadline && !state.repeating) state.deadlineDateText else state.dueDateText
        val today = LocalDate.now(clock.withZone(state.zoneId ?: clock.zone))
        return if (parseUiDate(date) == today) currentMinute() else LocalTime.of(9, 0)
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun updateDate(value: String) = _uiState.update { if (it.repeatRuleId != null) it else it.copy(dueDateText = value, dateConfirmed = true, errorMessage = null) }
    fun confirmDate() = _uiState.update { it.copy(dateConfirmed = true) }
    fun chooseArrangement(value: TaskArrangement) {
        if (value == TaskArrangement.ANYTIME) { selectPeriod(TaskPlanPeriod.ANYTIME); return }
        _uiState.update {
            it.copy(arrangementEditor = if (it.arrangementEditor == value) null else value,
                errorMessage = null)
        }
    }
    fun dismissArrangementEditor() = _uiState.update { it.copy(arrangementEditor = null) }
    fun toggleExtras() = _uiState.update { it.copy(extrasOpen = !it.extrasOpen) }
    fun toggleAdvanced() = _uiState.update { it.copy(advancedOpen = !it.advancedOpen) }
    fun toggleAdvancedEditor(editor: TaskAdvancedEditor) {
        if (_uiState.value.isLoading || _uiState.value.isSaving || _uiState.value.isSaved) return
        if (editor == TaskAdvancedEditor.REPEAT && _uiState.value.editingTaskId != null) return
        if (_uiState.value.activeEditor == editor) {
            if (editor == TaskAdvancedEditor.DEADLINE) setDeadline(false) else {
                setRepeating(false)
                _uiState.update { it.copy(activeEditor = null) }
            }
        } else {
            if (editor == TaskAdvancedEditor.DEADLINE) beginDeadlineDraft()
            _uiState.update { it.copy(activeEditor = editor, errorMessage = null) }
        }
    }
    fun updateTime(value: String) = _uiState.update {
        val complete = validMinute(value) && (it.repeating || parseUiDate(it.deadlineDateText) != null)
        it.copy(dueTimeText = value, deadlineEnabled = it.deadlineEnabled || complete,
            deadlineRepeating = if (complete) it.repeating else it.deadlineRepeating, errorMessage = null)
    }
    fun beginDeadlineDraft() = _uiState.update {
        if (it.deadlineEnabled || it.deadlineDraftStarted) it else it.copy(deadlineDateText = it.dueDateText, deadlineDraftStarted = true)
    }
    fun updateDeadlineDate(value: String) = _uiState.update { it.copy(deadlineDateText = value, deadlineDraftStarted = true, errorMessage = null) }
    fun setDeadline(enabled: Boolean) = _uiState.update { it.copy(deadlineEnabled = enabled,
        dueTimeText = if (enabled) it.dueTimeText else "", deadlineRepeating = it.repeating,
        deadlineDateText = if (!enabled) it.dueDateText else if (!it.deadlineDraftStarted && !it.deadlineEnabled) it.dueDateText else it.deadlineDateText,
        deadlineDraftStarted = enabled,
        activeEditor = if (!enabled && it.activeEditor == TaskAdvancedEditor.DEADLINE) null else it.activeEditor,
        errorMessage = null) }
    fun confirmDeadlineScope() { updateTime(_uiState.value.dueTimeText) }
    fun selectPeriod(value: TaskPlanPeriod) = _uiState.update { it.copy(planPeriod = value, planTimeText = "", arrangementEditor = if (value == TaskPlanPeriod.ANYTIME) null else TaskArrangement.PERIOD, extrasAvailable = true, errorMessage = null) }
    fun updatePlanTime(value: String) = _uiState.update { it.copy(planPeriod = TaskPlanPeriod.ANYTIME, planTimeText = value, arrangementEditor = null, extrasAvailable = true, errorMessage = null) }
    fun setImportant(value: Boolean) = _uiState.update { it.copy(important = value) }
    fun setAutoDefer(value: Boolean) = _uiState.update { it.copy(autoDefer = value && !it.repeating && it.repeatRuleId == null) }
    fun setRepeating(value: Boolean) = _uiState.update {
        if (it.editingTaskId != null) it else it.copy(repeating = value, autoDefer = if (value) false else it.autoDefer,
            dueDateText = if (value) formatUiDate(maxOf(parseUiDate(it.dueDateText) ?: LocalDate.now(clock), LocalDate.now(clock))) else it.dueDateText,
            weekdays = if (value) it.weekdays else DayOfWeek.entries.toSet(),
            customWeekdays = value && it.customWeekdays,
            repeatEndText = if (value) it.repeatEndText else "",
            errorMessage = null)
    }
    fun chooseRepeatMode(custom: Boolean) {
        val state = _uiState.value
        if (state.editingTaskId != null || state.isLoading || state.isSaving || state.isSaved) return
        if (state.repeating && (state.customWeekdays || state.weekdays.size < 7) == custom) {
            setRepeating(false)
        } else {
            setRepeating(true)
            _uiState.update { it.copy(customWeekdays = custom,
                weekdays = if (custom) it.weekdays else DayOfWeek.entries.toSet()) }
        }
    }
    fun setWeekdays(value: Set<DayOfWeek>) = _uiState.update { it.copy(weekdays = value, errorMessage = null) }
    fun updateRepeatEnd(value: String) = _uiState.update { it.copy(repeatEndText = value, errorMessage = null) }
    fun firstExecutionDate(state: TaskFormUiState = _uiState.value): LocalDate? {
        val start = parseUiDate(state.dueDateText) ?: return null
        val end = state.repeatEndText.takeIf { it.isNotBlank() }?.let(::parseUiDate)
        return (0L..6L).map(start::plusDays).firstOrNull { it.dayOfWeek in state.weekdays && (end == null || it <= end) }
    }
    fun updateNote(value: String) = _uiState.update { it.copy(note = value, errorMessage = null) }
    fun selectDifficulty(value: Int?) {
        require(value == null || value in 1..3) { "difficulty must be null or within 1..3" }
        _uiState.update { it.copy(difficulty = value, errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.isSaved) return
        val date = parseUiDate(state.dueDateText.trim())
        val error = validate(state, date)
        if (error != null || date == null) {
            val editor = when {
                error?.contains("截止") == true || error?.contains("最晚") == true -> TaskAdvancedEditor.DEADLINE
                state.repeating && date != null && state.title.isNotBlank() -> TaskAdvancedEditor.REPEAT
                else -> null
            }
            _uiState.update { it.copy(errorMessage = error ?: "日期格式为 YYYY.MM.DD。",
                activeEditor = editor ?: it.activeEditor, advancedOpen = editor != null || it.advancedOpen) }
            return
        }
        val zone = state.zoneId ?: clock.zone
        val deadlineDate = if (state.repeating) date else parseUiDate(state.deadlineDateText.trim())
        val dueInstant = if (state.deadlineEnabled) LocalDateTime.of(requireNotNull(deadlineDate), LocalTime.parse(state.dueTimeText.trim())).atZone(zone).toInstant() else null
        val plan = TaskPlan(state.planPeriod, state.planTimeText.trim().takeIf { it.isNotEmpty() }?.let(LocalTime::parse))
        val editingId = state.editingTaskId
        val revision = sessionRevision
        val newId = if (editingId == null) creationId ?: idGenerator().also { creationId = it } else null
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val existing = if (editingId == null) null else repository.findById(editingId)
                if (editingId != null && (existing == null || existing.deletedAt != null)) {
                    if (revision == sessionRevision) _uiState.update { it.copy(isSaving = false, errorMessage = "这个任务不存在，或已被封印。") }
                    return@launch
                }
                val now = Instant.ofEpochMilli(clock.millis())
                val oldDeadline = existing?.dueInstant?.atZone(zone)
                val unchangedDeadline = state.deadlineEnabled && oldDeadline != null && oldDeadline.toLocalDate() == deadlineDate &&
                    oldDeadline.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) == state.dueTimeText.trim()

                if (state.repeating) {
                    val rule = TaskRepeatRule(id = requireNotNull(newId), effectiveFrom = date, startDate = date,
                        endDate = state.repeatEndText.trim().takeIf { it.isNotEmpty() }?.let { requireNotNull(parseUiDate(it)) },
                        weekdays = state.weekdays, zoneId = zone, title = state.title.trim(), note = state.note.trim(),
                        plan = plan, deadlineTime = if (state.deadlineEnabled) LocalTime.parse(state.dueTimeText.trim()) else null,
                        important = state.important, difficulty = state.difficulty, taskType = state.taskType,
                        groupId = state.groupId.trim().ifEmpty { null }, showOnHome = state.showOnHome, createdAt = now, updatedAt = now)
                    saveRepeatRule(rule)
                } else repository.save(
                    Task(
                        id = existing?.id ?: requireNotNull(newId),
                        title = state.title.trim(),
                        dueLocalDate = date,
                        dueInstant = if (unchangedDeadline) existing?.dueInstant else dueInstant,
                        zoneId = zone,
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
                        plan = plan,
                        important = state.important,
                        repeatRuleId = existing?.repeatRuleId,
                        repeatOccurrenceDate = existing?.repeatOccurrenceDate,
                        isSkipped = existing?.isSkipped ?: false,
                        initialScheduledDate = existing?.initialScheduledDate ?: date,
                        autoDefer = state.autoDefer,
                        restoredOn = existing?.restoredOn,
                    ),
                )
                if (revision == sessionRevision) {
                    val targetDate = if (state.repeating) requireNotNull(firstExecutionDate(state)) else date
                    val savedId = if (state.repeating) TaskRepeatSchedule.instanceId(requireNotNull(newId), targetDate) else editingId ?: newId
                    _uiState.update { it.copy(isSaving = false, isSaved = true, savedDueDate = targetDate, savedTaskId = savedId) }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (revision != sessionRevision) return@launch
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message ?: "接受任务失败，请重试") }
            }
        }
    }

    /** 只消费一次成功事件；返回入口浏览日，业务所属日仍在 savedDueDate 中独立发布。 */
    fun consumeSaved(): LocalDate? {
        // 保留退出中的表单内容，避免导航前闪成空表单；只消费一次日期事件。
        if (_uiState.value.savedDueDate == null) return null
        val origin = _uiState.value.originDate
        _uiState.update { it.copy(savedDueDate = null) }
        return origin
    }

    private fun validate(state: TaskFormUiState, date: LocalDate?): String? {
        if (state.title.isBlank()) return "请填写任务名称。"
        if (date == null) return "日期格式为 YYYY.MM.DD。"
        if (state.planTimeText.isNotBlank() && !validMinute(state.planTimeText)) return "计划时间格式为 HH:MM。"
        if (state.deadlineEnabled || state.dueTimeText.isNotBlank()) {
            if (state.deadlineRepeating != state.repeating) return "请在截止中确认${if (state.repeating) "改为每个执行日当天截止" else "改为单次截止"}，或移除截止。"
            if (!validMinute(state.dueTimeText)) return "截止时间格式为 HH:MM。"
            if (!state.repeating && parseUiDate(state.deadlineDateText.trim()) == null) return "请检查最晚完成日期。"
        }
        if (state.repeating) {
            if (date < LocalDate.now(clock)) return "重复开始日不能早于今天。"
            if (state.weekdays.isEmpty()) return "至少选择一个执行日。"
            if (state.repeatEndText.isNotBlank()) {
                val end = parseUiDate(state.repeatEndText.trim()) ?: return "请检查重复结束日。"
                if (end < date) return "结束日不能早于开始日。"
            }
            if (firstExecutionDate(state) == null) return "起止区间没有有效执行日。"
        }
        return null
    }

    private fun validMinute(value: String): Boolean = Regex("[0-2][0-9]:[0-5][0-9]").matches(value.trim()) && runCatching { LocalTime.parse(value.trim()) }.isSuccess

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }

    /**
     * design.html 第 ④ 屏示例可见的日期格式 `YYYY.MM.DD`（如 `2026.09.18`）。
     * 与 `LocalDate.parse` 的 ISO 格式 `YYYY-MM-DD` 略有差异，需要独立解析路径。
     */
    private fun formatUiDate(date: LocalDate): String =
        if (date.year in 0..9999) "%04d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth) else date.toString()

    private fun parseUiDate(text: String): LocalDate? {
        // 先按 design 的点分格式解析；点分隔失败时回落到 ISO，方便用户混输。
        val dotted = Regex("""(\d{4})\.(\d{1,2})\.(\d{1,2})""")
        dotted.matchEntire(text)?.let { m ->
            val (y, mo, d) = m.destructured
            return runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
        }
        return runCatching { LocalDate.parse(text) }.getOrNull()
    }
}

class TaskFormViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = build(modelClass, SavedStateHandle())

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        build(modelClass, extras.createSavedStateHandle())

    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> build(modelClass: Class<T>, handle: SavedStateHandle): T {
        require(modelClass.isAssignableFrom(TaskFormViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer.get(applicationContext)
        val viewModel = TaskFormViewModel(
            repository = container.taskRepository,
            clock = container.clock,
            saveRepeatRule = container.taskRepeatRepository::create,
            savedStateHandle = handle,
        )
        // 设计稿截屏 fixture：`adb shell setprop debug.momentmark.taskform empty|sample`。
        // 仅 debug 构建生效；release 构建始终保持 ""（生产行为）。
        if (BuildConfig.DEBUG) {
            viewModel.debugSeedFixture = readDebugSeed(applicationContext)
        }
        return viewModel as T
    }

    private fun readDebugSeed(context: Context): String = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, "debug.momentmark.taskform", "") as? String).orEmpty()
    } catch (_: Throwable) {
        ""
    }
}
