package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** A light floating confirmation; the app places it above navigation without resizing the page. */
@Composable
fun PixelUndoToast(title: String, onUndo: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val touchSize = with(LocalDensity.current) {
        (MomentMarkTokens.ButtonTouchTargetMin.value * LocalView.current.resources.displayMetrics.density).toDp()
    }.coerceAtLeast(MomentMarkTokens.ButtonTouchTargetMin)
    PixelPanel(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = scheme.surface,
        borderColor = scheme.outlineVariant,
        borderWidth = MomentMarkTokens.PxThinBorderWidth,
        shadowColor = scheme.outline.copy(alpha = .25f),
        shadowOffset = MomentMarkTokens.PxShadowOffsetSmall,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.SpaceCompact, vertical = MomentMarkTokens.SpaceUnit),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            Column(Modifier.weight(1f).padding(horizontal = MomentMarkTokens.SpaceUnit)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text("已封印", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                Text(title, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.sizeIn(minWidth = touchSize, minHeight = touchSize)
                .semantics { contentDescription = "撤销封印 $title" }
                .pixelClickable(role = Role.Button, onClick = onUndo)
                .padding(horizontal = MomentMarkTokens.SpaceUnit), contentAlignment = Alignment.Center) {
                Text("撤销", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            }
            Box(Modifier.sizeIn(minWidth = touchSize, minHeight = touchSize)
                .semantics { contentDescription = "关闭撤销提示" }
                .pixelClickable(role = Role.Button, onClick = onDismiss), contentAlignment = Alignment.Center) {
                PixelButtonGlyph(PixelButtonIcon.Close, scheme.onSurfaceVariant)
            }
        }
    }
}
