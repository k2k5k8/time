package com.cch.momentmark.ui.daybook

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate

internal data class CompletionKey(val date: LocalDate, val id: String)
internal class CompletionTransition(val task: Task, val period: TaskPeriod, val order: List<String>) {
    var writeSucceeded by mutableStateOf(false)
    val strike = Animatable(0f)
}

/** Ephemeral decoration only. Discarding it never cancels or replays the ViewModel write. */
internal class DaybookCompletionTransitions {
    val entries = mutableStateMapOf<CompletionKey, CompletionTransition>()
    fun begin(date: LocalDate, task: Task, projection: DaybookTaskProjection): CompletionTransition? {
        val key = CompletionKey(date, task.id)
        if (task.isCompleted || key in entries) return null
        val section = projection.sections.firstOrNull { s -> s.tasks.any { it.id == task.id } } ?: return null
        return CompletionTransition(task, section.period, section.tasks.map { it.id }).also { entries[key] = it }
    }
    fun finish(key: CompletionKey, entry: CompletionTransition) {
        if (entries[key] === entry) entries.remove(key)
    }
}

@Composable
internal fun DaybookCompletionEffects(transitions: DaybookCompletionTransitions, tasks: List<Task>) {
    transitions.entries.toList().forEach { (id, entry) -> key(id, entry) {
        val record = tasks.firstOrNull { it.id == id.id && it.dueLocalDate == id.date }
        val observed = record?.isCompleted == true && record.deletedAt == null
        LaunchedEffect(observed, entry.writeSucceeded, record == null, record?.deletedAt, record?.movedTo) {
            if (record == null || record.deletedAt != null || record.movedTo != null) transitions.finish(id, entry)
            else if (observed && entry.writeSucceeded) {
                entry.strike.animateTo(1f, tween(MomentMarkTokens.DaybookCompletionStrikeMs, easing = LinearEasing))
                transitions.finish(id, entry)
            }
        }
    } }
}
