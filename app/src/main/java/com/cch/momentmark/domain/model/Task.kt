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
    val plan: TaskPlan = TaskPlan(),
    val important: Boolean = false,
    /** 实例原执行日不会随着编辑所属日改变；同系列同原执行日身份唯一。 */
    val repeatRuleId: String? = null,
    val repeatOccurrenceDate: LocalDate? = null,
    val isSkipped: Boolean = false,
    val initialScheduledDate: LocalDate = dueLocalDate,
    val autoDefer: Boolean = false,
    val restoredOn: LocalDate? = null,
    /** 只读日期投影，不落库；过去缺失实例不能伪装成实际待办。 */
    val movedTo: LocalDate? = null,
    val repeatDisplay: RepeatDisplay = RepeatDisplay.STORED,
    /** Read-only provenance from the same rule/exception snapshot; never persisted. Null is unknown. */
    val repeatContext: RepeatOccurrenceContext? = null,
) {
    init {
        require((repeatRuleId == null) == (repeatOccurrenceDate == null))
        require(repeatRuleId == null || !autoDefer)
        require(!isSkipped || (repeatRuleId != null && !isCompleted))
    }
}

enum class TaskType {
    MAIN,
    SIDE,
    LIMITED,
}

enum class RepeatDisplay { STORED, PREVIEW, UNRECORDED }

data class RepeatOccurrenceContext(val daily: Boolean, val hasException: Boolean)
