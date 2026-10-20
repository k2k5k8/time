package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmNoFontPadding
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 日子簿「＋ 接受新任务」入口行（design.html 第 ⑤ 屏最后一行）。
 *
 * - 左侧 15dp 虚线框（2.5dp 描边，颜色 primary）。
 * - 中间绿色粗体文案。
 * - 右侧 mono 弱注释 NEW QUEST。
 */
@Composable
fun NewQuestEntry(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val green = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "接受新任务（待办）"
                role = Role.Button
            }
            .pixelClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(
                top = MomentMarkTokens.DaybookNewTaskRowPaddingTop,
                bottom = MomentMarkTokens.DaybookNewTaskRowPaddingBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(MomentMarkTokens.CheckboxSize).drawBehind {
                // 与复选框同理：Stroke 居中描边，需内缩半个线宽才能占满盒内边框宽度。
                val stroke = MomentMarkTokens.CheckboxStrokeWidth.toPx()
                drawRect(
                    color = green,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                MomentMarkTokens.SpaceUnit.toPx(),
                                (MomentMarkTokens.SpaceUnit / 2).toPx(),
                            ),
                        ),
                    ),
                )
            },
        )
        Text(
            "＋ 接受新任务（待办）",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = MomentMarkTokens.DaybookNewTaskTitleLineHeight,
                platformStyle = MmNoFontPadding,
            ),
            color = green,
            modifier = Modifier.weight(1f),
        )
        Text(
            "NEW QUEST",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = MomentMarkTokens.DaybookTaskStatusFontSize,
                lineHeight = MomentMarkTokens.DaybookTaskStatusLineHeight,
                letterSpacing = MomentMarkTokens.MonoLetterSpacingNone,
                platformStyle = MmNoFontPadding,
            ),
            color = LocalMmExtendedColors.current.labelTertiary,
        )
    }
}
