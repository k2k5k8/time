package com.cch.momentmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.MmSemanticColor

/** DESIGN_SYSTEM §4 规定的像素按钮语义色。 */
enum class PixelButtonVariant {
    Primary,
    Secondary,
    Reward,
    Danger,
    Ghost,
}

/** Regular keeps existing full-width geometry; Compact and Icon opt into a smaller visual body. */
enum class PixelButtonLayout { Regular, Compact, Icon }

/**
 * J 像素按钮：直角墨线、3dp 硬影、按压位移与至少 48dp 的可点击区域。
 *
 * 组件只呈现调用方给出的标签和状态；业务校验、保存及加载生命周期仍由上层持有。
 * [loading] 会阻止重复点击，并以可读的处理中状态替换标签。
 * [selected] 持久沉入，颜色由 variant 或 [selectedColors] 提供，禁用/处理中仍保留。
 * [layout] 默认保留旧全宽主体；Compact/Icon 的视觉盒与至少 48dp 触摸盒分离。
 * 图标规格仍需中文 [label]（或 [contentDescription]）；不对外扩大透明点击区。
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
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = MomentMarkTokens.SpaceInner, vertical = MomentMarkTokens.SpaceCompact),
    icon: PixelButtonIcon? = null,
    selected: Boolean? = null,
    layout: PixelButtonLayout = PixelButtonLayout.Regular,
    selectedColors: MmSemanticColor? = null,
) {
    require(layout != PixelButtonLayout.Icon || icon != null) { "Icon buttons require a pixel icon" }
    val regular = layout == PixelButtonLayout.Regular
    val density = LocalDensity.current
    val deviceDensity = LocalView.current.resources.displayMetrics.density
    val touchTarget = with(density) { (MomentMarkTokens.ButtonTouchTargetMin.value * deviceDensity).toDp() }
        .coerceAtLeast(MomentMarkTokens.ButtonTouchTargetMin)
    val baseColors = pixelButtonColors(variant)
    val colors = if (selected == true && selectedColors != null) {
        PixelButtonColors(selectedColors.container, selectedColors.onContainer, baseColors.border)
    } else baseColors
    val interactionSource = remember { MutableInteractionSource() }
    val acceptsInput = enabled && !loading
    val pressed by interactionSource.collectIsPressedAsState()
    val pointerFeedback = rememberPixelPressFeedback(acceptsInput)
    val activePress = (pointerFeedback.pressed || pressed) && acceptsInput
    val restingShadow = MomentMarkTokens.PxShadowOffset
    // Immediate paired-colour inversion remains visible even with system animations disabled.
    val container = if (activePress) colors.content else colors.container
    val foreground = if (activePress) colors.container else colors.content

    // Keep the hit box stationary. Only the visual body moves, inside its allocated target.
    Box(
        modifier = modifier
            .sizeIn(minWidth = touchTarget, minHeight = touchTarget)
            .pixelPressFeedback(pointerFeedback, acceptsInput)
            .graphicsLayer {
                // Alpha's default offscreen layer clips the exterior hard shadow.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = if (enabled) 1f else MomentMarkTokens.AlphaDisabled
            }
            .semantics(mergeDescendants = true) {
                // Text already supplies the default label; avoid a second spoken copy.
                if (layout == PixelButtonLayout.Icon || icon == null || contentDescription != label) this.contentDescription = contentDescription
                selected?.let {
                    this.selected = it
                    stateDescription = if (it) "已选择" else "未选择"
                }
                if (loading) stateDescription = "处理中"
            }
            .clickable(
                enabled = acceptsInput,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = regular,
    ) {
        PixelPanel(
            backgroundColor = container,
            borderColor = colors.border,
            shadowOffset = restingShadow,
            sunken = selected == true || activePress,
            shadowColor = colors.border,
            propagateMinConstraints = true,
            fillWidth = regular,
        ) {
            Box(Modifier.then(if (regular) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minWidth = if (regular) touchTarget else MomentMarkTokens.CompactIconSize,
                    minHeight = if (regular) touchTarget else MomentMarkTokens.ButtonCompactMinHeight),
                contentAlignment = Alignment.Center) {
                if (layout == PixelButtonLayout.Icon) {
                    if (loading) Text("…", color = foreground, style = textStyle)
                    else PixelButtonGlyph(requireNotNull(icon), foreground)
                } else {
                    PixelButtonLabel(if (loading) "处理中…" else label, icon, foreground,
                        textStyle, if (regular) contentPadding else PaddingValues(
                            horizontal = MomentMarkTokens.ButtonCompactPaddingHorizontal,
                            vertical = MomentMarkTokens.ButtonCompactPaddingVertical),
                        fillWidth = regular)
                }
            }
        }
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
