package com.cch.momentmark.domain.model

import java.time.LocalDate

/**
 * 用户铭刻时刻时的方向选择（事实字段，必须持久化）。
 * 展示状态（未来倒数/就是今天/过去成就）由 EventTimeCalculator 按
 * creationDirection + anchorDate + 今天 派生，不落库（AGENTS.md §1.2）。
 */
enum class MomentDirection {
    FUTURE_COUNTDOWN,
    PAST_ACHIEVEMENT,
}

/**
 * 时刻：首页「大事件」的唯一实体，与 Task 完全独立（ARCHITECTURE §5.1）。
 * 只保存事实：相对天数、展示状态、血条占比全部是派生值。
 */
data class Moment(
    val id: String,
    val title: String,
    val note: String,
    val creationDirection: MomentDirection,
    /** 未来 = 目标日期；过去 = 开始日期。全日语义，本地自然日。 */
    val anchorDate: LocalDate,
    val groupId: String?,
    /** 重要度星级 1..3；null = 未选择。 */
    val rarity: Int?,
    val isPinned: Boolean,
    /** 置顶之间的手动顺序；未置顶时为 null。 */
    val pinnedOrder: Int?,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
