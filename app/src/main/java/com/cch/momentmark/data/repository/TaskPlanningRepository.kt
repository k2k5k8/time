package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.*
import com.cch.momentmark.domain.model.*
import java.time.*
import kotlinx.coroutines.flow.map

/** 规则数据持久化；生产保存后由 DB-10 运行层幂等补齐当前执行日。 */
class TaskPlanningRepository(private val dao: TaskPlanningDao, private val clock: Clock, private val onRuleSaved: suspend () -> Unit = {}) {
    fun observeRuleVersions(id: String) = dao.observeRuleVersions(id).map { rows -> rows.map(TaskPlanningMapper::toDomain) }
    suspend fun findRule(id: String, date: LocalDate) = dao.findRule(id, date.toString())?.let(TaskPlanningMapper::toDomain)
    suspend fun saveRule(rule: TaskRepeatRule) { dao.saveRuleVersion(TaskPlanningMapper.toEntity(rule.copy(updatedAt = clock.instant()))); onRuleSaved() }
    fun observeBreaks(id: String) = dao.observeBreaks(id).map { rows -> rows.map { TaskRepeatBreak(it.ruleId, LocalDate.parse(it.startDateIso), it.endExclusiveIso?.let(LocalDate::parse), TaskRepeatBreakKind.valueOf(it.kind)) } }
    suspend fun saveBreak(value: TaskRepeatBreak) = dao.saveBreak(TaskRepeatBreakEntity(value.ruleId, value.startDate.toString(), value.endExclusive?.toString(), value.kind.name))
    fun observeScheduleChanges(id: String) = dao.observeScheduleChanges(id).map { rows -> rows.map { TaskScheduleChange(it.id, it.taskId, LocalDate.parse(it.fromDateIso), LocalDate.parse(it.toDateIso), Instant.ofEpochMilli(it.changedAtEpochMillis), TaskScheduleChangeReason.valueOf(it.reason)) } }
}

object TaskPlanningMapper {
    fun toEntity(rule: TaskRepeatRule) = TaskRepeatRuleEntity(
        rule.id, rule.effectiveFrom.toString(), rule.startDate.toString(), rule.endDate?.toString(),
        rule.weekdays.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }, rule.zoneId.id,
        rule.title, rule.note, rule.plan.period.name, rule.plan.time?.toString(), rule.deadlineTime?.toString(),
        rule.important, rule.difficulty, rule.taskType?.name, rule.groupId, rule.showOnHome,
        rule.createdAt.toEpochMilli(), rule.updatedAt.toEpochMilli(),
    )
    fun toDomain(row: TaskRepeatRuleEntity): TaskRepeatRule {
        require(row.weekdaysMask in 1..127)
        return TaskRepeatRule(
            row.ruleId, LocalDate.parse(row.effectiveFromIso), LocalDate.parse(row.startDateIso), row.endDateIso?.let(LocalDate::parse),
            DayOfWeek.entries.filter { row.weekdaysMask and (1 shl (it.value - 1)) != 0 }.toSet(), ZoneId.of(row.zoneId), row.title, row.note,
            TaskPlan(TaskPlanPeriod.valueOf(row.planPeriod), row.planTime?.let(LocalTime::parse)), row.deadlineTime?.let(LocalTime::parse),
            row.important, row.difficulty, row.taskType?.let(TaskType::valueOf), row.groupId, row.showOnHome,
            Instant.ofEpochMilli(row.createdAtEpochMillis), Instant.ofEpochMilli(row.updatedAtEpochMillis),
        )
    }
}
