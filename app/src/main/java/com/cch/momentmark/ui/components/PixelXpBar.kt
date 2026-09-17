package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.ui.theme.MomentMarkTokens

@Composable
fun PixelXpBar(filledCells: Int, cellCount: Int = MomentMarkTokens.XpCellCount, modifier: Modifier = Modifier, contentDescription: String? = null) {
    require(cellCount > 0) { "cellCount must be positive" }
    val filled = filledCells.coerceIn(0, cellCount)
    // design `.xpbar .cell{border:2px solid var(--panel-line)}`：每格墨线细描边，填色与空格同框。
    Row(modifier.height(MomentMarkTokens.XpCellHeight).semantics { contentDescription?.let { this.contentDescription = it } }, horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.XpCellGap)) {
        repeat(cellCount) { index ->
            androidx.compose.foundation.layout.Box(
                Modifier
                    .weight(1f)
                    .height(MomentMarkTokens.XpCellHeight)
                    .background(if (index < filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(MomentMarkTokens.PxThinBorderWidth, MaterialTheme.colorScheme.outline),
            )
        }
    }
}
