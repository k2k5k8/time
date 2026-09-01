package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** J ⑤ 日子簿：真实 Task 按所属日期显示，切日只改变查询，绝不迁移过期任务。 */
@Composable
fun DaybookScreen(
    uiState: DaybookUiState,
    onAcceptNewTask: () -> Unit,
    onOpenSettings: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (Long) -> Unit,
    onTaskCompletionToggled: (Task) -> Unit,
    onTaskEditRequested: (String) -> Unit,
    onTaskSealRequested: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingSealTask by remember { mutableStateOf<Task?>(null) }
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = MomentMarkTokens.SpacePage, vertical = MomentMarkTokens.SpaceCard),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        PageTitlePanel(
            title = "${uiState.visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINA)} ${uiState.visibleMonth.year}",
            hud = "DUNGEON · CAL-${uiState.visibleMonth.monthValue.toString().padStart(2, '0')}",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .size(MomentMarkTokens.TouchTargetMin)
                    .semantics { contentDescription = "打开系统设置" }
                    .clickable(role = Role.Button, onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                PixelPanel(
                    modifier = Modifier.size(MomentMarkTokens.CompactIconSize),
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    shadowColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = MomentMarkTokens.PxShadowOffsetSmall,
                ) {
                    Text("⚙", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiary)
                }
            }
        }
        MonthGrid(
            month = uiState.visibleMonth,
            selectedDate = uiState.selectedDate,
            taskDates = uiState.taskDates,
            today = uiState.today,
            onDateSelected = onDateSelected,
            onPreviousMonth = { onMonthChanged(-1) },
            onNextMonth = { onMonthChanged(1) },
        )
        PixelPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
                Text(
                    text = if (uiState.selectedDate == uiState.today) {
                        "▸ 今日掉落 · ${uiState.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} TODO"
                    } else {
                        "▸ ${uiState.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} 的掉落 TODO"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (uiState.isLoading) {
                    Text("▸ LOADING…", style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
                } else if (uiState.tasks.isEmpty()) {
                    Text("这一天还没有任务掉落。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = MomentMarkTokens.SpaceUnit * 2))
                } else {
                    uiState.tasks.forEach { task ->
                        TaskRow(
                            task = task,
                            onToggleCompleted = { onTaskCompletionToggled(task) },
                            onEdit = { onTaskEditRequested(task.id) },
                            onSeal = { pendingSealTask = task },
                        )
                    }
                }
                NewQuestEntryRow(onClick = onAcceptNewTask)
                uiState.errorMessage?.let { Text("⚠ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    pendingSealTask?.let { task ->
        PixelConfirmationDialog(
            title = "放弃「${task.title}」？",
            message = "任务描述与完成状态将被封存进回收站，30 天内可复活。",
            confirmLabel = "✕ 确认放弃",
            onConfirm = {
                pendingSealTask = null
                onTaskSealRequested(task)
            },
            onDismiss = { pendingSealTask = null },
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    taskDates: Set<LocalDate>,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = MomentMarkTokens.SpaceInner),
    ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MonthMoveButton("◂", "上个月", onPreviousMonth)
                Text("▸ 选择副本日期", style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
                MonthMoveButton("▸", "下个月", onNextMonth)
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA").forEach { weekday ->
                    Text(weekday, style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary,
                        modifier = Modifier.weight(1f),)
                }
            }
            val leading = month.atDay(1).dayOfWeek.sundayIndex()
            val rows = (leading + month.lengthOfMonth() + 6) / 7
            val previousMonth = month.minusMonths(1)
            repeat(rows) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (row == 0) MomentMarkTokens.SpaceUnit else MomentMarkTokens.SpaceUnit),
                    horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
                ) {
                    repeat(7) { column ->
                        val cellIndex = row * 7 + column
                        val day = cellIndex - leading + 1
                        val date = when {
                            day < 1 -> previousMonth.atDay(previousMonth.lengthOfMonth() + day)
                            day > month.lengthOfMonth() -> month.plusMonths(1).atDay(day - month.lengthOfMonth())
                            else -> month.atDay(day)
                        }
                        DayCell(
                            date = date,
                            inVisibleMonth = day in 1..month.lengthOfMonth(),
                            selected = date == selectedDate,
                            hasTasks = date in taskDates,
                            today = date == today,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
}

@Composable
private fun MonthMoveButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(MomentMarkTokens.TouchTargetMin)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inVisibleMonth: Boolean,
    selected: Boolean,
    hasTasks: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val selectedColor = scheme.tertiary
    Box(
        modifier = modifier
            // Relative cell geometry follows the HTML calendar proportions;
            // no device/card width is hard-coded.
            .aspectRatio(0.92f)
            .semantics {
                contentDescription = "${date} ${if (selected) "已选择" else ""}${if (hasTasks) "，有任务" else ""}"
            }
            .then(
                if (inVisibleMonth) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .background(
                when {
                    today -> selectedColor
                    selected -> scheme.secondary
                    else -> scheme.surface
                },
            )
            .drawBehind {
                drawRect(
                    color = scheme.outline,
                    style = Stroke(width = MomentMarkTokens.PxThinBorderWidth.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !inVisibleMonth -> scheme.outlineVariant
                today -> scheme.onTertiary
                selected -> scheme.onSecondary
                else -> scheme.onSurface
            },
        )
        if (hasTasks && inVisibleMonth) {
            Text(
                text = "◆",
                style = MaterialTheme.typography.labelSmall,
                color = if (today) scheme.onTertiary else scheme.tertiary,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun DayOfWeek.sundayIndex(): Int = if (this == DayOfWeek.SUNDAY) 0 else value

@Composable
private fun TaskRow(task: Task, onToggleCompleted: () -> Unit, onEdit: () -> Unit, onSeal: () -> Unit) {
    val separatorColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MomentMarkTokens.SpaceCompact)
            .drawBehind {
                drawLine(
                    color = separatorColor,
                    start = Offset.Zero.copy(y = size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = MomentMarkTokens.PxThinBorderWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            MomentMarkTokens.SpaceUnit.toPx(),
                            MomentMarkTokens.SpaceUnit.toPx(),
                        ),
                    ),
                )
            },
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheckbox(task.isCompleted, task.title, onToggleCompleted)
        Text(
            task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isCompleted) LocalMmExtendedColors.current.labelTertiary else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        Text(
            "编辑",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .semantics { contentDescription = "编辑 ${task.title}" }
                .clickable(role = Role.Button, onClick = onEdit),
        )
        Text(
            "放弃",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { contentDescription = "放弃 ${task.title}" }.clickable(role = Role.Button, onClick = onSeal),
        )
        Text(
            text = if (task.isCompleted) "CLEAR +5 EXP" else task.dueInstant?.atZone(task.zoneId ?: java.time.ZoneId.systemDefault())
                ?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "TODO",
            style = MaterialTheme.typography.labelSmall,
            color = if (task.isCompleted) MaterialTheme.colorScheme.primary else LocalMmExtendedColors.current.labelTertiary,
            modifier = Modifier.semantics { contentDescription = if (task.isCompleted) "${task.title} CLEAR 加 5 经验" else "${task.title} ${task.dueInstant?.let { "截止时间" } ?: "TODO"}" },
        )
    }
}

@Composable
private fun TaskCheckbox(checked: Boolean, title: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(MomentMarkTokens.TouchTargetMin).semantics { contentDescription = "$title ${if (checked) "已完成" else "未完成"}" }
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(MomentMarkTokens.CheckboxSize)
                .background(if (checked) primary else MaterialTheme.colorScheme.background)
                .drawBehind {
                    drawRect(color = primary, style = Stroke(width = MomentMarkTokens.CheckboxStrokeWidth.toPx()))
                },
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun NewQuestEntryRow(onClick: () -> Unit) {
    val questGreen = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = MomentMarkTokens.SpaceUnit * 2),
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(MomentMarkTokens.CheckboxSize).drawBehind {
                drawRect(
                    color = questGreen,
                    style = Stroke(
                        width = MomentMarkTokens.CheckboxStrokeWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(MomentMarkTokens.SpaceUnit.toPx(), (MomentMarkTokens.SpaceUnit / 2).toPx())),
                    ),
                )
            },
        )
        Text("＋ 接受新任务（待办）", style = MaterialTheme.typography.titleSmall, color = questGreen)
        Text("NEW QUEST", style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
    }
}
