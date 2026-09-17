package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

data class MomentSummaryPresentation(val title: String, val statusLabel: String, val dateLabel: String, val isAchievement: Boolean)

@Composable
fun MomentSummaryPanel(presentation: MomentSummaryPresentation, modifier: Modifier = Modifier) {
    val extended = LocalMmExtendedColors.current
    val accent = if (presentation.isAchievement) extended.goldInk else MaterialTheme.colorScheme.secondary
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner), verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            if (presentation.isAchievement) Text("🏆", style = MaterialTheme.typography.displaySmall, color = extended.goldInk)
            Text(if (presentation.isAchievement) "🏆 ACHIEVEMENT · 成就珍藏中" else "⧖ QUEST · 倒数前往", style = MaterialTheme.typography.labelSmall, color = accent)
            Text(presentation.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(presentation.statusLabel, style = MaterialTheme.typography.displayMedium, color = accent)
            Text(presentation.dateLabel, style = MaterialTheme.typography.labelSmall, color = extended.labelTertiary)
        }
    }
    if (presentation.isAchievement) AchievementFrame(modifier = modifier.fillMaxWidth()) { content() }
    else PixelPanel(modifier = modifier.fillMaxWidth(), shadowOffset = MomentMarkTokens.PxShadowOffsetLarge) { content() }
}
