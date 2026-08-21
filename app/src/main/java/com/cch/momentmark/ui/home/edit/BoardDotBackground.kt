package com.cch.momentmark.ui.home.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The faint dot lattice behind the board while editing (设计方案 §一).
 *
 * The horizontal pitch is derived from the card snap unit: two dots per grid
 * column, so the dots read as "these are the places cards can land". Vertical
 * pitch follows the horizontal one (square lattice), which keeps the density
 * self-adapting to any screen width. Purely decorative: it does not consume
 * pointer events and is not composed outside edit mode. The entrance/exit
 * cross-fade is owned by the surrounding AnimatedVisibility.
 */
@Composable
internal fun BoardDotBackground(
    columns: Int,
    horizontalPadding: Dp,
    cardSpacing: Dp,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val paddingPx = horizontalPadding.toPx()
        val spacingPx = cardSpacing.toPx()
        val usableWidth = size.width - 2 * paddingPx
        if (usableWidth <= 0f) return@Canvas
        val cellWidth = (usableWidth - spacingPx * (columns - 1)) / columns
        val pitch = cellWidth / 2f
        if (pitch <= 0f) return@Canvas

        val radius = 2.dp.toPx().coerceAtMost(pitch / 6f)
        val stepY = pitch
        // Center the vertical pattern so partial rows look intentional.
        var y = (size.height % stepY) / 2f
        val paint = dotColor.copy(alpha = 0.22f)
        while (y <= size.height) {
            var x = paddingPx
            while (x <= size.width - paddingPx + 0.5f) {
                drawCircle(
                    color = paint,
                    radius = radius,
                    center = Offset(x, y),
                )
                x += pitch
            }
            y += stepY
        }
    }
}

/**
 * The dashed placeholder (Ghost) that stays in the dragged card's slot while
 * the card floats with the finger (设计方案 §5.3). The slot itself keeps
 * updating as reorders are predicted, so the ghost always marks where the
 * card would land if released right now. [alpha] fades it out while the
 * released card settles onto the slot.
 */
@Composable
internal fun GhostCardSlot(
    alpha: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.01f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val corner = 18.dp.toPx()
        val cornerRadius = CornerRadius(corner, corner)
        drawRoundRect(
            color = color.copy(alpha = 0.10f * alpha),
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = color.copy(alpha = 0.45f * alpha),
            cornerRadius = cornerRadius,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            ),
        )
    }
}
