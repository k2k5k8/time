package com.cch.momentmark.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.MomentMarkTokens
import kotlinx.coroutines.delay

/**
 * 全 App 统一的像素方块光标（design.html 的 `▮`）。
 *
 * 由 [PixelTextInput]（时刻名称 / 时刻故事 / 任务名称 / 任务描述）与第 ④ 屏的日期时间白框
 * （`DueCell`）共用，避免出现「有的输入框是 4dp 方块、有的是系统细线」的不一致。
 */

/**
 * 方块光标的闪烁相位。
 *
 * design.html `@keyframes blink{50%{opacity:0}}` + `animation:blink 1s steps(1) infinite`：
 * 1s 周期、`steps(1)` 表示硬切换（无渐变），即亮 0.5s / 灭 0.5s。未聚焦时恒为隐藏
 * （HTML 里只有聚焦的输入框才带 `.cur`）。
 */
@Composable
fun rememberPixelCursorVisible(focused: Boolean): Boolean {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(focused) {
        if (!focused) {
            visible = false
            return@LaunchedEffect
        }
        val half = MomentMarkTokens.CursorBlinkPeriodMs.toLong() / 2
        while (true) {
            visible = true
            delay(half)
            visible = false
            delay(half)
        }
    }
    return visible
}

/**
 * 在 [BasicTextField] 的**父容器**上绘制方块光标。
 *
 * 绘制原点取父容器的内容区左上角，因此父容器若在文本外还有 padding，需要用
 * [offsetX] / [offsetY] 补回（`PixelTextInput` 的白框内距即走这两个参数）。
 *
 * ⚠ 使用本 Modifier 的输入框必须把 `cursorBrush` 设为透明，否则会与系统光标重叠成双光标。
 */
@Composable
fun Modifier.pixelBlockCursor(
    layoutResult: TextLayoutResult?,
    selectionEnd: Int,
    visible: Boolean,
    color: Color,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
): Modifier {
    val density = LocalDensity.current
    val cursorWidthPx = with(density) { MomentMarkTokens.FormInputCursorWidth.toPx() }
    val cursorHeightPx = with(density) { MomentMarkTokens.FormInputCursorHeight.toPx() }
    val offsetXPx = with(density) { offsetX.toPx() }
    val offsetYPx = with(density) { offsetY.toPx() }
    return this.drawWithContent {
        drawContent()
        if (!visible) return@drawWithContent
        val result = layoutResult ?: return@drawWithContent
        // 输入回调与文本布局会在相邻帧更新；以当前布局长度钳制偏移，
        // 避免新 selection 对旧 layout 取 cursorRect 越界。
        val cursor = result.getCursorRect(
            selectionEnd.coerceIn(0, result.layoutInput.text.length),
        )
        // 光标在行盒内垂直居中：设计稿的 `▮` 墨迹 8.3dp 远矮于 20dp 行高。
        val cursorTop = cursor.top + offsetYPx +
            ((cursor.height - cursorHeightPx).coerceAtLeast(0f) / 2f)
        drawRect(
            color = color,
            topLeft = Offset(cursor.left + offsetXPx, cursorTop),
            size = Size(cursorWidthPx, cursorHeightPx),
        )
    }
}
