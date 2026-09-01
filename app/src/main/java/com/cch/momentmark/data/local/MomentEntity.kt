package com.cch.momentmark.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 时刻的持久化形状（ARCHITECTURE §5.1）：只存事实字段。
 * 方向为枚举名；日期为 ISO LocalDate 文本；相对天数与展示状态由内核派生。
 */
@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String,
    val creationDirection: String,
    val anchorDateIso: String,
    val groupId: String?,
    val rarity: Int?,
    val isPinned: Boolean,
    val pinnedOrder: Int?,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
