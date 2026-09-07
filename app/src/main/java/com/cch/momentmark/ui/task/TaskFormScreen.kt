package com.cch.momentmark.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate

/** J ④ 接受新任务：只由日子簿进入，保存后回到任务所属日期。 */
@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    taskId: String?,
    onSaved: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(taskId) { viewModel.start(taskId) }
    LaunchedEffect(state.savedDueDate) {
        state.savedDueDate?.let { dueDate ->
            viewModel.consumeSaved()
            onSaved(dueDate)
        }
    }
    TaskFormContent(
        state = state,
        onTitleChanged = viewModel::updateTitle,
        onDateChanged = viewModel::updateDate,
        onTimeChanged = viewModel::updateTime,
        onNoteChanged = viewModel::updateNote,
        onGroupChanged = viewModel::updateGroup,
        onTypeSelected = viewModel::selectType,
        onDifficultySelected = viewModel::selectDifficulty,
        onShowOnHomeToggled = viewModel::toggleShowOnHome,
        onToday = viewModel::selectToday,
        onTomorrow = viewModel::selectTomorrow,
        onSaturday = viewModel::selectThisSaturday,
        onSave = viewModel::submit,
        modifier = modifier,
    )
}

@Composable
internal fun TaskFormContent(
    state: TaskFormUiState,
    onTitleChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onGroupChanged: (String) -> Unit,
    onTypeSelected: (TaskType) -> Unit,
    onDifficultySelected: (Int?) -> Unit,
    onShowOnHomeToggled: () -> Unit,
    onToday: () -> Unit,
    onTomorrow: () -> Unit,
    onSaturday: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalMmExtendedColors.current
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MomentMarkTokens.SpacePage, vertical = MomentMarkTokens.SpaceCard),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        PageTitlePanel(
            title = if (state.editingTaskId == null) "▶ 接受新任务" else "✎ 编辑任务",
            hud = if (state.editingTaskId == null) "待办 · 来自日子簿" else "TASK · EDIT",
        )
        PixelPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
                Text("▸ 快捷日期 QUICK", style = MaterialTheme.typography.labelSmall, color = extended.labelTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
                    QuickDateButton(
                        label = "今日",
                        onClick = onToday,
                        modifier = Modifier.weight(1f),
                        selected = state.dueDateText == state.quickTodayDateText,
                        selectedBackground = scheme.primary,
                        selectedContent = scheme.onPrimary,
                    )
                    QuickDateButton(
                        label = "明日",
                        onClick = onTomorrow,
                        modifier = Modifier.weight(1f),
                        selected = state.dueDateText == state.quickTomorrowDateText,
                        selectedBackground = scheme.primary,
                        selectedContent = scheme.onPrimary,
                    )
                    QuickDateButton(
                        label = "本周六",
                        onClick = onSaturday,
                        modifier = Modifier.weight(1f),
                        selected = state.dueDateText == state.quickSaturdayDateText,
                        selectedBackground = scheme.primary,
                        selectedContent = scheme.onPrimary,
                    )
                }
            }
        }
        TaskField("▸ 任务名称 NAME ★", state.title, "例如：提交项目周报", "任务名称", onTitleChanged)
        TaskField("▸ 所属日期 DUE DATE ★", state.dueDateText, "YYYY-MM-DD", "所属日期，格式 YYYY-MM-DD", onDateChanged, true)
        TaskField("▸ 截止时间 TIME（可选）", state.dueTimeText, "HH:MM", "截止时间，格式 HH:MM，可选", onTimeChanged, true)
        TaskField("▸ 任务描述 LORE（可选）", state.note, "攻略、备注或链接", "任务描述，可选", onNoteChanged)
        TaskField("▸ 分组线路 PARTY（可选）", state.groupId, "例如：工作", "任务分组，可选", onGroupChanged, true)
        PixelPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
                Text("▸ 任务类型 TYPE", style = MaterialTheme.typography.labelSmall, color = extended.labelTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
                    TaskType.entries.forEach { type ->
                        QuickDateButton(
                            label = when (type) {
                                TaskType.MAIN -> "◆ 主线"
                                TaskType.SIDE -> "◇ 支线"
                                TaskType.LIMITED -> "⧗ 限时"
                            },
                            onClick = { onTypeSelected(type) },
                            modifier = Modifier.weight(1f),
                            selected = state.taskType == type,
                        )
                    }
                }
            }
        }
        DifficultySelector(state.difficulty, onDifficultySelected)
        PixelPanel(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "显示在首页开关" }
                .clickable(role = Role.Checkbox, onClick = onShowOnHomeToggled),
            backgroundColor = if (state.showOnHome) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = if (state.showOnHome) "[x] 显示在首页大事件（已开启）" else "[ ] 显示在首页大事件（默认关闭）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(MomentMarkTokens.SpaceInner),
            )
        }
        state.errorMessage?.let { Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        PixelPanel(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "确认接受任务" }
                .clickable(
                    enabled = !state.isSaving && !state.isLoading,
                    role = Role.Button,
                    onClick = onSave,
                ),
            backgroundColor = MaterialTheme.colorScheme.primary,
            borderColor = MaterialTheme.colorScheme.outline,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = when {
                    state.isLoading -> "读取任务中…"
                    state.isSaving -> "保存中…"
                    state.editingTaskId != null -> "✎ 保存修改 · 返回日子簿"
                    else -> "▶ 接受任务！保存到日子簿"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            )
        }
    }
}

@Composable
private fun DifficultySelector(difficulty: Int?, onDifficultySelected: (Int?) -> Unit) {
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
            Text("▸ 任务难度 DIFFICULTY（可选）", style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
                (1..3).forEach { value ->
                    PixelPanel(
                        modifier = Modifier.weight(1f).semantics { contentDescription = "$value 星任务难度" }
                            .clickable(role = Role.RadioButton, onClick = { onDifficultySelected(if (difficulty == value) null else value) }),
                        backgroundColor = if (difficulty == value) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
                        shadowColor = MaterialTheme.colorScheme.outline,
                    ) {
                        Text(
                            text = "★".repeat(value),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (difficulty == value) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceUnit * 2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDateButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    selected: Boolean = false,
    selectedBackground: Color = MaterialTheme.colorScheme.secondary,
    selectedContent: Color = MaterialTheme.colorScheme.onSecondary,
) {
    PixelPanel(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        backgroundColor = if (selected) selectedBackground else MaterialTheme.colorScheme.surface,
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                selectedContent
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceUnit * 2),
        )
    }
}

@Composable
private fun TaskField(
    label: String,
    value: String,
    hint: String,
    description: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
) {
    PixelTextInput(
        label = label,
        value = value,
        placeholder = hint,
        contentDescription = description,
        onValueChange = onValueChange,
        singleLine = singleLine,
    )
}
