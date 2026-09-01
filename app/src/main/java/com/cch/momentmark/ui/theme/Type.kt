package com.cch.momentmark.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 与网页基准一致的角色分工：标题/正文使用系统中文 sans，HUD 固定等宽。
 * 仓库内的第三方中文字体尚未完成再发布许可核验，因此不在发布 UI 中引用它们。
 */
val MmDisplayFont = FontFamily.SansSerif
val MmBodyFont = FontFamily.SansSerif
val MmHudFont = FontFamily.Monospace

/**
 * 字阶：像素冒险用超粗标题 + mono HUD 标签营造游戏感。
 * 数字永远是主角（display 系全部 extrabold）。
 * 规格见 DESIGN_SYSTEM.md §字号。
 */
val MmTypography = Typography(
    // 倒计时主数字 / 主线横幅
    displayLarge = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1).sp,
    ),
    // 详情页大数字
    displayMedium = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    // 任务卡数字
    displaySmall = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
    ),
    // 区块大标题
    headlineLarge = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    // 页面标题（队伍编成 / 装备库…）
    headlineSmall = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // 任务名称
    titleMedium = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // 正文（像素风正文略重）
    bodyLarge = TextStyle(
        fontFamily = MmBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MmBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MmBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // 按钮文字
    labelLarge = TextStyle(
        fontFamily = MmDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    // HUD 标签（mono）
    labelMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFamily = MmHudFont,
    ),
    // mono 小标签：HP%/DUE/GROUP
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp,
        fontFamily = MmHudFont,
    ),
)
