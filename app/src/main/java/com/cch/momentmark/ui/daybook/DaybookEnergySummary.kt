package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.RepeatDisplay
import com.cch.momentmark.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 与可执行列表一致，排除历史、完成及封印；未设置不补为轻量。 */
internal data class DaybookEnergySummary(val light: Int, val medium: Int, val heavy: Int) {
    val hasValues get() = light + medium + heavy > 0
    val label get() = "轻 $light · 中 $medium · 重 $heavy"
    val description get() = "未完成任务精力投入：轻量 $light 项，适中 $medium 项，费力 $heavy 项；未设置不计入"
}
internal fun summarizeDaybookEnergy(tasks: List<Task>): DaybookEnergySummary {
    val pending = tasks.filter { !it.isCompleted && it.deletedAt == null && it.movedTo == null && !it.isSkipped && it.repeatDisplay != RepeatDisplay.UNRECORDED }
    return DaybookEnergySummary(pending.count { it.difficulty == 1 }, pending.count { it.difficulty == 2 }, pending.count { it.difficulty == 3 })
}

/** 日期跟随已展示的任务快照；跨年时明确年份。 */
internal fun daybookTaskDateLabel(date: LocalDate, today: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MM.dd" else "yyyy.MM.dd"))

/** 标题与日期一行，完整三档另占一行；每档保持标识、名称和数值在一起。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DaybookTaskHeading(
    date: LocalDate,
    today: LocalDate,
    tasks: List<Task>,
    feedback: String? = null,
) {
    val summary = summarizeDaybookEnergy(tasks)
    val style = MaterialTheme.typography.labelSmall.copy(
        fontSize = MomentMarkTokens.DaybookTaskPanelTitleFontSize,
        lineHeight = MomentMarkTokens.DaybookTaskPanelTitleLineHeight,
        letterSpacing = MomentMarkTokens.DaybookTaskPanelTitleLetterSpacing,
        platformStyle = MmNoFontPadding)
    Column(Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
        // The slow-query overlay uses a fixed trailing slot; the shared left anchor never moves.
        Row(Modifier.fillMaxWidth().padding(end = MomentMarkTokens.SpaceUnit * 4).testTag("daybook-task-heading"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            Text(feedback?.let { "✓ $it" } ?: if (date == today) "▸ 今日掉落" else "▸ 当日掉落",
                Modifier.weight(1f).testTag("daybook-task-title"), style = style,
                color = LocalMmExtendedColors.current.goldInk)
            Text(daybookTaskDateLabel(date, today), Modifier.testTag("daybook-task-date"),
                style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (summary.hasValues) FlowRow(
            Modifier.fillMaxWidth().testTag("daybook-energy-summary")
                .clearAndSetSemantics { contentDescription = summary.description },
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 3),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            listOf("轻" to summary.light, "中" to summary.medium, "重" to summary.heavy)
                .forEachIndexed { index, (label, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                        Box(Modifier.size(MomentMarkTokens.SpaceUnit).testTag("daybook-energy-marker-$index")
                            .background(LocalMmExtendedColors.current.task.effort(index + 1).accent))
                        Text("$label $count", style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
        }
    }
}
