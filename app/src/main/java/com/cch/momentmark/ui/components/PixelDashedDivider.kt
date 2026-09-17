package com.cch.momentmark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.MomentMarkTokens

enum class PixelDividerOrientation { Horizontal, Vertical }

@Composable
fun PixelDashedDivider(
    orientation: PixelDividerOrientation = PixelDividerOrientation.Horizontal,
    modifier: Modifier = Modifier,
) {
    val color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = if (orientation == PixelDividerOrientation.Horizontal) modifier.fillMaxWidth().height(2.dp) else modifier,
    ) {
        val effect = PathEffect.dashPathEffect(floatArrayOf(MomentMarkTokens.SpaceUnit.toPx(), MomentMarkTokens.SpaceUnit.toPx()))
        if (orientation == PixelDividerOrientation.Horizontal) drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = MomentMarkTokens.PxThinBorderWidth.toPx(), pathEffect = effect)
        else drawLine(color = color, start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height), strokeWidth = MomentMarkTokens.PxThinBorderWidth.toPx(), pathEffect = effect)
    }
}
