package com.cch.momentmark.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 形状系统：像素冒险全局直角（0dp 圆角）。
 * 层级靠 3dp 墨线描边 + 硬偏移阴影表达，见 MomentMarkTokens。
 */
val MmShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)
