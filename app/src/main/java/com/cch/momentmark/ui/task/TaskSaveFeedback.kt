package com.cch.momentmark.ui.task

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** Small decorative stamp; storage and navigation remain with the caller. */
@Composable
internal fun TaskSaveFeedback(editing: Boolean) {
    val scale = remember { Animatable(if (editing) 1f else 1.12f) }
    LaunchedEffect(editing) { if (!editing) scale.animateTo(1f, tween(MomentMarkTokens.TaskStampSettleMs)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PixelPanel(Modifier.width(IntrinsicSize.Max).padding(MomentMarkTokens.SpaceCard).graphicsLayer {
            scaleX = scale.value; scaleY = scale.value
        }.testTag("task-save-feedback").semantics { liveRegion = LiveRegionMode.Polite },
            backgroundColor = MaterialTheme.colorScheme.primary) {
            Text(if (editing) "✓ 已保存" else "✓ 任务已接取",
                Modifier.padding(MomentMarkTokens.SpaceInner),
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
