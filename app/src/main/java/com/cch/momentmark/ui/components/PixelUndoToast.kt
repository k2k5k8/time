package com.cch.momentmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.ui.theme.MomentMarkTokens

@Composable
fun PixelUndoToast(title: String, onUndo: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    PixelPanel(modifier = modifier.fillMaxWidth(), backgroundColor = MaterialTheme.colorScheme.inverseSurface, borderColor = MaterialTheme.colorScheme.outline, shadowColor = MaterialTheme.colorScheme.outline) {
        Row(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⚔「$title」已封印", color = MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard)) {
                Text("↩ 撤销", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.semantics { contentDescription = "撤销封印 $title"; role = Role.Button }.clickable(role = Role.Button, onClick = onUndo))
                Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { contentDescription = "关闭撤销提示"; role = Role.Button }.clickable(role = Role.Button, onClick = onDismiss))
            }
        }
    }
}
