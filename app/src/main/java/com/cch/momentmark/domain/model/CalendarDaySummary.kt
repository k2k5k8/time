package com.cch.momentmark.domain.model

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDaySummary(
    val periodCounts: Map<TaskPlanPeriod, Int> = emptyMap(),
    val taskCount: Int = 0,
    val completedCount: Int = 0,
    val overdueCount: Int = 0,
    val soonCount: Int = 0,
    val movedCount: Int = 0,
) {
    /** Fixed slot order; multiple tasks in one period still produce just one solid marker. */
    val periods: List<TaskPlanPeriod> get() = TaskPlanPeriod.entries.filter { (periodCounts[it] ?: 0) > 0 }
    val historyOnly: Boolean get() = taskCount == 0 && movedCount > 0
    fun description(): String = buildString {
        if (taskCount > 0) {
            append("，${taskCount}项任务，")
            append(periods.joinToString("、") {
                val label = when (it) {
                    TaskPlanPeriod.ANYTIME -> "随时"
                    TaskPlanPeriod.MORNING -> "早上"
                    TaskPlanPeriod.MIDDAY -> "中午"
                    TaskPlanPeriod.EVENING -> "晚上"
                }
                "$label${periodCounts.getValue(it)}项"
            })
            if (completedCount > 0) append("，已完成${completedCount}项")
            if (overdueCount > 0) append("，已过截止${overdueCount}项")
            if (soonCount > 0) append("，即将截止${soonCount}项")
        }
        if (movedCount > 0) append("，${movedCount}项移日历史")
    }
}

data class CalendarSummaryRange(val days: Map<LocalDate, CalendarDaySummary>, val nextRefreshAt: Instant? = null)

/** Reuse the same repeat projection as the daily list. All inputs belong to one Room transaction. */
fun projectCalendarSummary(
    month: YearMonth,
    tasks: List<Task>,
    rules: List<TaskRepeatRule>,
    breaks: List<TaskRepeatBreak>,
    purged: Set<Pair<String, LocalDate>>,
    exceptions: Set<Pair<String, LocalDate>>,
    movedFrom: Map<LocalDate, Set<String>>,
    clock: Clock,
): CalendarSummaryRange {
    val now = clock.instant()
    val days = linkedMapOf<LocalDate, CalendarDaySummary>()
    val boundaries = mutableListOf<Instant>()
    for (offset in -1L..1L) {
        val current = month.plusMonths(offset)
        for (day in 1..current.lengthOfMonth()) {
            val date = current.atDay(day)
            val projected = TaskRepeatSchedule.project(date, rules, breaks, tasks, purged, clock, exceptions)
            val active = projected.filter { it.deletedAt == null && !it.isSkipped && it.movedTo == null && it.repeatDisplay != RepeatDisplay.UNRECORDED }
            active.forEach { task ->
                val risk = taskDeadlineState(task, now)
                if (risk != TaskDeadlineState.NONE) task.dueInstant?.let { deadline ->
                    boundaries += deadline.minus(Duration.ofHours(2))
                    boundaries += deadline.plusMillis(1)
                }
            }
            val summary = CalendarDaySummary(active.groupingBy { it.plan.resolvedPeriod }.eachCount(), active.size,
                active.count { it.isCompleted }, active.count { taskDeadlineState(it, now) == TaskDeadlineState.OVERDUE },
                active.count { taskDeadlineState(it, now) == TaskDeadlineState.SOON }, movedFrom[date].orEmpty().size)
            // Empty dates are explicit successful results, distinct from absent/unloaded months.
            days[date] = summary
        }
    }
    (rules.map { it.zoneId } + clock.zone).distinct().forEach { zone ->
        boundaries += now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
    }
    return CalendarSummaryRange(days, boundaries.filter { it > now }.minOrNull())
}
