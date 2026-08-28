package com.cch.momentmark.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 刻间设计语言「像素冒险 Pixel Quest」色板。
 *
 * 唯一取值来源，规格见 DESIGN_SYSTEM.md §Token。
 * 新代码禁止在 UI 文件中硬编码 Color(0x…)；
 * 浅色 *Light / 深色(洞窟) *Dark 成对定义，游戏色与语义色分区放置。
 */

// ── 昼 · 任务板（浅色模式） ────────────────────────────────
val MmBackgroundLight = Color(0xFFE8E0CC)
val MmSurfaceLight = Color(0xFFF6F1E3)
val MmSurfaceVariantLight = Color(0xFFDFD5BB)
val MmLabelLight = Color(0xFF2B2620)
val MmLabelSecondaryLight = Color(0xFF6B6152)
val MmLabelTertiaryLight = Color(0xFFA79B85)
val MmOutlineLight = Color(0xFF2B2620)
val MmOutlineVariantLight = Color(0xFFC9BFA6)

// ── 夜 · 洞窟（深色模式） ─────────────────────────────────
val MmBackgroundDark = Color(0xFF1B1826)
val MmSurfaceDark = Color(0xFF262138)
val MmSurfaceVariantDark = Color(0xFF37304E)
val MmLabelDark = Color(0xFFE4DEF2)
val MmLabelSecondaryDark = Color(0xFF9C93B8)
val MmLabelTertiaryDark = Color(0xFF5E5680)
val MmOutlineDark = Color(0xFF0E0C16)
val MmOutlineVariantDark = Color(0xFF37304E)

// ── 游戏色 · 昼 ───────────────────────────────────────────
val MmHpGreenLight = Color(0xFF46C168)      // HP 绿：主操作 / 进度正常（>60%）
val MmAmberLight = Color(0xFFF5A623)        // 限时橙：紧迫（30–60%）
val MmDangerLight = Color(0xFFE5484D)       // 危险红：<30% / 删除 / 放弃
val MmXpBlueLight = Color(0xFF3E9BFF)       // XP 蓝：经验 / 链接 / 支线
val MmGoldLight = Color(0xFFF5B301)         // 金币金：奖励 / 选中 / 复活
val MmManaPurpleLight = Color(0xFF9B6DFF)   // 魔法紫：主线 / 节日

// ── 游戏色 · 夜（洞窟） ───────────────────────────────────
val MmHpGreenDark = Color(0xFF5BD385)
val MmAmberDark = Color(0xFFFFB84D)
val MmDangerDark = Color(0xFFFF6B6F)
val MmXpBlueDark = Color(0xFF66B0FF)
val MmGoldDark = Color(0xFFFFC53D)
val MmManaPurpleDark = Color(0xFFB79AFF)

// ── 容器色（选中/浅底） ───────────────────────────────────
val MmHpContainerLight = Color(0xFFDFF3E5)
val MmOnHpContainerLight = Color(0xFF1C5A30)
val MmHpContainerDark = Color(0xFF1F4530)
val MmOnHpContainerDark = Color(0xFFBDF0CD)

val MmXpContainerLight = Color(0xFFDEEAFF)
val MmOnXpContainerLight = Color(0xFF14496B)
val MmXpContainerDark = Color(0xFF1E3A5C)
val MmOnXpContainerDark = Color(0xFFCCE5FF)

val MmDangerContainerLight = Color(0xFFFBE0E0)
val MmOnDangerContainerLight = Color(0xFF8E1F24)
val MmDangerContainerDark = Color(0xFF4A1D20)
val MmOnDangerContainerDark = Color(0xFFFFD9DA)
