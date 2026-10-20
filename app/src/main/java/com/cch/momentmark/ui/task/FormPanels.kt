package com.cch.momentmark.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import com.cch.momentmark.ui.components.*
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.LocalMmExtendedColors

/** Optional energy: only a user click toggles; assignment/refill callbacks keep their meaning. */
@Composable
internal fun DifficultyPanel(difficulty: Int?, onDifficultySelected: (Int?) -> Unit, enabled: Boolean = true, framed: Boolean = true) {
    val density = LocalDensity.current
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().then(if (framed) Modifier.padding(MomentMarkTokens.FormPanelPaddingHorizontal) else Modifier),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            TaskFormSectionLabel("难度 · 精力投入")
            @Composable fun option(value: Int, modifier: Modifier) {
                val label = listOf("轻量", "适中", "费力")[value - 1]
                val isSelected = difficulty == value
                val action = { onDifficultySelected(if (isSelected) null else value) }
                PixelButton("${"★".repeat(value)}\n$label", action,
                    modifier.testTag("task-energy-$value").clearAndSetSemantics {
                        contentDescription = "$value 星，$label，精力投入"
                        role = Role.Button
                        selected = isSelected
                        stateDescription = if (isSelected) "已选择，再次点击取消" else "未选择"
                        if (enabled) onClick(if (isSelected) "取消$label" else "选择$label") { action(); true }
                        else disabled()
                    }, variant = PixelButtonVariant.Ghost,
                    selected = isSelected, selectedColors = LocalMmExtendedColors.current.task.effort(value),
                    enabled = enabled)
            }
            if (density.fontScale > 1.3f) {
                (1..3).forEach { option(it, Modifier.fillMaxWidth()) }
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                (1..3).forEach { option(it, Modifier.weight(1f)) }
            }
        }
    }
    if (framed) PixelPanel(Modifier.fillMaxWidth()) { body() } else body()
}

@Composable
internal fun TaskFormSectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One main boundary per section; embedded fields own only their input/control border. */
@Composable
internal fun TaskFormSection(tag: String, content: @Composable ColumnScope.() -> Unit) {
    PixelPanel(Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.fillMaxWidth().padding(
            horizontal = MomentMarkTokens.FormPanelPaddingHorizontal,
            vertical = MomentMarkTokens.FormPanelPaddingVertical),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard), content = content)
    }
}

/** Fine local connector points at the active entry without adding another raised panel. */
@Composable
internal fun TaskFormConnectedBody(activeSlot: Int, slots: Int, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    Column(modifier.fillMaxWidth().drawBehind {
        val stroke = MomentMarkTokens.PxThinBorderWidth.toPx()
        val center = size.width * (activeSlot + .5f) / slots
        val gap = MomentMarkTokens.SpaceCompact.toPx()
        drawLine(outline, Offset(0f, 0f), Offset(center - gap, 0f), stroke)
        drawLine(outline, Offset(center + gap, 0f), Offset(size.width, 0f), stroke)
        drawLine(outline, Offset(center - gap, 0f), Offset(center, gap), stroke)
        drawLine(outline, Offset(center, gap), Offset(center + gap, 0f), stroke)
        drawLine(outline, Offset(0f, 0f), Offset(0f, size.height), stroke)
        drawLine(outline, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
        drawLine(outline, Offset(0f, size.height), Offset(size.width, size.height), stroke)
    }.padding(MomentMarkTokens.SpaceInner),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard), content = content)
}
