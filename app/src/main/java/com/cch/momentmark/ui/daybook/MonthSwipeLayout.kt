package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import com.cch.momentmark.ui.theme.MomentMarkTokens
import kotlin.math.abs
import kotlin.math.roundToInt

/** Calendar and tasks share a moving boundary; a short month releases space progressively. */
@Composable
internal fun MonthSwipeLayout(
    progress: Float,
    modifier: Modifier = Modifier,
    calendar: @Composable (Int) -> Unit,
    tasks: @Composable (Int) -> Unit,
) {
    Column(modifier) {
        MonthSwipeStrip(progress, calendar)
        MonthSwipeStrip(progress, tasks)
    }
}

@Composable
private fun MonthSwipeStrip(progress: Float, content: @Composable (Int) -> Unit) {
    Layout(modifier = if (progress == 0f) Modifier else Modifier.clipToBounds(), content = {
        for (delta in -1..1) content(delta)
    }) { measurables, constraints ->
        val width = constraints.maxWidth
        val stride = width + MomentMarkTokens.DaybookMonthPageGap.roundToPx()
        val pages = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val neighbor = if (progress < 0) 2 else 0
        val fraction = abs(progress).coerceIn(0f, 1f)
        val height = (pages[1].height * (1f - fraction) + pages[neighbor].height * fraction).roundToInt()
        layout(width, constraints.constrainHeight(height)) {
            for (index in 0..2) {
                // Hidden pages stay measured, without drawing or exposing offscreen input.
                if (index == 1 || (fraction > 0 && index == neighbor)) {
                    pages[index].place(((index - 1 + progress) * stride).roundToInt(), 0)
                }
            }
        }
    }
}
