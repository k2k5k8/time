package com.cch.momentmark.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 设计 token：像素冒险的间距、像素元素与动效规格。
 * 颜色见 Color.kt，字号见 Type.kt，形状见 Shapes.kt（全局直角）。
 * 完整规范与使用规则见 DESIGN_SYSTEM.md。
 */
object MomentMarkTokens {

    // ── 间距（4dp 栅格）──────────────────────────────────
    val SpaceUnit = 4.dp
    val SpacePage = 14.dp        // 页面水平边距
    val SpaceCard = 10.dp        // 卡片/任务卡之间
    val SpaceInner = 12.dp       // 卡片内边距
    val SpaceSection = 18.dp     // 区块之间

    // ── 像素元素 ─────────────────────────────────────────
    val PxBorderWidth = 3.dp     // 全局墨线描边
    val PxShadowOffset = 3.dp    // 硬偏移阴影（普通）
    val PxShadowOffsetLarge = 4.dp // 硬偏移阴影（主卡/弹窗）
    val PressOffset = 2.dp       // 按压时位移 + 阴影缩短量

    // ── 游戏化组件 ───────────────────────────────────────
    const val XpCellCount = 16   // 经验条总格数
    val XpCellHeight = 12.dp
    val XpCellGap = 3.dp
    val HpBarHeight = 10.dp      // 血条高度
    val CheckboxSize = 15.dp     // 目标复选框

    // ── 动效 ────────────────────────────────────────────
    const val MotionStepMs = 200          // 阶梯式变化（减血/落位）
    const val MotionPressMs = 100         // 按压位移响应
    const val MotionLongPressEditMs = 500L // 长按进入布局编辑（沿用既有交互契约）

    // ── 状态透明度 ───────────────────────────────────────
    const val AlphaStackedCard = 0.38f    // 装备库相邻卡
    const val AlphaDisabled = 0.6f        // 空线/不可用分组
    const val AlphaScrim = 0.5f           // 抽屉/弹窗压暗层
}
