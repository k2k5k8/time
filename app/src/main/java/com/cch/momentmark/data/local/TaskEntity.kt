package com.cch.momentmark.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Task 的 Room 形状：显式列，避免把新 P0 字段继续塞进 JSON。 */
@Entity(tableName = "tasks")
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
)
