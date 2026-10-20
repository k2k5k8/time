package com.cch.momentmark.domain.model

import java.time.Clock
import java.time.LocalDate

object TaskDeferral {
    fun eligible(task: Task, clock: Clock): Boolean {
        val today = LocalDate.now(clock)
        return task.repeatRuleId == null && task.autoDefer && !task.isCompleted && task.deletedAt == null &&
            task.dueLocalDate < today && task.restoredOn != today
    }
    fun planAfterMove(plan: TaskPlan, automatic: Boolean) = if (automatic) TaskPlan() else plan.copy(time = null)
}
