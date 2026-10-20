package com.cch.momentmark.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import com.cch.momentmark.domain.model.TaskPlanPeriod
import com.cch.momentmark.domain.model.resolveTaskPlanPeriod
import com.cch.momentmark.ui.components.*
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.MmSemanticColor
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** 页面提供状态/回调，轮盘仅维护候选；独立截止不随计划日期变化。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskPlanningPanel(state: TaskFormUiState, vm: TaskFormViewModel, extras: @Composable () -> Unit = {}) {
    var dateTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var dateDraft by rememberSaveable { mutableStateOf(state.dueDateText) }
    var intendedDay by rememberSaveable { mutableIntStateOf(1) }
    var timeTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var timeDraft by rememberSaveable { mutableStateOf("09:00") }
    var timeNow by rememberSaveable { mutableStateOf("09:00") }
    fun openTime(target: String) {
        timeNow = vm.currentMinute().toString()
        timeDraft = vm.initialTimeSelection(target == "deadline").toString()
        timeTarget = target
    }
    val taskColors = LocalMmExtendedColors.current.task
    val scheme = MaterialTheme.colorScheme
    val settingColors = MmSemanticColor(scheme.secondary, scheme.secondaryContainer, scheme.onSecondaryContainer)
    val effectivePeriod = resolveTaskPlanPeriod(state.planPeriod, runCatching { LocalTime.parse(state.planTimeText) }.getOrNull())
    val enabled = !state.isLoading && !state.isSaving && !state.isSaved
    val columns = if (LocalDensity.current.fontScale >= 1.5f) 2 else 4
    val customWeekdays = state.customWeekdays || state.weekdays.size < 7
    val today = LocalDate.parse(state.quickTodayDateText.replace('.', '-'))
    fun parse(value: String) = runCatching { LocalDate.parse(value.replace('.', '-')) }.getOrDefault(today)
    fun openDate(target: String, value: String) {
        dateDraft = parse(value).toString(); intendedDay = parse(value).dayOfMonth; dateTarget = target
    }
    @Composable fun note(value: String) { Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall) }
    @Composable fun action(label: String, icon: PixelButtonIcon? = null, click: () -> Unit) {
        TaskFormAction(label, click, icon = icon, enabled = enabled)
    }
    @Composable fun disclosure(label: String, open: Boolean, icon: PixelButtonIcon, click: () -> Unit) {
        TaskFormAction(label, click, Modifier.fillMaxWidth(), icon, enabled, expanded = open, field = true)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard)) {
        // Repeat occurrences keep their locked date in the page header; do not repeat it here.
        if (state.repeatRuleId == null) TaskFormSection("task-section-date") {
            val dateLabel = if (state.repeating) "重复开始日" else "哪天做"
            TaskFormSectionLabel(dateLabel)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                listOf("今天" to state.quickTodayDateText, "明天" to state.quickTomorrowDateText,
                    "后天" to state.quickAfterTomorrowDateText).forEachIndexed { index, (label, value) ->
                    TaskFormAction(label, { vm.chooseRelativeDate(index.toLong()) }, enabled = enabled,
                        selected = parse(state.dueDateText) == parse(value))
                }
                TaskFormAction("自定义", { vm.openDayOffset() },
                    enabled = enabled, selected = listOf(state.quickTodayDateText, state.quickTomorrowDateText, state.quickAfterTomorrowDateText)
                        .none { parse(it) == parse(state.dueDateText) })
            }
        }
        TaskFormSection("task-section-arrangement") {
            TaskFormReveal(true, "form-arrangement") {
                TaskFormSectionLabel("什么时候做")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                    listOf(Triple("随时", TaskArrangement.ANYTIME, PixelButtonIcon.Calendar),
                        Triple("时段", TaskArrangement.PERIOD, PixelButtonIcon.Sunrise),
                        Triple("时间", TaskArrangement.TIME, PixelButtonIcon.Clock)).forEach { (label, mode, icon) ->
                        TaskFormAction(label, {
                            vm.chooseArrangement(mode)
                            if (mode == TaskArrangement.TIME) {
                                if (vm.uiState.value.arrangementEditor == mode) openTime("plan") else timeTarget = null
                            }
                        },
                            Modifier.weight(1f), icon, enabled, selected = state.selectedArrangement == mode,
                            expanded = if (mode == TaskArrangement.PERIOD) state.arrangementEditor == mode else null,
                            selectedColors = taskColors.period(if (mode == TaskArrangement.ANYTIME) TaskPlanPeriod.ANYTIME else effectivePeriod))
                    }
                }
                TaskFormReveal(state.arrangementEditor == TaskArrangement.PERIOD, "form-period") {
                    TaskFormConnectedBody(1, 3) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                            TaskPlanPeriod.entries.filter { it != TaskPlanPeriod.ANYTIME }.forEach { period ->
                                TaskFormAction(listOf("随时", "早", "中", "晚")[period.ordinal], { vm.selectPeriod(period) },
                                    Modifier.weight(1f), when (period) { TaskPlanPeriod.MORNING -> PixelButtonIcon.Sunrise; TaskPlanPeriod.MIDDAY -> PixelButtonIcon.Sun; else -> PixelButtonIcon.Moon },
                                    enabled, selected = state.arrangement == TaskArrangement.PERIOD && state.planPeriod == period,
                                    selectedColors = taskColors.period(period))
                            }
                        }
                    }
                }
                if (state.selectedArrangement == TaskArrangement.PERIOD && state.arrangementEditor != TaskArrangement.PERIOD) {
                    note("当前安排 · ${listOf("随时", "早", "中", "晚")[state.planPeriod.ordinal]}")
                }
                TaskFormReveal(state.selectedArrangement == TaskArrangement.TIME, "form-time") {
                    TaskFormConnectedBody(2, 3) {
                        TaskFormAction(if (state.planTimeText.isBlank()) "选择时间" else "计划 ${state.planTimeText}", {
                            if (state.arrangementEditor != TaskArrangement.TIME) vm.chooseArrangement(TaskArrangement.TIME)
                            openTime("plan")
                        },
                            Modifier.fillMaxWidth(), PixelButtonIcon.Clock, enabled, selected = true, field = true,
                            selectedColors = taskColors.period(effectivePeriod))
                    }
                }
            }
        }
        extras()
        TaskFormSection("task-section-advanced") {
            disclosure(state.advancedSummary(), state.advancedOpen, PixelButtonIcon.Settings) { vm.toggleAdvanced() }
            TaskFormReveal(state.advancedOpen, "form-advanced") {
                val active = state.activeEditor
                Row(Modifier.fillMaxWidth().testTag("advanced-actions"), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                    TaskFormAction("重要", { vm.setImportant(!state.important) }, Modifier.weight(1f), PixelButtonIcon.Star,
                        enabled = enabled, checked = state.important, shortLabel = true,
                        selectedColors = taskColors.important)
                    TaskFormAction("截止", { vm.toggleAdvancedEditor(TaskAdvancedEditor.DEADLINE) }, Modifier.weight(1f), PixelButtonIcon.Clock,
                        enabled = enabled, selected = state.deadlineEnabled, editing = active == TaskAdvancedEditor.DEADLINE,
                        selectedColors = settingColors, shortLabel = true)
                    if (state.editingTaskId == null) TaskFormAction("重复", { vm.toggleAdvancedEditor(TaskAdvancedEditor.REPEAT) },
                        Modifier.weight(1f), PixelButtonIcon.Repeat, enabled = enabled, selected = state.repeating,
                        editing = active == TaskAdvancedEditor.REPEAT, selectedColors = settingColors, shortLabel = true)
                }
                // One shared body: switching changes its contents immediately, so outgoing fields
                // cannot overlap or expose a second live editor during a reveal transition.
                TaskFormReveal(active != null, "form-advanced-editor") {
                    if (active != null) TaskFormConnectedBody(
                        activeSlot = if (active == TaskAdvancedEditor.DEADLINE) 1 else 2,
                        slots = if (state.editingTaskId == null) 3 else 2,
                        modifier = Modifier.testTag("advanced-editor-${active.name.lowercase()}")) {
                        when (active) {
                            TaskAdvancedEditor.DEADLINE -> {
                                note("截止设置")
                                if (state.repeating) note("最晚完成 · 每个执行日当天") else {
                                    TaskFormAction("截止日期 ${state.deadlineDateText}", { openDate("deadline", state.deadlineDateText) },
                                        Modifier.fillMaxWidth(), PixelButtonIcon.Calendar, enabled, field = true)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                                        listOf("截止今天" to state.quickTodayDateText, "截止明天" to state.quickTomorrowDateText).forEach { (label, value) ->
                                            TaskFormAction(label, { vm.updateDeadlineDate(value) }, icon = PixelButtonIcon.Calendar, enabled = enabled)
                                        }
                                    }
                                }
                                TaskFormAction("截止时间 ${state.dueTimeText.ifBlank { "待选时间" }}", { openTime("deadline") },
                                    Modifier.fillMaxWidth(), PixelButtonIcon.Clock, enabled, field = true)
                                if (state.deadlineEnabled && state.deadlineRepeating != state.repeating) {
                                    note(if (state.repeating) "原截止属于单次任务，请确认每日截止。" else "原截止属于重复任务，请确认单次截止日期。")
                                    action(if (state.repeating) "改为每日截止 ${state.dueTimeText}" else "改为单次截止 ${state.deadlineDateText}", PixelButtonIcon.Clock) { vm.confirmDeadlineScope() }
                                }
                            }
                            TaskAdvancedEditor.REPEAT -> {
                                note("重复任务")
                                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
                                    verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                                    TaskFormAction("不重复", { vm.setRepeating(false) }, enabled = enabled, selected = !state.repeating)
                                    TaskFormAction("每天", { vm.chooseRepeatMode(custom = false) }, enabled = enabled, selected = state.repeating && !customWeekdays && state.weekdays.size == 7)
                                    TaskFormAction("每周自选", { vm.chooseRepeatMode(custom = true) }, enabled = enabled, selected = state.repeating && (customWeekdays || state.weekdays.size < 7))
                                }
                                if (state.repeating) {
                                    if (customWeekdays || state.weekdays.size < 7) DayOfWeek.entries.chunked(columns).forEach { chunk ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                                            chunk.forEach { day ->
                                                val chosen = day in state.weekdays
                                                TaskFormAction("周${listOf("一", "二", "三", "四", "五", "六", "日")[day.ordinal]}",
                                                    { vm.setWeekdays(if (chosen) state.weekdays - day else state.weekdays + day) },
                                                    Modifier.weight(1f), enabled = enabled, selected = chosen)
                                            }
                                            repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                    TaskFormAction("结束日 ${state.repeatEndText.ifBlank { "未设置" }}", { openDate("end", state.repeatEndText.ifBlank { state.dueDateText }) },
                                        Modifier.fillMaxWidth(), PixelButtonIcon.Calendar, enabled, field = true)
                                    if (state.repeatEndText.isNotBlank()) action("清除结束日", PixelButtonIcon.Close) { vm.updateRepeatEnd("") }
                                    note("结束日包含当天")
                                }
                            }
                        }
                        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (!state.repeating && state.repeatRuleId == null) {
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    note("未完成处理")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                        TaskFormAction("手动", { vm.setAutoDefer(false) }, Modifier.weight(1f), enabled = enabled, selected = !state.autoDefer)
                        TaskFormAction("自动顺延", { vm.setAutoDefer(!state.autoDefer) }, Modifier.weight(1f), PixelButtonIcon.Repeat, enabled, selected = state.autoDefer)
                    }
                    if (state.autoDefer) note("不会延长截止；自动顺延清除精确计划，回到随时。")
                }
            }
            val planTime = runCatching { LocalTime.parse(state.planTimeText) }.getOrNull()
            val deadline = runCatching { LocalTime.parse(state.dueTimeText) }.getOrNull()
            if (state.deadlineEnabled && planTime != null && deadline != null &&
                (state.repeating || parse(state.dueDateText).atTime(planTime) > parse(state.deadlineDateText).atTime(deadline)) &&
                (!state.repeating || planTime > deadline)) note("计划时间晚于截止")
        }
    }
    val timeSelection = LocalTime.parse(timeDraft)
    val deadlineTarget = timeTarget == "deadline"
    val timeLabel = if (deadlineTarget) "截止时间" else "计划时间"
    PixelTimeWheel(timeTarget != null, timeSelection,
        TimeWheelCopy("选择$timeLabel", "$timeLabel · $timeDraft", "", "时", "分", "当前时刻", "下一个整点", "取消", "确认$timeLabel", if (deadlineTarget) "移除截止" else "随时（全天）"),
        LocalTime.parse(timeNow),
        { timeDraft = it.toString() },
        { if (deadlineTarget) vm.updateTime(timeDraft) else vm.updatePlanTime(timeDraft); timeTarget = null },
        { if (deadlineTarget) { vm.setDeadline(false) } else vm.selectPeriod(TaskPlanPeriod.ANYTIME); timeTarget = null },
        { if (!deadlineTarget) vm.dismissArrangementEditor(); timeTarget = null })
    TaskDateInputDialog(state.dayOffsetDraft, state.dateInputDraft, vm::updateDayOffsetInput, vm::selectDayOffset,
        vm::updateDateInput, vm::selectDatePreset,
        { if (state.dayOffsetDraft != null) vm.confirmDayOffset() else vm.confirmDateInput() },
        { if (state.dayOffsetDraft != null) vm.dismissDayOffset() else vm.dismissDateInput() })
    val selection = DateWheelSelection(parse(dateDraft), intendedDay)
    PixelDateWheel(dateTarget != null, selection, dateWheelYearRange(today, selection.date),
        DateWheelCopy("选择${when (dateTarget) { "deadline" -> "截止日期"; "end" -> "重复结束日"; else -> if (state.repeating) "重复开始日" else "计划日期" }}",
            selection.date.toString(), "选择日期", null, "年", "月", "日", "今天", "取消", "确认日期"),
        { dateDraft = selection.withYear(it).date.toString() }, { dateDraft = selection.withMonth(it).date.toString() },
        { dateDraft = selection.withDay(it).date.toString(); intendedDay = it },
        { dateDraft = today.toString(); intendedDay = today.dayOfMonth },
        { when (dateTarget) { "deadline" -> vm.updateDeadlineDate(selection.date.toString()); "end" -> vm.updateRepeatEnd(selection.date.toString()); else -> vm.updateDate(selection.date.toString()) }; dateTarget = null },
        { dateTarget = null })
}
