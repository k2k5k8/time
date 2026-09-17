package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

data class MilestonePresentation(val label: String, val state: String, val detail: String, val progressFraction: Float?, val isNext: Boolean)

@Composable
fun MilestonePanel(items: List<MilestonePresentation>, modifier: Modifier = Modifier) {
    PixelPanel(modifier = modifier.fillMaxWidth(), shadowColor = MaterialTheme.colorScheme.outline) {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner), verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            Text("▸ 里程碑 MILESTONES", style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
            if (items.isEmpty()) Text("尚未生成里程碑。", style = MaterialTheme.typography.bodySmall, color = LocalMmExtendedColors.current.labelTertiary)
            else items.forEach { MilestoneRow(it) }
        }
    }
}

@Composable
private fun MilestoneRow(item: MilestonePresentation) {
    val color = when (item.state) { "已达成" -> MaterialTheme.colorScheme.primary; "下一项" -> LocalMmExtendedColors.current.goldInk; else -> LocalMmExtendedColors.current.labelTertiary }
    Column(Modifier.fillMaxWidth().padding(vertical = MomentMarkTokens.SpaceUnit), verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact)) {
            Text(if (item.state == "已达成") "☑" else if (item.isNext) "◈" else "□", style = MaterialTheme.typography.labelMedium, color = color)
            Text(item.label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(item.detail, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.weight(1f))
        }
        if (item.isNext) PixelHpBar(fraction = item.progressFraction ?: 0f, modifier = Modifier.fillMaxWidth(), fillColor = LocalMmExtendedColors.current.goldInk)
    }
}
