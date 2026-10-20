package com.cch.momentmark.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch

/** Local, square press tint. Foundation owns cancellation, scroll arbitration and activation. */
private data class PixelPressIndication(val tint: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node(), DrawModifierNode {
            private var pressed by mutableStateOf(false)
            override fun onAttach() {
                coroutineScope.launch {
                    val presses = mutableSetOf<PressInteraction.Press>()
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> presses.add(interaction)
                            is PressInteraction.Release -> presses.remove(interaction.press)
                            is PressInteraction.Cancel -> presses.remove(interaction.press)
                        }
                        pressed = presses.isNotEmpty()
                    }
                }
            }
            override fun onDetach() { pressed = false }
            override fun ContentDrawScope.draw() {
                drawContent()
                // Draw over opaque panel fills as well as plain text, within this hit box only.
                if (pressed) drawRect(tint)
            }
        }
}

/** Does not change layout or persist state; disabled controls emit no press interaction. */
fun Modifier.pixelClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(interactionSource = interactionSource,
        indication = PixelPressIndication(MaterialTheme.colorScheme.secondary.copy(alpha = .32f)),
        enabled = enabled, role = role, onClickLabel = onClickLabel, onClick = onClick)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pixelCombinedClickable(
    enabled: Boolean = true,
    onClickLabel: String,
    onLongClickLabel: String,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier = composed {
    combinedClickable(interactionSource = null,
        indication = PixelPressIndication(MaterialTheme.colorScheme.secondary.copy(alpha = .32f)),
        enabled = enabled, role = Role.Button, onClickLabel = onClickLabel,
        onLongClickLabel = onLongClickLabel, onLongClick = onLongClick, onClick = onClick)
}
