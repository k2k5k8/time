package com.cch.momentmark.data.local

import androidx.room.Entity

/**
 * The persistence shape deliberately stores event facts only. Relative labels,
 * weekdays and status words remain derived by EventTimeCalculator.
 */
@Entity(tableName = "time_events")
data class TimeEventEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val timeType: String,
    val localDateIso: String?,
    val instantEpochMillis: Long?,
    val zoneId: String?,
    val note: String,
    val iconKey: String,
    val paletteKey: String,
    val templateKey: String,
    val templateConfigJson: String,
    val advancedConfigJson: String,
    val groupId: String?,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val sortOrder: Long,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
