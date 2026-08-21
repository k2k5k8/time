package com.cch.momentmark.ui.daybook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

// ── Color tokens ──────────────────────────────────────────────

/** 品牌强调色 — 低饱和暖橙 */
val DaybookPrimary = Color(0xFFE98B5B)

/** 页面背景 — 温暖米白 */
val DaybookBackground = Color(0xFFFAF5EF)

/** 卡片表面 — 纯白偏暖 */
val DaybookSurface = Color(0xFFFFFFFF)

/** 半透明暖白表面 */
val DaybookSurfaceTranslucent = Color(0xFFFFFCF9).copy(alpha = 0.94f)

/** 主文字 — 深棕黑 */
val DaybookText = Color(0xFF2C2218)

/** 次要文字 — 中灰 */
val DaybookMuted = Color(0xFF9E8D7D)

/** 辅助文字 — 浅灰 */
val DaybookSubtle = Color(0xFFBFB0A1)

/** 分割线 / 描边 — 极浅暖灰 */
val DaybookLine = Color(0xFFF0E8DE)

/** 系统节日标记色 */
val DaybookSystemMark = Color(0xFFE98B5B)

/** 用户记录标记色 */
val DaybookUserMark = Color(0xFF7E9BC5)

/** 今天/选中态背景 */
val DaybookTodayTint = Color(0xFFE98B5B)

/** 浅橙背景（胶囊按钮等） */
val DaybookPrimaryTint = Color(0xFFFBE6D8)

/** 虚线边框色 */
val DaybookDashedBorder = Color(0xFFD9C9B8)

// ── Size tokens ───────────────────────────────────────────────

/** 大卡片圆角 */
val DaybookCardRadius = 24.dp

/** 中卡片圆角 */
val DaybookCardRadiusMedium = 22.dp

/** 小元素圆角 */
val DaybookCardRadiusSmall = 16.dp

/** 页面水平间距 */
val DaybookHorizontalPadding = 16.dp

/** 卡片间距 */
val DaybookCardSpacing = 12.dp

// ── Typography tokens ─────────────────────────────────────────

val DaybookMonthFontSize: TextUnit = 28.sp
val DaybookYearFontSize: TextUnit = 12.sp
val DaybookWeekLabelFontSize: TextUnit = 12.sp
val DaybookDateFontSize: TextUnit = 14.sp
val DaybookSelectedDateFontSize: TextUnit = 15.sp
val DaybookLegendFontSize: TextUnit = 11.sp
val DaybookCardTitleFontSize: TextUnit = 18.sp
val DaybookCardBodyFontSize: TextUnit = 13.sp
val DaybookCardCaptionFontSize: TextUnit = 11.sp

val DaybookMonthFontWeight = FontWeight.Medium
val DaybookTitleFontWeight = FontWeight.SemiBold
