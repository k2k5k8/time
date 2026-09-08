package com.cch.momentmark.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** DESIGN_SYSTEM §4 规定的像素按钮语义色。 */
enum class PixelButtonVariant {
    Primary,
    Secondary,
    Reward,
    Danger,
    Ghost,
}

/**
 * J 像素按钮：直角墨线、3dp 硬影、按压位移与至少 44dp 的可点击区域。
 *
 * 组件只呈现调用方给出的标签和状态；业务校验、保存及加载生命周期仍由上层持有。
 * [loading] 会阻止重复点击，并以可读的处理中状态替换标签。
 */
@Composable
fun PixelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PixelButtonVariant = PixelButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String = label,
) {
    val colors = pixelButtonColors(variant)
    val interactionSource = remember { MutableInteractionSource() }
    val acceptsInput = enabled && !loading
    val pressed by interactionSource.collectIsPressedAsState()
    val restingShadow = MomentMarkTokens.PxShadowOffset
    val pressedShadow = restingShadow - MomentMarkTokens.PressOffset
    val shadowOffset by animateDpAsState(
        targetValue = if (pressed && acceptsInput) pressedShadow else restingShadow,
        animationSpec = tween(MomentMarkTokens.MotionPressMs),
        label = "pixel-button-shadow",
    )
    val pressShift = restingShadow - shadowOffset

    PixelPanel(
        modifier = modifier
            .defaultMinSize(minHeight = MomentMarkTokens.TouchTargetMin)
            .graphicsLayer {
                translationX = pressShift.toPx()
                translationY = pressShift.toPx()
                alpha = if (enabled) 1f else MomentMarkTokens.AlphaDisabled
            }
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                if (loading) stateDescription = "处理中"
            }
            .clickable(
                enabled = acceptsInput,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        backgroundColor = colors.container,
        borderColor = colors.border,
        shadowOffset = shadowOffset,
        shadowColor = colors.border,
    ) {
        Text(
            text = if (loading) "处理中…" else label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.SpaceInner,
                    vertical = MomentMarkTokens.SpaceCompact,
                ),
        )
    }
}

private data class PixelButtonColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun pixelButtonColors(variant: PixelButtonVariant): PixelButtonColors {
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        PixelButtonVariant.Primary -> PixelButtonColors(scheme.primary, scheme.onPrimary, scheme.outline)
        PixelButtonVariant.Secondary -> PixelButtonColors(scheme.secondary, scheme.onSecondary, scheme.outline)
        PixelButtonVariant.Reward -> PixelButtonColors(scheme.tertiary, scheme.onTertiary, scheme.outline)
        PixelButtonVariant.Danger -> PixelButtonColors(scheme.error, scheme.onError, scheme.outline)
        PixelButtonVariant.Ghost -> PixelButtonColors(scheme.surface, scheme.onSurface, scheme.outline)
    }
}
