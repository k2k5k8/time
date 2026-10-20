package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.cch.momentmark.data.repository.TaskRepeatRepository
import com.cch.momentmark.domain.model.*
import com.cch.momentmark.ui.components.*
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.*

/** One page-level entry. Only the manager body scrolls; actions keep the existing callbacks. */
@Composable
internal fun DaybookRepeatControls(series: List<TaskRepeatRepository.Series>, enabled: Boolean,
    onPause: (String, Boolean) -> Unit, onResume: (String) -> Unit, onEdit: (TaskRepeatRule, () -> Unit) -> Unit, clock: Clock, saveError: String?,
    modifier: Modifier = Modifier, loaded: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskRepeatRule?>(null) }
    var stopping by remember { mutableStateOf<String?>(null) }
    val entryFocus = remember { FocusRequester() }
    val hostView = LocalView.current
    var restoreFocus by remember { mutableStateOf(false) }
    val managerScroll = rememberScrollState()
    fun dismiss() { expanded = false; restoreFocus = true }
    LaunchedEffect(expanded, restoreFocus) {
        if (!expanded && restoreFocus) {
            withFrameNanos { }
            while (!hostView.hasWindowFocus()) withFrameNanos { }
            entryFocus.requestFocus(); restoreFocus = false
        }
    }
    val count = if (loaded) "${series.size}" else "加载中"
    TaskActionButton("重复任务 $count", PixelButtonIcon.Repeat, { expanded = true },
        modifier.focusRequester(entryFocus).testTag("daybook-repeat-series"), enabled = enabled && loaded,
        description = if (loaded) "重复任务，${series.size}个系列，打开管理" else "重复任务，加载中")
    if (expanded) TaskActionDialog(
        "重复任务", "daybook-repeat-manager", ::dismiss,
        closeLabel = "关闭重复任务", closeTag = "daybook-repeat-close",
        subtitle = "${series.size}个系列", error = saveError, scrollState = managerScroll,
    ) {
        if (series.isEmpty()) TaskActionNote("还没有重复任务。")
        series.forEach { item -> key(item.rule.id) {
            RepeatSeriesCard(item, clock, enabled,
                onPause = { if (item.breakKind == null) onPause(item.rule.id, false) else onResume(item.rule.id) },
                onEdit = { editing = item.rule }, onStop = { stopping = item.rule.id })
        } }
    }
    // Child dialogs sit above the manager; Back/Cancel returns to its scroll position.
    stopping?.let { id ->
        val title = series.firstOrNull { it.rule.id == id }?.rule?.title.orEmpty()
        PixelConfirmationDialog("停止今天及以后的重复任务？", "$title\n保留完成、跳过及封印历史。以后可从重复任务恢复，不补造停止期间的任务。", "停止重复任务", { onPause(id, true); stopping = null }, { stopping = null }, "取消")
    }
    editing?.let { rule -> RepeatRuleEditor(rule, maxOf(rule.effectiveFrom, TaskRepeatSchedule.today(rule, clock)), onEdit, { editing = null }, saveError) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatSeriesCard(item: TaskRepeatRepository.Series, clock: Clock, enabled: Boolean,
    onPause: () -> Unit, onEdit: () -> Unit, onStop: () -> Unit) {
    val summary = repeatSeriesSummary(item.rule, item.breakKind, clock)
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().testTag("repeat-series-${item.rule.id}")
        .background(colors.surfaceVariant).border(MomentMarkTokens.PxThinBorderWidth, colors.outline)
        .padding(MomentMarkTokens.SpaceCompact),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact),
            verticalAlignment = Alignment.Top) {
            Text(item.rule.title, Modifier.weight(1f), color = colors.onSurface,
                style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.testTag("repeat-status-${item.rule.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                PixelButtonGlyph(when (summary.status) {
                    "暂停" -> PixelButtonIcon.Pause
                    "停止" -> PixelButtonIcon.Stop
                    "未开始" -> PixelButtonIcon.Clock
                    "结束" -> PixelButtonIcon.Check
                    else -> PixelButtonIcon.Repeat
                }, colors.onSurfaceVariant)
                Text(summary.status, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            Text(summary.schedule, Modifier.testTag("repeat-schedule-${item.rule.id}"),
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(summary.range, Modifier.testTag("repeat-range-${item.rule.id}"),
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        TaskActionDivider()
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            // Keep visible short labels, but identify the series on each native action node.
            TaskActionButton(if (item.breakKind == null) "暂停" else "恢复",
                null, onPause,
                Modifier.testTag("repeat-pause-${item.rule.id}"), enabled = enabled,
                description = "${if (item.breakKind == null) "暂停" else "恢复"}重复任务：${item.rule.title}")
            TaskActionButton("修改规则", null, onEdit,
                Modifier.testTag("repeat-edit-${item.rule.id}"), enabled = enabled,
                description = "修改重复任务规则：${item.rule.title}")
            if (item.breakKind == null) TaskActionButton("停止", null, onStop,
                Modifier.testTag("repeat-stop-${item.rule.id}"), PixelButtonVariant.Danger, enabled,
                description = "停止重复任务：${item.rule.title}")
        }
    }
}

@Composable
internal fun RepeatRuleEditor(rule: TaskRepeatRule, effectiveDate: LocalDate, onSave: (TaskRepeatRule, () -> Unit) -> Unit, onDismiss: () -> Unit, saveError: String?) {
    val columns = if (LocalDensity.current.fontScale >= 1.3f) 2 else 4
    var title by remember(rule, effectiveDate) { mutableStateOf(rule.title) }
    var days by remember(rule, effectiveDate) { mutableStateOf(rule.weekdays) }
    var end by remember(rule, effectiveDate) { mutableStateOf(rule.endDate) }
    var plan by remember(rule, effectiveDate) { mutableStateOf(rule.plan) }
    var difficulty by remember(rule, effectiveDate) { mutableStateOf(rule.difficulty) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var wheelVisible by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(DateWheelSelection(end ?: effectiveDate)) }
    val endFocus = remember { FocusRequester() }
    var restoreEndFocus by remember { mutableStateOf(false) }
    LaunchedEffect(restoreEndFocus) {
        if (restoreEndFocus) {
            withFrameNanos { }
            withFrameNanos { }
            endFocus.requestFocus(); restoreEndFocus = false
        }
    }
    LaunchedEffect(saveError) { if (saveError != null) { saving = false; error = saveError } }
    TaskActionDialog("修改规则", "repeat-rule-editor", onDismiss,
        closeLabel = "关闭规则编辑", closeTag = "repeat-rule-close",
        subtitle = "${effectiveDate.year}年${effectiveDate.monthValue}月${effectiveDate.dayOfMonth}日起生效",
        error = error, dismissEnabled = !saving, avoidIme = true,
        footer = {
            TaskActionDivider()
            PixelButton("保存后续规则", {
                if (!saving) {
                    runCatching { editedRepeatRule(rule, effectiveDate, title, days, end, plan, difficulty) }
                        .onSuccess { saving = true; error = null; onSave(it, onDismiss) }
                        .onFailure { error = it.message ?: "请检查重复规则" }
                }
            }, Modifier.fillMaxWidth().testTag("repeat-rule-save"), loading = saving)
            PixelButton("取消", onDismiss, Modifier.fillMaxWidth().testTag("repeat-rule-cancel"),
                PixelButtonVariant.Ghost, enabled = !saving)
        },
    ) {
        PixelTextInput("任务标题", title, "", "重复规则标题", { title = it }, singleLine = true,
            framed = false, enabled = !saving)
        Text("重复周期", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        PixelButton("每天", { days = DayOfWeek.entries.toSet() }, Modifier.testTag("repeat-rule-daily"),
            PixelButtonVariant.Ghost, enabled = !saving, selected = days.size == 7)
        DayOfWeek.entries.chunked(columns).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                chunk.forEach { day ->
                    PixelButton("周${listOf("一","二","三","四","五","六","日")[day.ordinal]}",
                        { days = if (day in days) days - day else days + day },
                        Modifier.weight(1f).testTag("repeat-rule-day-${day.value}").semantics { selected = day in days },
                        PixelButtonVariant.Ghost, enabled = !saving, selected = day in days)
                }
            }
        }
        TaskActionDivider()
        Text("日期范围", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text("开始 ${rule.startDate}", Modifier.testTag("repeat-rule-start"),
            color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
        TaskActionButton("结束 ${end ?: "无结束日"}", PixelButtonIcon.Calendar,
            { selection = DateWheelSelection(end ?: effectiveDate); wheelVisible = true },
            Modifier.fillMaxWidth().focusRequester(endFocus).testTag("repeat-rule-end"), enabled = !saving,
            description = "结束日，${end?.let { "$it，包含当日" } ?: "无结束日"}，选择日期")
        if (end != null) TaskActionButton("无结束日", null, { end = null },
            Modifier.testTag("repeat-rule-end-clear"), enabled = !saving)
        TaskActionDivider()
        Text("计划安排", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        TaskPlanPeriod.entries.chunked(columns).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                chunk.forEach { period ->
                    PixelButton(listOf("随时","早","中","晚")[period.ordinal], { plan = TaskPlan(period) },
                        Modifier.weight(1f).testTag("repeat-rule-period-${period.name}"), PixelButtonVariant.Ghost,
                        enabled = !saving, selected = plan.time == null && plan.period == period,
                        selectedColors = LocalMmExtendedColors.current.task.period(period))
                }
            }
        }
        rule.plan.time?.let { time ->
            PixelButton("精确时间 $time", { plan = rule.plan }, Modifier.fillMaxWidth().testTag("repeat-rule-exact-time"),
                PixelButtonVariant.Ghost, enabled = !saving, selected = plan.time == time,
                selectedColors = LocalMmExtendedColors.current.task.period(rule.plan.resolvedPeriod))
        }
        TaskActionDivider()
        com.cch.momentmark.ui.task.DifficultyPanel(difficulty, { difficulty = it }, enabled = !saving, framed = false)
    }
    PixelDateWheel(wheelVisible, selection, dateWheelYearRange(effectiveDate, selection.date),
        DateWheelCopy("选择结束日", preview = "${selection.date}（包含当日）",
            year = "年", month = "月", day = "日", today = "生效日", cancel = "取消", confirm = "确认结束日"),
        onYearSelected = { selection = selection.withYear(it) },
        onMonthSelected = { selection = selection.withMonth(it) },
        onDaySelected = { selection = selection.withDay(it) },
        onToday = { selection = DateWheelSelection(effectiveDate) },
        onConfirm = { end = selection.date; wheelVisible = false },
        onDismiss = { wheelVisible = false },
        onClosed = { restoreEndFocus = true })
}

@Composable
internal fun RepeatInstanceActions(task: Task, ruleToday: LocalDate, futureEditDate: LocalDate,
    onEdit: () -> Unit, onFutureEdit: () -> Unit, onSkip: (Boolean) -> Unit, onPause: (Boolean) -> Unit,
    onMakeUp: () -> Unit = {}, onSeal: () -> Unit, onDismiss: () -> Unit, canEditFuture: Boolean = true) {
    var accepted by remember(task.id) { mutableStateOf(false) }
    fun accept(action: () -> Unit) { if (!accepted) { accepted = true; action() } }
    val today = task.repeatOccurrenceDate == ruleToday
    TaskActionDialog(task.title, "repeat-task-actions", onDismiss,
        subtitle = "执行日 ${task.repeatOccurrenceDate}") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            if (!task.isSkipped) {
                TaskActionButton("编辑当天", PixelButtonIcon.Edit, { accept(onEdit) },
                    Modifier.weight(1f).testTag("repeat-edit-day"), PixelButtonVariant.Primary, enabled = !accepted,
                    description = "编辑当天任务，不修改后续规则")
            }
            // Opening a child editor does not consume an action; Back returns here.
            TaskActionButton("修改后续", PixelButtonIcon.Repeat, onFutureEdit,
                Modifier.weight(1f).testTag("repeat-edit-future"), enabled = !accepted && canEditFuture,
                description = if (futureEditDate == task.repeatOccurrenceDate) "修改后续规则，从本执行日起生效"
                    else "修改后续规则，从今天起生效")
        }
        if (!task.isCompleted) {
            TaskActionDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                if (today) TaskActionButton(if (task.isSkipped) "撤销跳过" else "当天跳过", PixelButtonIcon.Skip,
                    { accept { onSkip(!task.isSkipped) } }, Modifier.weight(1f).testTag("repeat-skip"), enabled = !accepted)
                TaskActionButton("明天补做", PixelButtonIcon.Calendar, { accept(onMakeUp) },
                    Modifier.weight(1f).testTag("repeat-makeup"), enabled = !accepted,
                    description = "明天补做，新建单次任务，保留原实例")
            }
        }
        TaskActionDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            TaskActionButton("暂停系列", PixelButtonIcon.Pause, { accept { onPause(false) } },
                Modifier.weight(1f).testTag("repeat-pause"), enabled = !accepted,
                description = "暂停系列，从今天起")
            TaskActionButton("停止系列", PixelButtonIcon.Stop, onClick = { onPause(true) },
                Modifier.weight(1f).testTag("repeat-stop"), enabled = !accepted,
                description = "停止系列，从今天起，保留历史")
        }
        TaskActionDivider()
        TaskActionButton("封印当天", PixelButtonIcon.Seal, { accept(onSeal) },
            Modifier.testTag("repeat-seal"), PixelButtonVariant.Danger, enabled = !accepted)
    }
}
