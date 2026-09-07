package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cch.momentmark.domain.model.HomeMomentProjector
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** 真实时间派生的 HP 血条；边框不被填充色覆盖。 */
@Composable
fun PixelHpBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    fillColor: androidx.compose.ui.graphics.Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val resolvedFillColor = fillColor ?: when {
        fraction > 0.6f -> scheme.primary
        fraction >= HomeMomentProjector.LIMITED_FRACTION -> LocalMmExtendedColors.current.amber
        else -> scheme.error
    }
    val safeFraction = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(MomentMarkTokens.HpBarHeight)
            .background(scheme.surfaceVariant)
            .clipToBounds()
            .drawBehind {
                val inset = MomentMarkTokens.PxThinBorderWidth.toPx()
                val fillWidth = ((size.width - inset * 2) * safeFraction).coerceAtLeast(0f)
                    drawRect(
                    color = resolvedFillColor,
                    topLeft = Offset(inset, inset),
                    size = Size(fillWidth, (size.height - inset * 2).coerceAtLeast(0f)),
                )
            }
            .border(MomentMarkTokens.PxThinBorderWidth, scheme.outline),
    )
}
