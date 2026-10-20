package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** 语义化像素面板层级；只有内嵌容器可以取消硬影。 */
enum class PixelPanelVariant {
    Raised,
    Emphasized,
    Flat,
}

/**
 * 像素面板：直角面 + 3dp 墨线描边 + 右下硬偏移影（DESIGN_SYSTEM §3.3）。
 * 硬影与 HTML `.px/.px4 { box-shadow: Npx Npx 0 var(--panel-line) }` 等价：
 * **不占布局空间**——影子只画在面板右/下之外，卡片外框尺寸 = 分配到的尺寸。
 * （早期实现为影子预留 end/bottom padding，会让每张卡比设计稿窄/矮 3–4dp。）
 * 需要保持几何尺寸但不显示影子时使用 Flat 变体或传入透明 shadowColor。
 *
 * @param fillContent 让描边盒撑满外层尺寸。默认 `false`（描边高 = 内容高），
 *   适合普通 wrap-content 面板。当外层由 `Modifier.size(...)` 固定了高度、
 *   而内容比它矮时（如 `.back` 返回方块：外层 25dp、内部字形仅 7dp），
 *   必须传 `true`，否则 HTML 的 `border` 会包住整块 25dp、而 Compose 只包住 7dp，
 *   同时硬影仍按 25dp 计算 —— 会在方块下方留下一段与边框错位的"悬空阴影"。
 * @param fillWidth 默认撑满宽度；紧凑控件传 false，主体、描边和硬影按内容同盒测量。
 * @param propagateMinConstraints 将按钮最小尺寸传给主体与内容，避免仅硬影撑高。
 *   默认关闭，普通面板仍按内容增高，不在滚动容器中强制 fillMaxSize。
 * @param sunken 持久沉入；null 不启用选择绘制层，false/true 保持同一层并切换抬起/沉入。
 *   与暂态按压共用原硬影位置，不叠加位移或改变外层命中盒。
 * @param pressInteractionSource 与外层 clickable/combinedClickable 共用；按住时主体移到阴影位置，外层点击盒不移动。
 */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = MomentMarkTokens.PxBorderWidth,
    shadowOffset: Dp = MomentMarkTokens.PxShadowOffset,
    shadowColor: Color = borderColor,
    variant: PixelPanelVariant = PixelPanelVariant.Raised,
    fillContent: Boolean = false,
    propagateMinConstraints: Boolean = false,
    fillWidth: Boolean = true,
    pressInteractionSource: MutableInteractionSource? = null,
    pressEnabled: Boolean = true,
    sunken: Boolean? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolvedShadowOffset = when (variant) {
        PixelPanelVariant.Raised -> shadowOffset
        PixelPanelVariant.Emphasized -> MomentMarkTokens.PxShadowOffsetLarge
        PixelPanelVariant.Flat -> Dp.Hairline
    }
    val resolvedShadowColor = if (variant == PixelPanelVariant.Flat) Color.Transparent else shadowColor
    val pointerFeedback = if (pressInteractionSource != null) rememberPixelPressFeedback(pressEnabled) else null
    val pressed = pressEnabled && (pressInteractionSource?.collectIsPressedAsState()?.value == true || pointerFeedback?.pressed == true)
    val depressed = sunken == true || pressed
    val pressShift = if (depressed && resolvedShadowColor != Color.Transparent) resolvedShadowOffset else Dp.Hairline
    val visibleShadowOffset = if (depressed) Dp.Hairline else resolvedShadowOffset
    Box(
        modifier
            .then(if (pointerFeedback != null) Modifier.pixelPressFeedback(pointerFeedback, pressEnabled) else Modifier)
            .drawBehind {
                if (visibleShadowOffset > Dp.Hairline && resolvedShadowColor != Color.Transparent) {
                    val offsetPx = visibleShadowOffset.toPx()
                    // HTML box-shadow 是整块面板平移 (N,N) 后的形状；面板之外露出
                    // 右侧竖带（含右下角）+ 底部横带。右带必须延伸到 height+offset，
                    // 否则右下角 offset×offset 的区域两条带都不覆盖，出现缺角。
                    drawRect(
                        color = resolvedShadowColor,
                        topLeft = Offset(size.width, offsetPx),
                        size = Size(offsetPx, size.height),
                    )
                    drawRect(
                        color = resolvedShadowColor,
                        topLeft = Offset(offsetPx, size.height),
                        size = Size((size.width - offsetPx).coerceAtLeast(0f), offsetPx),
                    )
                }
            },
        // Buttons share their caller's minimum body size with the shadow container.
        // Keep ordinary scrolling panels wrap-content by default.
        propagateMinConstraints = propagateMinConstraints,
    ) {
        Box(
            Modifier
                .then(if (pressInteractionSource != null || sunken != null) Modifier.graphicsLayer {
                    translationX = pressShift.toPx()
                    translationY = pressShift.toPx()
                } else Modifier)
                .then(when {
                    fillContent -> Modifier.fillMaxSize()
                    fillWidth -> Modifier.fillMaxWidth()
                    else -> Modifier
                })
                .background(backgroundColor)
                .border(borderWidth, borderColor),
            propagateMinConstraints = propagateMinConstraints,
        ) {
            content()
        }
    }
}
