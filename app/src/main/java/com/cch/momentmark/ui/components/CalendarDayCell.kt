package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cch.momentmark.domain.model.CalendarDaySummary
import com.cch.momentmark.domain.model.TaskPlanPeriod
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate

/**
 * 日子簿日期格：与 design.html 第 ⑤ 屏 `.dg` 对齐。
 *
 * - 普通日：surface 面 + 2dp 墨线，13sp mono 数字。
 * - 其他月份（dim）：surfaceVariant 底 + labelTertiary 字。
 * - 今天：tertiary 填充 + 2dp 40% 墨影，字色 onTertiary。
 * - 选中非今天：surface 面 + 2dp tertiary 边框（设计稿 today 即为选中，PRD AC-25 要求区分）。
 * - 日期格内顶部前景直接绘制固定四时段实心方点，无底板或描边，不参与尺寸测量。
 */
@Composable
fun CalendarDayCell(
    date: LocalDate,
    inVisibleMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    summary: CalendarDaySummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val outline = scheme.outline

    val backgroundColor = when {
        today -> scheme.tertiary
        !inVisibleMonth -> scheme.surfaceVariant
        else -> scheme.surface
    }
    val contentColor = when {
        today -> scheme.onTertiary
        !inVisibleMonth -> LocalMmExtendedColors.current.labelTertiary
        else -> scheme.onSurface
    }
    val borderColor = when {
        today || !selected -> outline
        else -> scheme.tertiary
    }
    val shadowColor = if (today) outline.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent

    val semanticsText = buildString {
        append(date)
        if (selected) append("，已选择")
        if (today) append("，今天")
        append(summary?.description() ?: "，事项加载中")
    }

    val taskColors = LocalMmExtendedColors.current.task
    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            // One date node owns both its summary and activation; dots and digits add no extra focus.
            .then(if (inVisibleMonth) Modifier.pixelClickable(enabled = enabled, role = Role.Button, interactionSource = pressInteraction, onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = semanticsText
                role = Role.Button
                if (inVisibleMonth && enabled) this.onClick(label = "选择日期") { onClick(); true }
                else if (inVisibleMonth) disabled()
            },
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = MomentMarkTokens.PxThinBorderWidth,
        shadowOffset = MomentMarkTokens.DaybookTodayShadowOffset,
        shadowColor = shadowColor,
        pressInteractionSource = pressInteraction,
        pressEnabled = enabled && inVisibleMonth,
        fillContent = true,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).drawWithContent {
                drawContent()
                val periods = summary?.periods.orEmpty()
                val dot = MomentMarkTokens.DaybookDotSize.toPx()
                val slots = TaskPlanPeriod.entries
                val available = (size.width - 2 * MomentMarkTokens.PxThinBorderWidth.toPx()).coerceAtLeast(dot * slots.size)
                val gap = minOf(MomentMarkTokens.DaybookDotGap.toPx(), ((available - dot * slots.size) / (slots.size - 1)).coerceAtLeast(0f))
                val width = dot * slots.size + gap * (slots.size - 1)
                val start = (size.width - width) / 2
                val top = MomentMarkTokens.DaybookCellMarkerTopInset.toPx()
                slots.forEachIndexed { index, period ->
                    if (period in periods) drawRect(taskColors.calendarMarker(period, today),
                        Offset(start + index * (dot + gap), top), Size(dot, dot))
                }
                if (summary?.historyOnly == true) drawRect(if (today) scheme.onTertiary else scheme.onSurfaceVariant,
                    Offset((size.width - dot) / 2, top), Size(dot, dot))
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                modifier = Modifier.padding(top = MomentMarkTokens.DaybookDotSize * 2),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = MomentMarkTokens.DaybookCellFontSize,
                    lineHeight = MomentMarkTokens.DaybookCellLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = contentColor,
            )

        }
    }
}
