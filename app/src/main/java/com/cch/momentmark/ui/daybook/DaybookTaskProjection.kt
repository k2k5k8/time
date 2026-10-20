package com.cch.momentmark.ui.daybook

import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.RepeatDisplay
import com.cch.momentmark.ui.components.TaskRowMark
import java.time.LocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalTime

/** 展示输入默认从真实 Task 映射；旧任务随时，不能用截止推导安排。 */
enum class TaskPeriod(val label: String, val description: String) {
    MORNING("早", "早间"), MIDDAY("中", "中午及下午"), EVENING("晚", "晚间"), ANYTIME("随时", "随时"),
}

data class TaskExecutionPlan(val period: TaskPeriod = TaskPeriod.ANYTIME, val time: LocalTime? = null, val important: Boolean = false) {
    val resolvedPeriod: TaskPeriod get() = TaskPeriod.valueOf(
        com.cch.momentmark.domain.model.resolveTaskPlanPeriod(
            com.cch.momentmark.domain.model.TaskPlanPeriod.valueOf(period.name), time,
        ).name,
    )
}

internal fun Task.executionPlan() = TaskExecutionPlan(TaskPeriod.valueOf(plan.period.name), plan.time, important)

typealias TaskDeadlineState = com.cch.momentmark.domain.model.TaskDeadlineState
data class DaybookTaskSection(val period: TaskPeriod, val tasks: List<Task>)
data class DaybookTaskProjection(val sections: List<DaybookTaskSection>, val completed: List<Task>,
    val routineIds: Set<String> = emptySet(), val future: Boolean = false)

fun taskDeadlineState(task: Task, now: Instant) = com.cch.momentmark.domain.model.taskDeadlineState(task, now)

fun projectDaybookTasks(tasks: List<Task>, clock: Clock, plans: Map<String, TaskExecutionPlan> = emptyMap(),
    date: LocalDate? = null, completionHolds: List<CompletionHold> = emptyList()): DaybookTaskProjection {
    fun plan(task: Task) = plans[task.id] ?: task.executionPlan()
    val now = clock.instant()
    val future = date != null && date > LocalDate.now(clock)
    val held = completionHolds.associateBy { it.task.id }
    val active = tasks.map { held[it.id]?.task ?: it }.filter { it.movedTo == null && it.deletedAt == null && !it.isSkipped && it.repeatDisplay != com.cch.momentmark.domain.model.RepeatDisplay.UNRECORDED }
    val routineIds = if (future) active.filter {
        !it.isCompleted && it.repeatRuleId != null && it.repeatContext?.daily == true && it.repeatContext.hasException == false &&
            !plan(it).important && taskDeadlineState(it, now) !in setOf(TaskDeadlineState.OVERDUE, TaskDeadlineState.SOON)
    }.map { it.id }.toSet() else emptySet()
    val comparator = compareBy<Task> {
        when (taskDeadlineState(it, now)) {
            TaskDeadlineState.OVERDUE -> 0
            TaskDeadlineState.SOON -> 1
            else -> if (plan(it).important) 2 else 3
        }
    }.thenBy { it.dueInstant ?: Instant.MAX }
        .thenBy { plan(it).time ?: LocalTime.MAX }
        .thenBy { if (future && it.id in routineIds) 1 else 0 }
        .thenBy { it.createdAt }.thenBy { it.id }
    val sections = TaskPeriod.entries.mapNotNull { period ->
            active.filter { !it.isCompleted && plan(it).resolvedPeriod == period }
                .sortedWith(comparator).let { restoreCompletionPositions(it, completionHolds.filter { h -> h.period == period }) }.takeIf { it.isNotEmpty() }?.let { DaybookTaskSection(period, it) }
        }
    return DaybookTaskProjection(sections,
        active.filter { it.isCompleted }.sortedWith(
            compareBy<Task> { plan(it).resolvedPeriod.ordinal }
                .thenBy { plan(it).time ?: LocalTime.MAX }
                .thenBy { it.createdAt }.thenBy { it.id },
        ),
        routineIds, future,
    )
}

/** Read-only UI policy, using each repeat rule's timezone; rendering never materializes an instance. */
internal data class DaybookTaskRowState(
    val mark: TaskRowMark,
    val description: String,
    val canComplete: Boolean = false,
    val canOpen: Boolean = false,
    val canSeal: Boolean = false,
)

internal fun taskRowState(task: Task, date: LocalDate, clock: Clock): DaybookTaskRowState {
    val today = LocalDate.now(clock.withZone(task.zoneId ?: clock.zone))
    return when {
        task.deletedAt != null -> DaybookTaskRowState(TaskRowMark.EMPTY, "已封印")
        task.movedTo != null -> DaybookTaskRowState(TaskRowMark.ARROW, "已移至 ${task.movedTo}", canOpen = true)
        task.repeatDisplay == RepeatDisplay.UNRECORDED -> DaybookTaskRowState(TaskRowMark.CROSS, "过去重复任务未记录")
        task.isSkipped -> DaybookTaskRowState(TaskRowMark.SKIP, "已跳过",
            canOpen = task.repeatOccurrenceDate == today)
        task.isCompleted -> DaybookTaskRowState(TaskRowMark.CHECK, "已完成", true, true, true)
        task.repeatRuleId != null && (date > today || task.repeatDisplay == RepeatDisplay.PREVIEW) -> DaybookTaskRowState(TaskRowMark.CLOCK, "未来重复任务，尚未到执行日", canOpen = true, canSeal = true)
        else -> DaybookTaskRowState(if (date < today) TaskRowMark.CROSS else TaskRowMark.EMPTY,
            if (date < today) "该日未完成" else "未完成",
            task.repeatRuleId == null || task.repeatOccurrenceDate == today, true, true)
    }
}

/** Capture the original section and its neighbouring identities, independent of current risk sorting. */
data class CompletionHold(val task: Task, val period: TaskPeriod, val order: List<String>)
private fun restoreCompletionPositions(rows: List<Task>, holds: List<CompletionHold>): List<Task> {
    if (holds.isEmpty()) return rows
    // Keep the existing section geometry while the user watches its title being crossed out.
    // Newly arriving identities append in their normal comparator order until the handoff ends.
    val order = holds.maxBy { it.order.size }.order.withIndex().associate { it.value to it.index }
    return rows.sortedBy { order[it.id] ?: Int.MAX_VALUE }
}
