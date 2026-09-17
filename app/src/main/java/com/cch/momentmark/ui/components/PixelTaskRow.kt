package com.cch.momentmark.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmNoFontPadding
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 日子簿任务行（design.html 第 ⑤ 屏）：
 * - 左侧 15dp 复选框；
 * - 中间标题（未完成=墨色/已完成=次要色+危险删除线）；
 * - 右侧 mono 状态（CLEAR / 截止时间 / 全天）。
 *
 * 设计稿列表内没有显式「编辑/放弃」按钮；本实现保留回调：
 * - 点击标题行 → onEdit
 * - 长按整行 → onSeal（二次确认仍由 DaybookScreen 弹窗处理）
 * - 点击复选框 → onToggleCompleted
 */
data class TaskRowPresentation(
    val title: String,
    val isCompleted: Boolean,
    val statusLabel: String,
    val statusDescription: String,
    val isAllDay: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelTaskRow(
    presentation: TaskRowPresentation,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onSeal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = presentation.title
    val separatorColor = MaterialTheme.colorScheme.surfaceVariant
    val titleColor = if (presentation.isCompleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    // 行盒：固定 36dp = 8(pad) + 18(内容) + 8(pad) + 2(border-bottom)，与 design `.px` 面板内的
    // 任务行 `padding:8px 0; border-bottom:2px dashed` 一致。
    // 虚线必须画在**行盒底边**：`drawBehind` 必须排在 `padding` 之前，否则它看到的是内缩 8dp 的
    // 内容盒，虚线会上移 8dp、紧贴标题（⑤ 屏实测偏移 6dp）。
    Row(
        modifier
            .fillMaxWidth()
            .height(MomentMarkTokens.DaybookTaskRowHeight)
            .drawBehind {
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
            .padding(vertical = MomentMarkTokens.DaybookTaskRowPaddingVertical)
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onSeal,
                role = Role.Button,
            )
            .semantics {
                contentDescription = "${title}，${presentation.statusDescription}，点击编辑，长按放弃"
            },
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelTaskCheckbox(presentation.isCompleted, title, onToggleCompleted)
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = MomentMarkTokens.DaybookTaskTitleLineHeight,
                platformStyle = MmNoFontPadding,
            ),
            color = titleColor,
            textDecoration = if (presentation.isCompleted) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = presentation.statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = MomentMarkTokens.DaybookTaskStatusFontSize,
                lineHeight = MomentMarkTokens.DaybookTaskStatusLineHeight,
                letterSpacing = MomentMarkTokens.MonoLetterSpacingNone,
                platformStyle = MmNoFontPadding,
            ),
            color = when {
                presentation.isCompleted -> MaterialTheme.colorScheme.primary
                presentation.isAllDay -> LocalMmExtendedColors.current.goldInk
                else -> LocalMmExtendedColors.current.labelTertiary
            },
            modifier = Modifier.semantics { contentDescription = presentation.statusDescription },
        )
    }
}

@Composable
private fun PixelTaskCheckbox(checked: Boolean, title: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val checkboxBorder = LocalMmExtendedColors.current.labelTertiary
    Box(
        Modifier
            .size(MomentMarkTokens.CheckboxSize)
            .semantics {
                contentDescription = "$title ${if (checked) "已完成" else "未完成"}；复选框"
                role = Role.Checkbox
            }
            .background(if (checked) primary else MaterialTheme.colorScheme.surface)
            .drawBehind {
                // Stroke 以路径为中心描边：内缩半个线宽，边框才会像 HTML `border:2.5px` 一样
                // 占满盒内 0..2.5dp（否则只在内侧留下 1.25dp、绿底变成 12.5dp 而不是设计的 10px）。
                val stroke = MomentMarkTokens.CheckboxStrokeWidth.toPx()
                drawRect(
                    color = if (checked) outline else checkboxBorder,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
            }
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                "✓",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = MomentMarkTokens.CheckboxCheckMarkFontSize,
                    lineHeight = MomentMarkTokens.CheckboxCheckMarkFontSize,
                    // design `::after{font-weight:800}`：兜底字体在 Bold 下勾号明显偏细（实测白像素 63 vs 79）。
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = MomentMarkTokens.MonoLetterSpacingNone,
                    platformStyle = MmNoFontPadding,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.offset(y = MomentMarkTokens.CheckboxCheckMarkOffsetY),
            )
        }
    }
}
