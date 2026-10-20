package com.cch.momentmark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmNoFontPadding
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** Status pixels are independent of completion capability; callers supply both explicitly. */
enum class TaskRowMark { EMPTY, CHECK, CROSS, ARROW, SKIP, CLOCK, BUSY }

data class TaskRowPresentation(
    val title: String,
    val isCompleted: Boolean,
    val statusLabel: String,
    val statusDescription: String,
    val isAllDay: Boolean = false,
    val urgent: Boolean = false,
    val statusBelowTitle: Boolean = false,
    val titleActionLabel: String = "编辑",
    val mark: TaskRowMark = if (isCompleted) TaskRowMark.CHECK else TaskRowMark.EMPTY,
    val rowActionEnabled: Boolean = true,
    val sealEnabled: Boolean = true,
    val emphasized: Boolean = true,
    val showSeparator: Boolean = true,
    val important: Boolean = false,
    val showRiskIcon: Boolean = false,
    val indicatorsBelowTitle: Boolean = false,
    val completionPending: Boolean = false,
    val strikeProgress: Float? = null,
)

@Composable
fun PixelTaskRow(
    presentation: TaskRowPresentation,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onSeal: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    completionEnabled: Boolean = enabled,
) {
    val title = presentation.title
    var titleLayout by remember(title) { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val deviceDensity = LocalView.current.resources.displayMetrics.density
    val touchTarget = with(density) { (MomentMarkTokens.ButtonTouchTargetMin.value * deviceDensity).toDp() }
    val markTop = MomentMarkTokens.DaybookTaskRowPaddingVertical +
        ((MomentMarkTokens.DaybookTaskTitleLineHeight.value * density.fontScale - MomentMarkTokens.CheckboxSize.value) / 2f).coerceAtLeast(0f).dp
    val separatorColor = if (presentation.emphasized) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = if (presentation.isCompleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val description = buildString {
        append("${title}，${presentation.statusDescription}")
        if (presentation.rowActionEnabled) append("，点击${presentation.titleActionLabel}")
        if (presentation.sealEnabled) append("，长按放弃")
    }
    // Clear child text/role virtual nodes: Android must receive the Chinese label and action together.
    // Executable rows retain a separate completion node; history has one whole-row node.
    val actions = (when {
        presentation.rowActionEnabled && presentation.sealEnabled -> Modifier.pixelCombinedClickable(
            enabled = enabled, onClick = onEdit, onLongClick = onSeal,
            onClickLabel = presentation.titleActionLabel, onLongClickLabel = "放弃任务")
        presentation.rowActionEnabled -> Modifier.pixelClickable(enabled = enabled,
            onClickLabel = presentation.titleActionLabel, onClick = onEdit)
        else -> Modifier
    }).clearAndSetSemantics {
        contentDescription = description
        text = AnnotatedString(title)
        if (presentation.rowActionEnabled) {
            role = Role.Button
            if (enabled) onClick(presentation.titleActionLabel) { onEdit(); true } else disabled()
        }
        if (presentation.sealEnabled && enabled) onLongClick("放弃任务") { onSeal(); true }
    }
    // Keep the separator at the outside edge. Long titles grow the row without moving its first-line mark.
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MomentMarkTokens.DaybookTaskRowHeight)
            .drawBehind {
                if (!presentation.showSeparator) return@drawBehind
                val stroke = MomentMarkTokens.DaybookTaskRowBorderWidth.toPx()
                val y = size.height - stroke / 2f
                drawLine(
                    color = separatorColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(
                            MomentMarkTokens.DaybookTaskRowDashOn.toPx(),
                            MomentMarkTokens.DaybookTaskRowDashOff.toPx(),
                        ),
                    ),
                )
            }
            .then(if (!completionEnabled) actions else Modifier),
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        verticalAlignment = Alignment.Top,
    ) {
        // A fixed real-device hit slot keeps every mark/title aligned; read-only marks have no action node.
        Box(Modifier.widthIn(min = touchTarget).heightIn(min = touchTarget), contentAlignment = Alignment.TopCenter) {
            if (completionEnabled) {
                PixelTaskCheckbox(presentation.isCompleted, title, enabled, onToggleCompleted,
                    presentation.mark, touchTarget, markTop, presentation.completionPending)
            } else {
                PixelTaskMark(presentation.mark, Modifier.padding(top = markTop))
            }
        }
        Row(Modifier.weight(1f).heightIn(min = touchTarget).then(if (completionEnabled) actions else Modifier),
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(vertical = MomentMarkTokens.DaybookTaskRowPaddingVertical)) {
                Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                if (presentation.important && !presentation.indicatorsBelowTitle) PixelButtonGlyph(PixelButtonIcon.Star, LocalMmExtendedColors.current.task.important.accent)
                if (presentation.showRiskIcon && presentation.urgent && !presentation.indicatorsBelowTitle) Text("!", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.clearAndSetSemantics {})
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth().testTag("task-title-strike").drawWithContent {
                        drawContent()
                        val layout = titleLayout ?: return@drawWithContent
                        val progress = presentation.strikeProgress ?: return@drawWithContent
                        for (line in 0 until layout.lineCount) {
                            val left = layout.getLineLeft(line)
                            val right = layout.getLineRight(line)
                            val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
                            drawLine(titleColor, Offset(left, y), Offset(left + (right - left) * progress, y), 1.dp.toPx())
                        }
                    },
                    onTextLayout = { titleLayout = it },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (presentation.emphasized) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = MomentMarkTokens.DaybookTaskTitleLineHeight,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = titleColor,
                    textDecoration = if (presentation.isCompleted && presentation.strikeProgress == null) TextDecoration.LineThrough else TextDecoration.None,
                )
                }
                if (presentation.statusBelowTitle && presentation.statusLabel.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                        if (presentation.indicatorsBelowTitle) {
                            if (presentation.important) PixelButtonGlyph(PixelButtonIcon.Star, LocalMmExtendedColors.current.task.important.accent)
                            if (presentation.showRiskIcon && presentation.urgent) Text("!", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.clearAndSetSemantics {})
                        }
                        TaskStatus(presentation)
                    }
                }
            }
            if (!presentation.statusBelowTitle && presentation.statusLabel.isNotEmpty()) {
                Box(Modifier.padding(top = MomentMarkTokens.DaybookTaskRowPaddingVertical)) { TaskStatus(presentation) }
            }
        }
    }
}

@Composable
private fun TaskStatus(presentation: TaskRowPresentation) {
    Text(
        text = presentation.statusLabel,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = MomentMarkTokens.DaybookTaskStatusFontSize,
            lineHeight = MomentMarkTokens.DaybookTaskStatusLineHeight,
            letterSpacing = MomentMarkTokens.MonoLetterSpacingNone,
            platformStyle = MmNoFontPadding,
        ),
        color = when {
            presentation.urgent -> MaterialTheme.colorScheme.error
            presentation.isCompleted -> MaterialTheme.colorScheme.primary
            presentation.mark in setOf(TaskRowMark.ARROW, TaskRowMark.SKIP, TaskRowMark.CLOCK) ||
                (presentation.mark == TaskRowMark.CROSS && !presentation.rowActionEnabled) -> MaterialTheme.colorScheme.onSurfaceVariant
            presentation.isAllDay -> LocalMmExtendedColors.current.goldInk
            else -> LocalMmExtendedColors.current.labelTertiary
        },
        modifier = (if (presentation.statusBelowTitle) Modifier.fillMaxWidth() else Modifier.widthIn(max = MomentMarkTokens.DaybookTaskStatusMaxWidth))
            .clearAndSetSemantics {},
    )
}

@Composable
private fun PixelTaskCheckbox(checked: Boolean, title: String, enabled: Boolean, onClick: () -> Unit,
    mark: TaskRowMark, touchTarget: androidx.compose.ui.unit.Dp, markTop: androidx.compose.ui.unit.Dp, pending: Boolean) {
    Box(Modifier.size(touchTarget)
        .pixelClickable(enabled = enabled, role = Role.Checkbox,
            onClickLabel = if (checked) "取消完成" else "标为完成", onClick = onClick)
        .clearAndSetSemantics {
            contentDescription = "$title ${if (checked) "已完成" else "未完成"}；复选框"
            role = Role.Checkbox
            if (pending) stateDescription = "正在保存完成状态"
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            if (enabled) onClick(if (checked) "取消完成" else "标为完成") { onClick(); true } else disabled()
        },
        contentAlignment = Alignment.TopCenter) {
        PixelTaskMark(mark, Modifier.padding(top = markTop))
    }
}

/** Pixel patterns are decoration. The caller owns state, Chinese descriptions and allowed actions. */
@Composable
private fun PixelTaskMark(mark: TaskRowMark, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val ink = if (mark == TaskRowMark.CHECK) colors.onPrimary else colors.onSurfaceVariant
    val border = if (mark == TaskRowMark.EMPTY) LocalMmExtendedColors.current.labelTertiary else colors.onSurfaceVariant
    Canvas(modifier.size(MomentMarkTokens.CheckboxSize).testTag("task-mark-${mark.name}").clearAndSetSemantics {}) {
        val stroke = MomentMarkTokens.CheckboxStrokeWidth.toPx()
        if (mark == TaskRowMark.CHECK) drawRect(colors.primary) else drawRect(colors.surface)
        drawRect(if (mark == TaskRowMark.CHECK) colors.outline else border,
            topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke))
        val pixels = when (mark) {
            TaskRowMark.EMPTY -> emptyList()
            TaskRowMark.CHECK -> listOf("000001", "000011", "100110", "111100", "011000")
            TaskRowMark.CROSS -> listOf("10001", "01010", "00100", "01010", "10001")
            TaskRowMark.ARROW -> listOf("00100", "00010", "11111", "00010", "00100")
            TaskRowMark.SKIP -> listOf("00000", "00000", "11111", "00000", "00000")
            TaskRowMark.BUSY -> listOf("11111", "01010", "00100", "01010", "11111")
            TaskRowMark.CLOCK -> listOf("01110", "10101", "10111", "10001", "01110")
        }
        val unit = size.width * .10f
        val width = (pixels.firstOrNull()?.length ?: 0) * unit
        pixels.forEachIndexed { y, line -> line.forEachIndexed { x, value ->
            if (value == '1') drawRect(ink, Offset((size.width - width) / 2 + x * unit,
                (size.height - pixels.size * unit) / 2 + y * unit), Size(unit, unit))
        } }
    }
}
