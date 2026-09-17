package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 设计画布等比缩放容器。
 *
 * design.html 的 `.phone` 内容区固定为 314×648 CSS px，`MomentMarkTokens` 里的每一个
 * 字号 / 间距 / 描边都是在这块画布上量出来的（例如首页双列小卡 140×114）。
 * 真实设备逻辑宽度不是 314dp 时，纯 dp 布局会出现「盒子跟着屏宽拉伸、内容却保持固定尺寸」
 * 的错配 —— 首页卡片因此从 1.261 的设计宽高比被拉成 1.535。
 *
 * 这里按 `设备可用宽度 ÷ [MomentMarkTokens.DesignCanvasWidth]` 覆盖 [LocalDensity]，
 * 让整棵树在同一个缩放坐标系里布局：卡片宽高比、内距与字号的比例在任何设备上都与设计稿一致。
 *
 * ## 行为约定
 * - **基准设备（可用宽度 = 314dp）缩放系数恰为 1.0f**，`Density` 数值不变、
 *   不会触发下游重组，因此原有 314dp 模拟器上的视觉回归结论完全不受影响。
 * - **只缩放 `density`，不缩放 `fontScale`**。`sp` 换算成像素用的是
 *   `sp × fontScale × density`，若两者都乘 `s`，字号会被放大 `s²` —— 字相对卡片
 *   会越缩越大（384dp 设备上实测标题比设计稿大 22%）。只乘 `density` 时
 *   字号与 dp 同比例缩放，卡内文字占比才和设计稿一致。
 * - 不设上下界：更宽的设备等比放大（384dp 屏 → 1.223×），更窄的设备等比缩小。
 *   平板 / 横屏会得到很大的字号，这是"复刻固定画布"的必然结果；若将来要支持，
 *   应在此处增加最大宽度约束并改为居中留白，而不是改 token。
 *
 * 用法：在主题之外包住整套界面（含底部导航），保证切换页签时缩放基准不跳变。
 */
@Composable
fun PixelCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val baseDensity = LocalDensity.current
        // maxWidth 由外层（设备）密度解释，拿到的是真实可用宽度，与内部缩放互不影响。
        val availableWidth = maxWidth.value
        val scale = if (availableWidth > 0f) {
            availableWidth / MomentMarkTokens.DesignCanvasWidth.value
        } else {
            1f
        }
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = baseDensity.density * scale,
                fontScale = baseDensity.fontScale,
            ),
            content = content,
        )
    }
}
