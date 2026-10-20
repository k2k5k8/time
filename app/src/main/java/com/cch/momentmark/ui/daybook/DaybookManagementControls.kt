package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskRepeatRule
import com.cch.momentmark.ui.components.PixelButtonIcon
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.Clock

/** Page-level tools, outside all month previews. Completed records always belong to selectedDate. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DaybookManagementControls(
    state: DaybookUiState, enabled: Boolean,
    onToggle: (Task) -> Unit, onEditTask: (String) -> Unit, onSeal: (Task) -> Unit,
    onPause: (String, Boolean) -> Unit, onResume: (String) -> Unit,
    onEditRule: (TaskRepeatRule, () -> Unit) -> Unit, onRetry: () -> Unit,
    clock: Clock, plans: Map<String, TaskExecutionPlan>,
    completingIds: Set<String> = emptySet(),
) {
    val date = state.selectedDate
    var completedOpen by remember(date) { mutableStateOf(false) }
    var restoreFocus by remember(date) { mutableStateOf(false) }
    val entryFocus = remember { FocusRequester() }
    val hostView = LocalView.current
    val ready = state.hasLoadedTasks && state.tasksDate == date && !state.isLoading && state.loadError == null
    val completed = if (ready) projectDaybookTasks(state.tasks.filter { it.id !in completingIds }, clock, plans).completed else null
    val countLabel = when {
        state.loadError != null -> "加载失败"
        completed != null -> completed.size.toString()
        else -> "加载中"
    }
    fun dismiss() { completedOpen = false; restoreFocus = true }
    LaunchedEffect(completedOpen, restoreFocus) {
        if (!completedOpen && restoreFocus) {
            withFrameNanos { }
            while (!hostView.hasWindowFocus()) withFrameNanos { }
            entryFocus.requestFocus(); restoreFocus = false
        }
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(top = MomentMarkTokens.SpaceCard, bottom = MomentMarkTokens.SpaceInner)
            .testTag("daybook-management-tools"),
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
    ) {
        TaskActionButton("已完成 $countLabel", PixelButtonIcon.Check,
            { completedOpen = true }, Modifier.focusRequester(entryFocus).testTag("daybook-completed-toggle"),
            enabled = enabled && ready,
            description = if (completed != null) "$date，已完成，${completed.size}项任务，打开管理" else "$date，已完成，$countLabel")
        DaybookRepeatControls(state.repeatSeries, enabled, onPause, onResume, onEditRule, clock,
            state.errorMessage, loaded = state.hasLoadedRepeatSeries)
    }
    if (completedOpen) TaskActionDialog(
        "已完成", "daybook-completed-manager", ::dismiss,
        closeLabel = "关闭已完成任务", closeTag = "daybook-completed-close",
        subtitle = "$date · ${completed?.let { "${it.size}项任务" } ?: countLabel}", error = state.errorMessage,
    ) {
        when {
            state.loadError != null -> {
                TaskActionNote("加载失败：${state.loadError}")
                TaskActionButton("重试", PixelButtonIcon.Repeat, onRetry,
                    Modifier.testTag("daybook-completed-retry"), enabled = enabled)
            }
            completed == null -> TaskActionNote("正在加载这一天的已完成任务…")
            completed.isEmpty() -> Text("这一天还没有已完成任务。", Modifier.testTag("daybook-completed-empty"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            else -> DaybookTaskList(date, completed, enabled && ready, onToggle, onEditTask, onSeal,
                plans, clock, completedOnly = true, completionBusy = state.completionBusy)
        }
    }
}
