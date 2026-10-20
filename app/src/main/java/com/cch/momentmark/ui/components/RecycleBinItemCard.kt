package com.cch.momentmark.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * J ⑩ 封印之地条目卡（design.html 第⑩屏 GRAVEYARD · 昼）。
 *
 * 结构：标题行（🔗 标题 + 类型徽章）→ 元数据行（封存于 X 天前 · 剩余 Y 天）→
 * 按钮行（✚ 复活 gold / 永久净化 ghost）。
 *
 * 卡片本身无硬影（Flat 变体，仅 3dp 墨线边框）；内部按钮带 .px 硬影。
 */
data class RecycleBinItemPresentation(
    val title: String,
    val typeLabel: String,
    val sealedDaysAgo: Long,
    val remainingDays: Long,
)

@Composable
fun RecycleBinItemCard(
    presentation: RecycleBinItemPresentation,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink3 = LocalMmExtendedColors.current.labelTertiary
    val restorePress = remember { MutableInteractionSource() }
    val deletePress = remember { MutableInteractionSource() }
    PixelPanel(
        modifier = modifier.fillMaxWidth(),
        variant = PixelPanelVariant.Flat,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = MomentMarkTokens.RecycleBinCardPaddingVertical,
                    horizontal = MomentMarkTokens.RecycleBinCardPaddingHorizontal,
                ),
        ) {
            // 标题行：🔗 标题 + 类型徽章
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🔗 ${presentation.title}",
                    fontSize = MomentMarkTokens.RecycleBinCardTitleFontSize,
                    lineHeight = MomentMarkTokens.RecycleBinCardTitleLineHeight,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink3,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // 徽章：mono 8sp/800，2dp 边框，padding 1dp 6dp
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .semantics { contentDescription = "${presentation.typeLabel} 已封印" },
                ) {
                    Text(
                        text = "${presentation.typeLabel} · 已封印",
                        fontSize = MomentMarkTokens.RecycleBinBadgeFontSize,
                        lineHeight = MomentMarkTokens.RecycleBinBadgeLineHeight,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = ink3,
                        modifier = Modifier
                            .border(BorderStroke(MomentMarkTokens.PxThinBorderWidth, ink3))
                            .padding(
                                vertical = MomentMarkTokens.RecycleBinBadgePaddingVertical,
                                horizontal = MomentMarkTokens.RecycleBinBadgePaddingHorizontal,
                            ),
                    )
                }
            }
            // 元数据行：mono 9sp/800，margin-top 4dp
            Text(
                text = "封存于 ${presentation.sealedDaysAgo} 天前 · 剩余 ${presentation.remainingDays} 天",
                fontSize = MomentMarkTokens.RecycleBinMetaFontSize,
                lineHeight = MomentMarkTokens.RecycleBinMetaLineHeight,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = ink3,
                modifier = Modifier.padding(top = MomentMarkTokens.RecycleBinMetaTopGap),
            )
            // 按钮行：gap 8dp，margin-top 9dp；复活=gold flex:1，永久净化=ghost wrap
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = MomentMarkTokens.RecycleBinButtonRowTopGap),
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.RecycleBinButtonRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 复活按钮：gold 背景，flex:1，带 3dp 硬影
                PixelPanel(
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "复活 ${presentation.title}"
                            role = Role.Button
                        }
                        .clickable(interactionSource = restorePress, indication = null, role = Role.Button, onClick = onRestore),
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    pressInteractionSource = restorePress,
                ) {
                    Text(
                        text = "✚ 复活",
                        fontSize = MomentMarkTokens.RecycleBinButtonFontSize,
                        lineHeight = MomentMarkTokens.RecycleBinButtonLineHeight,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = MomentMarkTokens.RecycleBinButtonPaddingVertical),
                    )
                }
                // 永久净化按钮：ghost（panel 底），ink-3 字，wrap content，带 3dp 硬影
                PixelPanel(
                    modifier = Modifier
                        .semantics {
                            contentDescription = "永久净化 ${presentation.title}"
                            role = Role.Button
                        }
                        .clickable(interactionSource = deletePress, indication = null, role = Role.Button, onClick = onPermanentlyDelete),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    fillWidth = false,
                    pressInteractionSource = deletePress,
                ) {
                    Text(
                        text = "永久净化",
                        fontSize = MomentMarkTokens.RecycleBinButtonFontSize,
                        lineHeight = MomentMarkTokens.RecycleBinButtonLineHeight,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = ink3,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(
                            vertical = MomentMarkTokens.RecycleBinButtonPaddingVertical,
                            horizontal = MomentMarkTokens.RecycleBinCardPaddingHorizontal,
                        ),
                    )
                }
            }
        }
    }
}
