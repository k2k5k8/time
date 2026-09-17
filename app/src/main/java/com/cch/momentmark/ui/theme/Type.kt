package com.cch.momentmark.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import com.cch.momentmark.R
import androidx.compose.ui.unit.sp

/**
 * 全局正文保持 Android SansSerif；HTML ①屏的近似字体只由首页显式引用，避免影响其他页面。
 */
val MmDisplayFont = FontFamily.SansSerif
val MmBodyFont = FontFamily.SansSerif
val MmHudFont = FontFamily.Monospace
val MmHomeDisplayFont = FontFamily(Font(R.font.lantinghei_sc_heavy, weight = FontWeight.ExtraBold))

/**
 * 关闭 Compose 默认的 `includeFontPadding`。
 *
 * `design.html` 里每个文字块的行盒高度 = 浏览器 `line-height:normal` 的实测值（已逐项登记为
 * `*LineHeight` token）；Compose 默认会在这个行盒外再补一份字体 ascent/descent 留白，
 * 使盒高比 token 多 1–2dp，逐块累积成整页下移（⑤ 屏实测：月标题面板 +1dp、状态栏 +2dp）。
 * 因此凡是以「HTML 行盒 = lineHeight token」为前提复刻的文字，都应套用本样式。
 */
val MmNoFontPadding = PlatformTextStyle(includeFontPadding = false)

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
