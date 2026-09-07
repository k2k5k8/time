package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** 语义化像素面板层级；只有内嵌容器可以取消硬影。 */
enum class PixelPanelVariant {
    Raised,
    Emphasized,
    Flat,
}

/**
 * 像素面板：直角面 + 3dp 墨线描边 + 右下硬偏移影（DESIGN_SYSTEM §3.3）。
 * 影子是面板后方独立的偏移矩形，布局会为它保留右/下空间；这等效于 HTML
 * `.px/.px4 { box-shadow: Npx Npx 0 var(--panel-line) }`，不会被内容面层覆盖。
 * 需要保持几何尺寸但不显示影子时使用 Flat 变体或传入透明 shadowColor。
 */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = MomentMarkTokens.PxBorderWidth,
    shadowOffset: Dp = MomentMarkTokens.PxShadowOffset,
    shadowColor: Color = borderColor,
    variant: PixelPanelVariant = PixelPanelVariant.Raised,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolvedShadowOffset = when (variant) {
        PixelPanelVariant.Raised -> shadowOffset
        PixelPanelVariant.Emphasized -> MomentMarkTokens.PxShadowOffsetLarge
        PixelPanelVariant.Flat -> Dp.Hairline
    }
    val resolvedShadowColor = if (variant == PixelPanelVariant.Flat) Color.Transparent else shadowColor
    Box(
        modifier
            // drawBehind 必须位于 padding 之前，才能覆盖 HTML box-shadow 的额外区域。
            .drawBehind {
                if (resolvedShadowOffset > Dp.Hairline && resolvedShadowColor != Color.Transparent) {
                    val offsetPx = resolvedShadowOffset.toPx()
                    drawRect(
                        color = resolvedShadowColor,
                        topLeft = Offset(offsetPx, offsetPx),
                        size = Size(
                            width = (size.width - offsetPx).coerceAtLeast(0f),
                            height = (size.height - offsetPx).coerceAtLeast(0f),
                        ),
                    )
                }
            }
            .padding(end = resolvedShadowOffset, bottom = resolvedShadowOffset),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(borderWidth, borderColor),
        ) {
            content()
        }
    }
}
