package com.cch.momentmark.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * J 最终稿 `.ach`：墨线外框和硬影之内，再留出 3dp 的金色内框。
 * 这是成就的视觉语义，不承载或伪造 P1 里程碑数据。
 */
@Composable
fun AchievementFrame(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    PixelPanel(
        modifier = modifier,
        backgroundColor = backgroundColor,
        variant = PixelPanelVariant.Emphasized,
    ) {
        Box(Modifier.fillMaxWidth()) {
            content()
            Box(
                Modifier
                    .matchParentSize()
                    .padding(MomentMarkTokens.AchievementInset)
                    .border(MomentMarkTokens.PxThinBorderWidth, LocalMmExtendedColors.current.goldInk),
            )
        }
    }
}
