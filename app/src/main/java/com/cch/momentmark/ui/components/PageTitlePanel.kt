package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * J 页题面板（proposal_J_final .pagetitle）：
 * panel 面 + 3dp 墨线 + 3dp 硬影，左标题、右 mono HUD 弱注释。
 */
@Composable
fun PageTitlePanel(
    title: String,
    hud: String,
    modifier: Modifier = Modifier,
) {
    PixelPanel(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.SpaceInner,
                    vertical = MomentMarkTokens.SpaceCard,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = hud,
                style = MaterialTheme.typography.labelSmall,
                color = LocalMmExtendedColors.current.labelTertiary,
            )
        }
    }
}
