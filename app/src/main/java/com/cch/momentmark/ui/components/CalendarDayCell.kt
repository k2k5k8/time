package com.cch.momentmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * - 有任务且非今天：底部 8sp 内容标记 🎁。
 */
@Composable
fun CalendarDayCell(
    date: LocalDate,
    inVisibleMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        if (hasTasks) append("，有任务")
    }

    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics {
                contentDescription = semanticsText
                role = Role.Button
            }
            .then(if (inVisibleMonth) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = MomentMarkTokens.PxThinBorderWidth,
        shadowOffset = MomentMarkTokens.DaybookTodayShadowOffset,
        shadowColor = shadowColor,
        fillContent = true,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = MomentMarkTokens.DaybookCellFontSize,
                    lineHeight = MomentMarkTokens.DaybookCellLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = contentColor,
            )
            if (hasTasks && !today) {
                Text(
                    text = "🎁",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = MomentMarkTokens.DaybookCellMarkerFontSize,
                        lineHeight = MomentMarkTokens.DaybookCellMarkerLineHeight,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = contentColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // design `.fx{bottom:3px}` 相对**内边距盒**（边框之内）定位；
                        // Compose 的 border 画在盒内、不占布局 → 取 3 + 2 边框补偿。
                        .padding(bottom = MomentMarkTokens.DaybookCellMarkerBottomInset),
                )
            }
        }
    }
}
