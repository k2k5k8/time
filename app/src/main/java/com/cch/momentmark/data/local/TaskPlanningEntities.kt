package com.cch.momentmark.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "task_repeat_rules", primaryKeys = ["ruleId", "effectiveFromIso"])
data class TaskRepeatRuleEntity(
    val ruleId: String, val effectiveFromIso: String, val startDateIso: String, val endDateIso: String?,
    val weekdaysMask: Int, val zoneId: String, val title: String, val note: String,
    val planPeriod: String, val planTime: String?, val deadlineTime: String?, val important: Boolean,
    val difficulty: Int?, val taskType: String?, val groupId: String?, val showOnHome: Boolean,
    val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long,
)

@Entity(tableName = "task_repeat_breaks", primaryKeys = ["ruleId", "startDateIso"])
data class TaskRepeatBreakEntity(val ruleId: String, val startDateIso: String, val endExclusiveIso: String?, val kind: String)

/** 单日编辑与净化事实独立于任务生命周期，净化后也不能重新生成。 */
@Entity(tableName = "task_repeat_exceptions", primaryKeys = ["ruleId", "dateIso"])
data class TaskRepeatExceptionEntity(val ruleId: String, val dateIso: String, val purged: Boolean)

@Entity(tableName = "task_schedule_changes", indices = [Index("taskId")], foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
])
data class TaskScheduleChangeEntity(
    @PrimaryKey val id: String, val taskId: String, val fromDateIso: String, val toDateIso: String,
    val changedAtEpochMillis: Long, val reason: String,
)
