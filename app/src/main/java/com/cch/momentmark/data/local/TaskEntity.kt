package com.cch.momentmark.data.local

import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Task 的 Room 形状：显式列，避免把新 P0 字段继续塞进 JSON。 */
@Entity(tableName = "tasks", indices = [Index(value = ["repeatRuleId", "repeatOccurrenceDateIso"], unique = true)])
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueLocalDateIso: String,
    val dueInstantEpochMillis: Long?,
    val zoneId: String?,
    val note: String,
    val taskType: String?,
    val difficulty: Int?,
    val groupId: String?,
    val isCompleted: Boolean,
    val completedAtEpochMillis: Long?,
    val showOnHome: Boolean,
    val deletedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "'ANYTIME'") val planPeriod: String = "ANYTIME",
    val planTime: String? = null,
    @ColumnInfo(defaultValue = "0") val important: Boolean = false,
    val repeatRuleId: String? = null,
    val repeatOccurrenceDateIso: String? = null,
    @ColumnInfo(defaultValue = "0") val isSkipped: Boolean = false,
    val initialScheduledDateIso: String? = null,
    @ColumnInfo(defaultValue = "0") val autoDefer: Boolean = false,
    val restoredOnIso: String? = null,
)
