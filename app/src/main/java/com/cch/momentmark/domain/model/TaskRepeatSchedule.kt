package com.cch.momentmark.domain.model

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDate
import java.util.Base64

/** 有限日期查询：不为未来或长期离线的中间日期写入无限实例。 */
object TaskRepeatSchedule {
    fun ruleFor(rules: List<TaskRepeatRule>, id: String, date: LocalDate): TaskRepeatRule? =
        rules.filter { it.id == id && it.effectiveFrom <= date }.maxByOrNull { it.effectiveFrom }

    fun today(rule: TaskRepeatRule, clock: Clock): LocalDate = LocalDate.now(clock.withZone(rule.zoneId))
    fun occurs(rule: TaskRepeatRule, date: LocalDate): Boolean =
        date >= rule.startDate && date >= rule.effectiveFrom && (rule.endDate == null || date <= rule.endDate) && date.dayOfWeek in rule.weekdays
    fun isOnBreak(breaks: List<TaskRepeatBreak>, id: String, date: LocalDate): Boolean =
        breaks.any { it.ruleId == id && date >= it.startDate && (it.endExclusive == null || date < it.endExclusive) }

    fun instanceId(id: String, date: LocalDate): String = "repeat:${Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray(StandardCharsets.UTF_8))}:$date"
    fun identity(id: String): Pair<String, LocalDate>? = runCatching {
        val parts = id.split(':')
        require(parts.size == 3 && parts[0] == "repeat")
        String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8) to LocalDate.parse(parts[2])
    }.getOrNull()

    fun instance(rule: TaskRepeatRule, date: LocalDate, clock: Clock, display: RepeatDisplay = RepeatDisplay.STORED): Task = Task(
        id = instanceId(rule.id, date), title = rule.title, dueLocalDate = date,
        dueInstant = rule.deadlineTime?.let { date.atTime(it).atZone(rule.zoneId).toInstant() }, zoneId = rule.zoneId,
        note = rule.note, taskType = rule.taskType, difficulty = rule.difficulty, groupId = rule.groupId,
        isCompleted = false, completedAt = null, showOnHome = rule.showOnHome, deletedAt = null,
        createdAt = if(display == RepeatDisplay.STORED) clock.instant() else rule.createdAt, updatedAt = if(display == RepeatDisplay.STORED) clock.instant() else rule.updatedAt, plan = rule.plan, important = rule.important,
        repeatRuleId = rule.id, repeatOccurrenceDate = date, repeatDisplay = display,
    )

    fun project(date: LocalDate, rules: List<TaskRepeatRule>, breaks: List<TaskRepeatBreak>, tasks: List<Task>,
                purged: Set<Pair<String, LocalDate>>, clock: Clock, exceptions: Set<Pair<String, LocalDate>> = emptySet()): List<Task> {
        val result = tasks.filter { it.dueLocalDate == date && it.deletedAt == null }.mapNotNull { task ->
            val id = task.repeatRuleId ?: return@mapNotNull task
            val originalDate = requireNotNull(task.repeatOccurrenceDate)
            val rule = ruleFor(rules, id, originalDate) ?: return@mapNotNull task
            if (task.isCompleted || task.isSkipped) task
            else if (isOnBreak(breaks, id, originalDate) || (!occurs(rule, originalDate) && id to originalDate !in exceptions)) null
            else if (date < today(rule, clock)) task.copy(repeatDisplay = RepeatDisplay.UNRECORDED) else task
        }.toMutableList()
        rules.map { it.id }.distinct().forEach { id ->
            val rule = ruleFor(rules, id, date) ?: return@forEach
            if (!occurs(rule, date) || isOnBreak(breaks, id, date) || id to date in purged ||
                tasks.any { it.repeatRuleId == id && it.repeatOccurrenceDate == date }) return@forEach
            result += instance(rule, date, clock, if (date < today(rule, clock)) RepeatDisplay.UNRECORDED else RepeatDisplay.PREVIEW)
        }
        return result.map { task ->
            val id = task.repeatRuleId ?: return@map task
            val original = task.repeatOccurrenceDate ?: return@map task
            val rule = ruleFor(rules, id, original) ?: return@map task.copy(repeatContext = null)
            // A stored single-day edit stays foreground even if its exception marker is missing.
            val changed = task.dueLocalDate != original || task.title != rule.title || task.note != rule.note ||
                task.plan != rule.plan || task.important != rule.important || task.difficulty != rule.difficulty ||
                task.dueInstant != rule.deadlineTime?.let { original.atTime(it).atZone(rule.zoneId).toInstant() } ||
                task.zoneId != rule.zoneId || task.taskType != rule.taskType || task.groupId != rule.groupId ||
                task.showOnHome != rule.showOnHome
            task.copy(repeatContext = RepeatOccurrenceContext(rule.weekdays.size == 7,
                id to original in exceptions || changed))
        }
    }
}
