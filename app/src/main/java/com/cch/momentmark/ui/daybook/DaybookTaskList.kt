package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelButtonGlyph
import com.cch.momentmark.ui.components.PixelButtonIcon
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.cch.momentmark.domain.model.Task
import androidx.compose.ui.text.rememberTextMeasurer
import com.cch.momentmark.ui.components.PixelTaskRow
import com.cch.momentmark.ui.components.TaskRowPresentation
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** 一个连续列表；共享任务行只接收展示数据与回调。安排字段由调用方提供。 */
@Composable
internal fun DaybookTaskList(
    date: LocalDate,
    tasks: List<Task>,
    enabled: Boolean,
    onToggle: (Task) -> Unit,
    onEdit: (String) -> Unit,
    onSeal: (Task) -> Unit,
    plans: Map<String, TaskExecutionPlan> = emptyMap(),
    clock: Clock = Clock.systemDefaultZone(),
    onLocate: (LocalDate) -> Unit = {},
    acceptedTaskId: String? = null,
    completedOnly: Boolean = false,
    transitions: DaybookCompletionTransitions? = null,
    requestCompletion: ((Task, (Boolean) -> Unit) -> Unit)? = null,
    completionBusy: Set<String> = emptySet(),
) {
    var now by remember(clock) { mutableStateOf(clock.instant()) }
    LaunchedEffect(clock) { while (true) {
        now = clock.instant()
        val midnight = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.zone).toInstant()
        delay(minOf(30_000L, java.time.Duration.between(now, midnight).toMillis().coerceAtLeast(1L)))
    } }
    val history = tasks.filter { it.deletedAt == null && (it.movedTo != null || it.isSkipped || it.repeatDisplay == com.cch.momentmark.domain.model.RepeatDisplay.UNRECORDED) }
    val holds = transitions?.entries?.filterKeys { it.date == date }.orEmpty()
    val projection = projectDaybookTasks(tasks, Clock.fixed(now, clock.zone), plans, date,
        holds.values.map { CompletionHold(it.task, it.period, it.order) })
    val density = LocalDensity.current
    val largeFont = density.fontScale > 1.3f
    val futureTrack = projection.future && !completedOnly
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val nodeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentTop = MomentMarkTokens.DaybookFutureRowTop + MomentMarkTokens.DaybookFutureBodyVertical
    val periodStyle = MaterialTheme.typography.labelSmall.copy(fontSize = MomentMarkTokens.DaybookPeriodFontSize)
    val textMeasurer = rememberTextMeasurer()
    // Measure the same two-character label/style we draw; preserve a gap before the checkbox.
    val periodWidthPx = textMeasurer.measure(TaskPeriod.ANYTIME.label, periodStyle, softWrap = false).size.width
    val gutterWidth = maxOf(MomentMarkTokens.DaybookPeriodGutterWidth,
        with(density) { periodWidthPx.toDp() } + MomentMarkTokens.SpaceUnit)
    @Composable fun row(task: Task, period: TaskPeriod?, first: Boolean, routineStart: Boolean = false) {
        val transition = holds[CompletionKey(date, task.id)]
        val confirmed = transition?.writeSucceeded == true && tasks.any { it.id == task.id && it.isCompleted }
        val busy = transition != null || task.id in completionBusy
        val plan = plans[task.id] ?: task.executionPlan()
        val rowState = taskRowState(task, date, clock)
        val historical = task in history
        val routine = task.id in projection.routineIds
        val foreground = projection.future && !completedOnly && !historical && !task.isCompleted && !routine
        val deadlineState = taskDeadlineState(task, now)
        val deadline = task.dueInstant?.atZone(task.zoneId ?: ZoneId.systemDefault())
        val deadlineText = deadline?.let {
            (if (it.toLocalDate() != date) it.format(DateTimeFormatter.ofPattern("MM.dd ")) else "") + it.format(DateTimeFormatter.ofPattern("HH:mm")) + "前"
        }
        val status = if (task.movedTo != null) task.movedTo.format(DateTimeFormatter.ofPattern(if (task.movedTo.year != date.year) "yyyy.MM.dd" else "MM.dd"))
        else if (task.isSkipped) "已跳过"
        else if (task.repeatDisplay == com.cch.momentmark.domain.model.RepeatDisplay.UNRECORDED) "未记录"
        else if (task.isCompleted) listOfNotNull("已完成", plan.resolvedPeriod.description,
            plan.time?.format(DateTimeFormatter.ofPattern("HH:mm"))).joinToString(" · ") else listOfNotNull(
            when (deadlineState) { TaskDeadlineState.OVERDUE -> "已过截止"; TaskDeadlineState.SOON -> "即将截止"; else -> null },
            plan.time?.format(DateTimeFormatter.ofPattern("HH:mm")), deadlineText, if (plan.important) "重要" else null,
            if (task.repeatDisplay == com.cch.momentmark.domain.model.RepeatDisplay.PREVIEW) "计划" else null,
        ).joinToString(" · ")
        val description = listOf(rowState.description, when {
            task.isCompleted -> listOfNotNull("${plan.resolvedPeriod.description}任务",
                plan.time?.let { "计划 $it" }).joinToString("，")
            else -> listOfNotNull("${plan.resolvedPeriod.description}任务", plan.time?.let { "计划 $it" },
                deadline?.let { "截止时间 $it" }, when (deadlineState) {
                    TaskDeadlineState.OVERDUE -> "已过截止"; TaskDeadlineState.SOON -> "即将截止"; else -> null
                }, if (plan.important) "重要" else null).joinToString("，")
        }).filterNotNull().joinToString("，")
        val requester = remember(task.id) { BringIntoViewRequester() }
        LaunchedEffect(task.id == acceptedTaskId) {
            if (task.id == acceptedTaskId) { delay(180); requester.bringIntoView() }
        }
        Row(Modifier.fillMaxWidth().testTag("daybook-task-row-${task.id}")
            .then(if (futureTrack && period != null && !historical) Modifier.drawBehind {
                // The axis owns a separate slot: neither face, border nor external hard shadow enters it.
                val x = gutterWidth.toPx() + MomentMarkTokens.DaybookFutureAxisWidth.toPx() / 2f
                drawLine(trackColor, Offset(x, 0f), Offset(x, size.height), MomentMarkTokens.DaybookFutureAxisStroke.toPx())
                val markTop = ((MomentMarkTokens.DaybookTaskTitleLineHeight.value * density.fontScale -
                    MomentMarkTokens.CheckboxSize.value) / 2f).coerceAtLeast(0f).dp
                val y = (contentTop + MomentMarkTokens.DaybookTaskRowPaddingVertical + markTop +
                    MomentMarkTokens.CheckboxSize / 2).toPx()
                val node = MomentMarkTokens.DaybookFutureNodeSize.toPx()
                drawRect(nodeColor, Offset(x - node / 2, y - node / 2), Size(node, node))
            } else Modifier)
            .then(if (task.id == acceptedTaskId) Modifier
                .border(2.dp, MaterialTheme.colorScheme.primary)
                .semantics { stateDescription = "新接取的任务" } else Modifier)
            .bringIntoViewRequester(requester)) {
            // 每个非空时段只标首行；已完成行继续留空并保持标题对齐。
            Column(Modifier.width(gutterWidth)) {
            Text(if (first) period?.label.orEmpty() else "", modifier = Modifier.width(gutterWidth)
                .then(if (first && period != null) Modifier.testTag("daybook-period-${period.label}") else Modifier)
                .padding(top = MomentMarkTokens.DaybookTaskRowPaddingVertical +
                    if (futureTrack && !historical) contentTop else 0.dp, end = MomentMarkTokens.SpaceUnit)
                .clearAndSetSemantics {}, softWrap = false,
                style = periodStyle,
                color = if (period != null) LocalMmExtendedColors.current.task.period(
                    com.cch.momentmark.domain.model.TaskPlanPeriod.valueOf(period.name)).onContainer else MaterialTheme.colorScheme.primary)
            if (routineStart) PixelButtonGlyph(PixelButtonIcon.Repeat, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            @Composable fun taskBody() { PixelTaskRow(
                TaskRowPresentation(task.title, task.isCompleted || confirmed, status, if (confirmed) "已完成" else if (busy) "正在保存完成状态" else description, !historical && task.dueInstant == null,
                    !historical && (deadlineState == TaskDeadlineState.OVERDUE || deadlineState == TaskDeadlineState.SOON), completedOnly || largeFont || projection.future, titleActionLabel = if (task.movedTo != null) "定位目标日期" else "查看任务操作",
                    mark = if (confirmed) com.cch.momentmark.ui.components.TaskRowMark.CHECK else if (busy) com.cch.momentmark.ui.components.TaskRowMark.BUSY else rowState.mark, rowActionEnabled = rowState.canOpen, sealEnabled = rowState.canSeal, emphasized = !routine, showSeparator = !foreground,
                    important = foreground && plan.important, showRiskIcon = foreground, indicatorsBelowTitle = futureTrack,
                    completionPending = busy && !confirmed, strikeProgress = transition?.strike?.value),
                onToggleCompleted = { if (enabled && !busy) {
                    val entry = transitions?.begin(date, task, projection)
                    if (requestCompletion != null) requestCompletion(task) { success ->
                        if (entry != null) {
                            if (success) entry.writeSucceeded = true
                            else transitions.finish(CompletionKey(date, task.id), entry)
                        }
                    } else {
                        // Stateless/preview consumers confirm through their observed Task result.
                        entry?.writeSucceeded = true
                        onToggle(task)
                    }
                } },
                onEdit = { if (enabled) { if (task.movedTo != null) onLocate(task.movedTo) else onEdit(task.id) } }, onSeal = { if (enabled) onSeal(task) },
                modifier = Modifier.then(when {
                    task.movedTo != null -> Modifier.testTag("moved-history-${task.id}")
                    historical -> Modifier.testTag("repeat-history-${task.id}")
                    else -> Modifier
                }), enabled = enabled && !busy,
                completionEnabled = rowState.canComplete,
            ) }
            if (futureTrack && !historical) {
                Spacer(Modifier.width(MomentMarkTokens.DaybookFutureAxisWidth + MomentMarkTokens.DaybookFutureAxisGap))
                // All future rows share the same body and first-line offsets; only the surface differs.
                Box(Modifier.weight(1f).padding(top = MomentMarkTokens.DaybookFutureRowTop,
                    bottom = MomentMarkTokens.DaybookFutureRowBottom, end = MomentMarkTokens.DaybookFutureShadow)) {
                    @Composable fun body() {
                        Box(Modifier.padding(horizontal = MomentMarkTokens.DaybookFutureBodyHorizontal,
                            vertical = MomentMarkTokens.DaybookFutureBodyVertical)) { taskBody() }
                    }
                    if (foreground) PixelPanel(Modifier.testTag("daybook-foreground-${task.id}"),
                        borderWidth = MomentMarkTokens.DaybookFutureBorder,
                        shadowOffset = MomentMarkTokens.DaybookFutureShadow) { body() }
                    else Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("daybook-routine-${task.id}")) { body() }
                }
            } else Box(Modifier.weight(1f)) { taskBody() }
        }
    }
    if (completedOnly) {
        projection.completed.forEach { task -> key(task.id) { row(task, null, false) } }
        return
    }
    projection.sections.forEach { section ->
        Column(Modifier.fillMaxWidth()) {
            section.tasks.forEachIndexed { index, task -> key(task.id) {
                row(task, section.period, index == 0, task.id in projection.routineIds &&
                    (index == 0 || section.tasks[index - 1].id !in projection.routineIds))
            } }
        }
    }
    if (history.isNotEmpty()) {
        // A quiet boundary between executable tasks and history; no extra shadow/card layer.
        Box(Modifier.fillMaxWidth().padding(vertical = MomentMarkTokens.SpaceUnit)
            .height(1.dp).background(MaterialTheme.colorScheme.outlineVariant).testTag("daybook-history-divider"))
        history.forEachIndexed { index, task -> key(task.id) {
            row(task, (plans[task.id] ?: task.executionPlan()).resolvedPeriod, index == 0 ||
                (plans[history[index - 1].id] ?: history[index - 1].executionPlan()).resolvedPeriod !=
                (plans[task.id] ?: task.executionPlan()).resolvedPeriod)
        } }
    }
}
