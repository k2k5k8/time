package com.cch.momentmark.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 日子簿的独立待办实体；不复用 Moment 或遗留 TimeEvent。 */
data class Task(
    val id: String,
    val title: String,
    /** 决定日子簿归属的本地自然日，始终存在。 */
    val dueLocalDate: LocalDate,
    /** 可选精确截止时刻，仅用于展示/未来提醒，不改变日子簿归属。 */
    val dueInstant: Instant?,
    val zoneId: ZoneId?,
    val note: String,
    val taskType: TaskType?,
    val difficulty: Int?,
    val groupId: String?,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    /** 默认 false；P0 后续由首页投影读取，绝不把 Task 转为 Moment。 */
    val showOnHome: Boolean,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class TaskType {
    MAIN,
    SIDE,
    LIMITED,
}
