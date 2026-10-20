package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.layout.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.ui.components.*
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate

@Composable
internal fun SingleTaskActions(task: Task, today: LocalDate, onEdit: () -> Unit,
    onMove: (LocalDate) -> Unit, onAuto: (Boolean) -> Unit, onSeal: () -> Unit, onDismiss: () -> Unit) {
    var picking by remember(task.id) { mutableStateOf(false) }
    var accepted by remember(task.id) { mutableStateOf(false) }
    fun accept(action: () -> Unit) { if (!accepted) { accepted = true; action() } }
    var selection by remember(task.id) { mutableStateOf(DateWheelSelection(task.dueLocalDate)) }
    if (picking) {
        PixelDateWheel(visible = true, selection = selection, yearRange = dateWheelYearRange(today, selection.date),
            copy = DateWheelCopy("选择安排日", selection.date.toString(), "保留原截止与时段", "精确计划时间将清除", "年", "月", "日", "今天", "取消", "安排"),
            onYearSelected = { selection = selection.withYear(it) }, onMonthSelected = { selection = selection.withMonth(it) },
            onDaySelected = { selection = selection.withDay(it) }, onToday = { selection = DateWheelSelection(today) },
            onConfirm = { accept { onMove(selection.date) } }, onDismiss = { picking = false })
    } else TaskActionDialog(task.title, "single-task-actions", onDismiss,
        subtitle = "归属日 ${task.dueLocalDate}") {
        TaskActionButton("编辑", PixelButtonIcon.Edit, { accept(onEdit) },
            Modifier.testTag("task-action-edit"), variant = PixelButtonVariant.Primary, enabled = !accepted)
        if (!task.isCompleted) {
            TaskActionDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                TaskActionButton("今天", PixelButtonIcon.Calendar, { accept { onMove(today) } },
                    Modifier.weight(1f).testTag("task-move-today"), enabled = !accepted,
                    description = "安排到今天，保留截止与时段，清除精确计划时间")
                TaskActionButton("明天", PixelButtonIcon.Calendar, { accept { onMove(today.plusDays(1)) } },
                    Modifier.weight(1f).testTag("task-move-tomorrow"), enabled = !accepted,
                    description = "安排到明天，保留截止与时段，清除精确计划时间")
                TaskActionButton("另选", PixelButtonIcon.Calendar, { picking = true },
                    Modifier.weight(1f).testTag("task-move-pick"), enabled = !accepted,
                    description = "另选安排日，保留截止与时段，清除精确计划时间")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                Text("自动顺延", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium)
                TaskActionButton(if (task.autoDefer) "已开启" else "已关闭", PixelButtonIcon.Repeat,
                    { accept { onAuto(!task.autoDefer) } }, Modifier.testTag("task-auto-defer"),
                    enabled = !accepted, description = "自动顺延，回到随时，不延长截止", checked = task.autoDefer)
            }
        }
        TaskActionDivider()
        TaskActionButton("封印", PixelButtonIcon.Seal, { accept(onSeal) },
            Modifier.testTag("task-action-seal"), PixelButtonVariant.Danger, enabled = !accepted)
    }
}

/** Presentation only. The owner supplies the observed rows and each record's write state. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DaybookPendingControls(
    tasks: List<Task>, today: LocalDate, enabled: Boolean, onMove: (Task, LocalDate) -> Unit,
    onComplete: (Task) -> Unit = {}, busy: Set<String> = emptySet(), errors: Map<String, String> = emptyMap(),
    loadError: String? = null, onRetry: () -> Unit = {}, onEmptyDismiss: () -> Unit = {}, loading: Boolean = false,
) {
    var open by remember(today) { mutableStateOf(false) }
    var restoreFocus by remember(today) { mutableStateOf(false) }
    val entryFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val hostView = androidx.compose.ui.platform.LocalView.current
    fun dismiss() { open = false; restoreFocus = true }
    LaunchedEffect(enabled) { if (!enabled && open) dismiss() }
    LaunchedEffect(open, restoreFocus, enabled) {
        if (!open && restoreFocus && enabled) {
            withFrameNanos { }
            while (!hostView.hasWindowFocus()) withFrameNanos { }
            if (tasks.isNotEmpty() || loadError != null) entryFocus.requestFocus() else onEmptyDismiss()
            restoreFocus = false
        }
    }
    if (tasks.isNotEmpty() || loadError != null) Box(Modifier.fillMaxWidth().padding(end = MomentMarkTokens.SpaceUnit * 4)) {
        PixelButton(if (loadError == null) "待安排 ${tasks.size}" else "待安排 · 加载失败", { open = true },
            Modifier.focusRequester(entryFocus).focusProperties { canFocus = enabled }.testTag("daybook-pending-toggle")
                .semantics { stateDescription = if (open) "已打开" else "已关闭" },
            variant = PixelButtonVariant.Ghost, enabled = enabled,
            icon = PixelButtonIcon.Calendar, layout = PixelButtonLayout.Compact)
    }
    if (open && enabled) TaskActionDialog("待安排", "daybook-pending-manager", ::dismiss,
        closeLabel = "关闭待安排任务", closeTag = "daybook-pending-close", subtitle = "$today · ${tasks.size}项任务") {
        when {
            loadError != null -> {
                TaskActionNote("加载失败：$loadError")
                TaskActionButton("重试", PixelButtonIcon.Repeat, onRetry, Modifier.testTag("pending-retry"))
            }
            loading -> TaskActionNote("正在加载待安排任务…")
            tasks.isEmpty() -> Text("没有待安排任务了。", Modifier.testTag("daybook-pending-empty"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            else -> tasks.sortedWith(compareBy<Task> { it.dueLocalDate }.thenBy { it.createdAt }.thenBy { it.id }).forEach { task ->
                key(task.id) {
                    val processing = task.id in busy
                    Column(Modifier.fillMaxWidth().testTag("pending-row-${task.id}"),
                        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                        Text(task.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        TaskActionNote("原归属日 ${task.dueLocalDate}")
                        if (processing) TaskActionNote("正在保存…")
                        errors[task.id]?.let { Text("保存失败：$it；请重试", Modifier.testTag("pending-error-${task.id}"),
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
                            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                            TaskActionButton("今天", PixelButtonIcon.Calendar, { onMove(task, today) },
                                Modifier.testTag("pending-today-${task.id}"), enabled = !processing,
                                description = "${task.title}，移到今天")
                            TaskActionButton("明天", PixelButtonIcon.Calendar, { onMove(task, today.plusDays(1)) },
                                Modifier.testTag("pending-tomorrow-${task.id}"), enabled = !processing,
                                description = "${task.title}，移到明天")
                            TaskActionButton("已完成", PixelButtonIcon.Check, { onComplete(task) },
                                Modifier.testTag("pending-complete-${task.id}"), enabled = !processing,
                                description = "${task.title}，标记原日已完成")
                        }
                    }
                }
            }
        }
    }
}
