package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

@Composable
fun MomentStoryPanel(note: String, isAchievement: Boolean, modifier: Modifier = Modifier) {
    if (note.isBlank()) return
    PixelPanel(modifier = modifier.fillMaxWidth(), shadowColor = MaterialTheme.colorScheme.outline) {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
            Text(if (isAchievement) "▸ 成就故事 STORY" else "▸ 任务背景 LORE", style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = MomentMarkTokens.SpaceUnit * 2))
        }
    }
}
