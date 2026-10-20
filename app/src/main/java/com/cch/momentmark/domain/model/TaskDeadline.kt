package com.cch.momentmark.domain.model

import java.time.Instant
import java.time.Duration

enum class TaskDeadlineState { NONE, NORMAL, SOON, OVERDUE }

fun taskDeadlineState(task: Task, now: Instant): TaskDeadlineState {
    val deadline = task.dueInstant ?: return TaskDeadlineState.NONE
    if (task.isCompleted || task.deletedAt != null || task.isSkipped || task.repeatDisplay == com.cch.momentmark.domain.model.RepeatDisplay.UNRECORDED) return TaskDeadlineState.NONE
    return when {
        now > deadline -> TaskDeadlineState.OVERDUE
        Duration.between(now, deadline) <= Duration.ofHours(2) -> TaskDeadlineState.SOON
        else -> TaskDeadlineState.NORMAL
    }
}

