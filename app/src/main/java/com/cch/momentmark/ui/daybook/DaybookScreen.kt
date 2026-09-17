package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.ui.components.CalendarDayCell
import com.cch.momentmark.ui.components.NewQuestEntry
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelStatusHud
import com.cch.momentmark.ui.components.PixelTaskRow
import com.cch.momentmark.ui.components.TaskRowPresentation
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmNoFontPadding
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
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        PixelStatusHud(
            leftText = "♥\uFE0E×3",
            rightText = "DUNGEON MAP",
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DaybookTitlePanel(
            month = uiState.visibleMonth,
            hud = "DAYBOOK · CAL-${uiState.visibleMonth.monthValue.toString().padStart(2, '0')} · SWIPE",
            onPreviousMonth = { onMonthChanged(-1) },
            onNextMonth = { onMonthChanged(1) },
            onOpenSettings = onOpenSettings,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MomentMarkTokens.DaybookScrollHorizontalPadding),
        ) {
            MonthGrid(
                month = uiState.visibleMonth,
                selectedDate = uiState.selectedDate,
                taskDates = uiState.taskDates,
                today = uiState.today,
                onDateSelected = onDateSelected,
                onPreviousMonth = { onMonthChanged(-1) },
                onNextMonth = { onMonthChanged(1) },
            )
            TaskPanel(
                uiState = uiState,
                onAcceptNewTask = onAcceptNewTask,
                onTaskCompletionToggled = onTaskCompletionToggled,
                onTaskEditRequested = onTaskEditRequested,
                onTaskSealRequested = { pendingSealTask = it },
            )
        }
        // design `.form-scroll` 与 `.nav` 之间的 10px 间隔：决定滚动可视区的截断位置。
        Spacer(Modifier.height(MomentMarkTokens.DaybookScrollToNavGap))
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
private fun DaybookTitlePanel(
    month: YearMonth,
    hud: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val label = "${month.month.getDisplayName(TextStyle.FULL, Locale.CHINA)} ${month.year}"
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MomentMarkTokens.DaybookTitlePanelMarginHorizontal,
                end = MomentMarkTokens.DaybookTitlePanelMarginHorizontal,
                bottom = MomentMarkTokens.DaybookTitlePanelMarginBottom,
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.DaybookTitlePanelPaddingHorizontal,
                    vertical = MomentMarkTokens.DaybookTitlePanelPaddingVertical,
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MomentMarkTokens.DaybookTitleArrowFontSize,
                    lineHeight = MomentMarkTokens.DaybookTitleArrowLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    platformStyle = MmNoFontPadding,
                ),
                color = LocalMmExtendedColors.current.labelTertiary,
                modifier = Modifier
                    .padding(end = MomentMarkTokens.SpaceUnit * 2)
                    .clickable(onClick = onPreviousMonth),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MomentMarkTokens.DaybookTitleLabelFontSize,
                    lineHeight = MomentMarkTokens.DaybookTitleLabelLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    platformStyle = MmNoFontPadding,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.height(MomentMarkTokens.DaybookTitleLabelBoxHeight),
            )
            Text(
                text = "▶",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MomentMarkTokens.DaybookTitleArrowFontSize,
                    lineHeight = MomentMarkTokens.DaybookTitleArrowLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    platformStyle = MmNoFontPadding,
                ),
                color = LocalMmExtendedColors.current.labelTertiary,
                modifier = Modifier
                    .padding(start = MomentMarkTokens.SpaceUnit * 2)
                    .clickable(onClick = onNextMonth),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenSettings)
                    .semantics { contentDescription = "打开设置" },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = hud,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = MomentMarkTokens.DaybookTitleHudFontSize,
                        lineHeight = MomentMarkTokens.DaybookTitleHudLineHeight,
                        letterSpacing = MomentMarkTokens.DaybookTitleHudLetterSpacing,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = LocalMmExtendedColors.current.labelTertiary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = MomentMarkTokens.DaybookWeekdayHeaderPaddingTop,
                    bottom = MomentMarkTokens.DaybookWeekdayHeaderPaddingBottom,
                )
                .padding(bottom = MomentMarkTokens.DaybookWeekdayHeaderMarginBottom),
        ) {
            listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA").forEach { weekday ->
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = MomentMarkTokens.DaybookWeekdayHeaderFontSize,
                        lineHeight = MomentMarkTokens.DaybookWeekdayHeaderLineHeight,
                        letterSpacing = MomentMarkTokens.MonoLetterSpacingNone,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = LocalMmExtendedColors.current.labelTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val leading = month.atDay(1).dayOfWeek.sundayIndex()
        val rows = (leading + month.lengthOfMonth() + 6) / 7
        val previousMonth = month.minusMonths(1)
        repeat(rows) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (row == 0) 0.dp else MomentMarkTokens.DaybookCalendarGap),
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DaybookCalendarGap),
            ) {
                repeat(7) { column ->
                    val cellIndex = row * 7 + column
                    val day = cellIndex - leading + 1
                    val date = when {
                        day < 1 -> previousMonth.atDay(previousMonth.lengthOfMonth() + day)
                        day > month.lengthOfMonth() -> month.plusMonths(1).atDay(day - month.lengthOfMonth())
                        else -> month.atDay(day)
                    }
                    CalendarDayCell(
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
private fun TaskPanel(
    uiState: DaybookUiState,
    onAcceptNewTask: () -> Unit,
    onTaskCompletionToggled: (Task) -> Unit,
    onTaskEditRequested: (String) -> Unit,
    onTaskSealRequested: (Task) -> Unit,
) {
    val title = if (uiState.selectedDate == uiState.today) {
        "▸ 今日掉落 · ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MM.dd"))} TODO"
    } else {
        "▸ ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MM.dd"))} 的掉落 TODO"
    }
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MomentMarkTokens.DaybookTaskPanelMarginTop),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.DaybookTaskPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.DaybookTaskPanelPaddingVertical,
                ),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = MomentMarkTokens.DaybookTaskPanelTitleFontSize,
                    lineHeight = MomentMarkTokens.DaybookTaskPanelTitleLineHeight,
                    letterSpacing = MomentMarkTokens.DaybookTaskPanelTitleLetterSpacing,
                    platformStyle = MmNoFontPadding,
                ),
                color = LocalMmExtendedColors.current.goldInk,
            )
            if (uiState.isLoading) {
                Text(
                    "▸ LOADING…",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalMmExtendedColors.current.labelTertiary,
                )
            } else if (uiState.tasks.isEmpty()) {
                Text(
                    "这一天还没有任务掉落。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = MomentMarkTokens.SpaceUnit * 2),
                )
            } else {
                uiState.tasks.forEach { task ->
                    PixelTaskRow(
                        presentation = TaskRowPresentation(
                            title = task.title,
                            isCompleted = task.isCompleted,
                            statusLabel = statusLabelFor(task),
                            statusDescription = statusDescriptionFor(task),
                            isAllDay = task.dueInstant == null,
                        ),
                        onToggleCompleted = { onTaskCompletionToggled(task) },
                        onEdit = { onTaskEditRequested(task.id) },
                        onSeal = { onTaskSealRequested(task) },
                    )
                }
            }
            NewQuestEntry(onClick = onAcceptNewTask)
            uiState.errorMessage?.let {
                Text(
                    "⚠ $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun statusLabelFor(task: Task): String = when {
    task.isCompleted -> "CLEAR +5"
    task.dueInstant != null -> {
        val zoned = task.dueInstant.atZone(task.zoneId ?: java.time.ZoneId.systemDefault())
        "${zoned.format(DateTimeFormatter.ofPattern("HH:mm"))} 截止"
    }
    else -> "全天"
}

private fun statusDescriptionFor(task: Task): String = when {
    task.isCompleted -> "${task.title} CLEAR 加 5 经验"
    task.dueInstant != null -> "${task.title} 截止时间 ${task.dueInstant.atZone(task.zoneId ?: java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"
    else -> "${task.title} 全天任务"
}

private fun DayOfWeek.sundayIndex(): Int = if (this == DayOfWeek.SUNDAY) 0 else value
