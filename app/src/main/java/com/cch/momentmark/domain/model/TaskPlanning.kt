package com.cch.momentmark.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** 执行安排独立于截止；精确时间与显式时段互斥。 */
enum class TaskPlanPeriod { ANYTIME, MORNING, MIDDAY, EVENING }
data class TaskPlan(val period: TaskPlanPeriod = TaskPlanPeriod.ANYTIME, val time: LocalTime? = null) {
    /** Shared by list projection and calendar summary; exact time takes precedence. */
    val resolvedPeriod: TaskPlanPeriod get() = resolveTaskPlanPeriod(period, time)

    init {
        require(time == null || period == TaskPlanPeriod.ANYTIME)
        require(time == null || (time.second == 0 && time.nano == 0))
    }
}

/** Pure classification: no clock, storage or UI dependency. */
fun resolveTaskPlanPeriod(period: TaskPlanPeriod, time: LocalTime?): TaskPlanPeriod = time?.let {
    when {
        it.hour < 12 -> TaskPlanPeriod.MORNING
        it.hour < 18 -> TaskPlanPeriod.MIDDAY
        else -> TaskPlanPeriod.EVENING
    }
} ?: period

/** 同一系列按生效日起保存版本；后续编辑不覆盖更早版本。 */
data class TaskRepeatRule(
    val id: String,
    val effectiveFrom: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val weekdays: Set<DayOfWeek>,
    val zoneId: ZoneId,
    val title: String,
    val note: String = "",
    val plan: TaskPlan = TaskPlan(),
    val deadlineTime: LocalTime? = null,
    val important: Boolean = false,
    val difficulty: Int? = null,
    val taskType: TaskType? = null,
    val groupId: String? = null,
    val showOnHome: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank() && title.isNotBlank())
        require(weekdays.isNotEmpty())
        require(effectiveFrom >= startDate)
        require(endDate == null || endDate >= effectiveFrom)
        require(difficulty == null || difficulty in 1..3)
        require(deadlineTime == null || (deadlineTime.second == 0 && deadlineTime.nano == 0))
        require((0L..6L).any { offset ->
            val date = effectiveFrom.plusDays(offset)
            (endDate == null || date <= endDate) && date.dayOfWeek in weekdays
        }) { "起止区间没有有效执行日" }
    }
}

/** 暂停/停止区间保留历史，结束日不包含恢复当天。 */
enum class TaskRepeatBreakKind { PAUSE, STOP }
data class TaskRepeatBreak(val ruleId: String, val startDate: LocalDate, val endExclusive: LocalDate? = null, val kind: TaskRepeatBreakKind) {
    init { require(endExclusive == null || endExclusive >= startDate) }
}

enum class TaskScheduleChangeReason { EDIT, MANUAL, AUTO }
data class TaskScheduleChange(val id: String, val taskId: String, val fromDate: LocalDate, val toDate: LocalDate, val changedAt: Instant, val reason: TaskScheduleChangeReason)
