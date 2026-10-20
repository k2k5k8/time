package com.cch.momentmark.data.repository

import androidx.room.withTransaction
import com.cch.momentmark.data.local.*
import com.cch.momentmark.domain.model.*
import java.time.*
import kotlinx.coroutines.flow.*

/** Room 事务统一串行化生成/编辑/暂停；跨日核对按各系列时区，与浏览日期无关。 */
class TaskRepeatRepository(private val database: MomentMarkDatabase, private val clock: Clock) {
    private val planning = database.taskPlanningDao()
    private val tasks = database.taskDao()
    private val checkedAt = MutableStateFlow(clock.instant())
    data class Series(val rule: TaskRepeatRule, val breakKind: TaskRepeatBreakKind?, val versions: List<TaskRepeatRule> = listOf(rule))
    private data class Snapshot(val rules: List<TaskRepeatRule>, val breaks: List<TaskRepeatBreak>, val tasks: List<Task>, val purged: Set<Pair<String, LocalDate>>, val exceptions: Set<Pair<String, LocalDate>>)
    private fun TaskRepeatBreakEntity.domain() = TaskRepeatBreak(ruleId, LocalDate.parse(startDateIso), endExclusiveIso?.let(LocalDate::parse), TaskRepeatBreakKind.valueOf(kind))
    private fun snapshot() = combine(planning.observeRules(), planning.observeAllBreaks(), tasks.observeAll(), planning.observeExceptions(), checkedAt) { rules, breaks, rows, exceptions, _ ->
        Snapshot(rules.map(TaskPlanningMapper::toDomain), breaks.map { it.domain() }, rows.map(TaskMapper::toDomain), exceptions.filter { it.purged }.map { it.ruleId to LocalDate.parse(it.dateIso) }.toSet(), exceptions.map { it.ruleId to LocalDate.parse(it.dateIso) }.toSet())
    }
    fun observeForDate(date: LocalDate) = combine(database.invalidationTracker
        .createFlow("tasks", "task_repeat_rules", "task_repeat_breaks", "task_repeat_exceptions"), checkedAt) { _, _ -> Unit }
        .map {
            // Classification must never combine a new rule with stale single-day exceptions.
            database.withTransaction {
                val exceptions = planning.allExceptions()
                TaskRepeatSchedule.project(date, planning.allRules().map(TaskPlanningMapper::toDomain),
                    planning.allBreaks().map { it.domain() }, tasks.allTasks().map(TaskMapper::toDomain),
                    exceptions.filter { it.purged }.map { it.ruleId to LocalDate.parse(it.dateIso) }.toSet(), clock,
                    exceptions.map { it.ruleId to LocalDate.parse(it.dateIso) }.toSet())
            }
        }
    fun observeActive() = snapshot().map { value ->
        value.tasks.map { it.dueLocalDate }.distinct().flatMap { date -> TaskRepeatSchedule.project(date, value.rules, value.breaks, value.tasks, value.purged, clock, value.exceptions) }
            .filter { !it.isSkipped && it.repeatDisplay == RepeatDisplay.STORED }
    }
    fun observeCalendar(month: YearMonth) = snapshot().map { value ->
        (1..month.lengthOfMonth()).map(month::atDay).filter { date ->
            TaskRepeatSchedule.project(date, value.rules, value.breaks, value.tasks, value.purged, clock, value.exceptions).any { !it.isSkipped && it.repeatDisplay != RepeatDisplay.UNRECORDED }
        }.toSet()
    }
    /** Invalidation observes every source, then reads one transaction (no mixed Flow snapshots). */
    fun observeCalendarSummary(month: YearMonth): Flow<CalendarSummaryRange> = database.invalidationTracker
        .createFlow("tasks", "task_repeat_rules", "task_repeat_breaks", "task_repeat_exceptions", "task_schedule_changes")
        .map {
            database.withTransaction {
                val rows = tasks.calendarTasks()
                val rules = planning.allRules().map(TaskPlanningMapper::toDomain)
                val exceptions = planning.allExceptions()
                val history = rows.filter { it.task.deletedAtEpochMillis == null }
                    .flatMap { row -> row.changes.filter { it.fromDateIso != row.task.dueLocalDateIso }
                        .map { LocalDate.parse(it.fromDateIso) to row.task.id } }
                    .groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
                projectCalendarSummary(month, rows.map { TaskMapper.toDomain(it.task) }, rules,
                    planning.allBreaks().map { it.domain() },
                    exceptions.filter { it.purged }.map { it.ruleId to LocalDate.parse(it.dateIso) }.toSet(),
                    exceptions.map { it.ruleId to LocalDate.parse(it.dateIso) }.toSet(), history, clock)
            }
        }

    fun observeSeries() = snapshot().map { value -> value.rules.map { it.id }.distinct().mapNotNull { id ->
        val rule = currentRule(value.rules, id) ?: value.rules.filter { it.id == id }.minByOrNull { it.effectiveFrom } ?: return@mapNotNull null
        val date = TaskRepeatSchedule.today(rule, clock)
        Series(rule, value.breaks.lastOrNull { it.ruleId == id && it.startDate <= date && (it.endExclusive == null || date < it.endExclusive) }?.kind, value.rules.filter { it.id == id })
    } }
    private fun currentRule(rules: List<TaskRepeatRule>, id: String) = rules.filter { it.id == id && it.effectiveFrom <= TaskRepeatSchedule.today(it, clock) }.maxByOrNull { it.effectiveFrom }

    /** 新建规则与今天实例一起提交；失败不会留下半保存系列。 */
    suspend fun create(rule: TaskRepeatRule) {
        database.withTransaction {
            require(rule.startDate >= TaskRepeatSchedule.today(rule, clock)) { "重复开始日不能早于今天" }
            planning.saveRuleVersion(TaskPlanningMapper.toEntity(rule.copy(updatedAt = clock.instant())))
            ensure(rule.id, TaskRepeatSchedule.today(rule, clock))
        }
        checkedAt.value = clock.instant()
    }

    suspend fun reconcile() {
        database.withTransaction {
            val rules = planning.allRules().map(TaskPlanningMapper::toDomain)
            rules.map { it.id }.distinct().forEach { id ->
                val rule = currentRule(rules, id) ?: return@forEach
                ensure(id, TaskRepeatSchedule.today(rule, clock))
            }
        }
        checkedAt.value = clock.instant()
    }
    private suspend fun ensure(id: String, date: LocalDate): Task? {
        tasks.findOccurrence(id, date.toString())?.let { return TaskMapper.toDomain(it) }
        if (planning.allExceptions().any { it.ruleId == id && it.dateIso == date.toString() && it.purged }) return null
        val rule = planning.findRule(id, date.toString())?.let(TaskPlanningMapper::toDomain) ?: return null
        if (!TaskRepeatSchedule.occurs(rule, date) || TaskRepeatSchedule.isOnBreak(planning.allBreaks().map { it.domain() }, id, date)) return null
        val task = TaskRepeatSchedule.instance(rule, date, clock)
        tasks.upsert(TaskMapper.toEntity(task, clock.millis()))
        return task
    }
    /** 打开编辑/单日封印是明确操作，才把未来预览保存为实例。过去未记录不补造。 */
    suspend fun materialize(taskId: String): Task? = database.withTransaction {
        tasks.findById(taskId)?.let { return@withTransaction TaskMapper.toDomain(it) }
        val (id, date) = TaskRepeatSchedule.identity(taskId) ?: return@withTransaction null
        val rule = planning.findRule(id, date.toString())?.let(TaskPlanningMapper::toDomain) ?: return@withTransaction null
        if (date < TaskRepeatSchedule.today(rule, clock)) return@withTransaction null
        ensure(id, date)
    }
    suspend fun skip(taskId: String, skipped: Boolean) = database.withTransaction {
        val task = materialize(taskId) ?: error("这个执行日已不可操作")
        val rule = planning.findRule(requireNotNull(task.repeatRuleId), requireNotNull(task.repeatOccurrenceDate).toString())?.let(TaskPlanningMapper::toDomain) ?: error("规则不存在")
        require(task.repeatOccurrenceDate == TaskRepeatSchedule.today(rule, clock)) { "只能跳过或撤销今天" }
        if (skipped) require(!TaskRepeatSchedule.isOnBreak(planning.allBreaks().map { it.domain() }, rule.id, requireNotNull(task.repeatOccurrenceDate))) { "重复已暂停" }
        require(!task.isCompleted && task.deletedAt == null) { "已完成或封印的实例不能跳过" }
        tasks.setSkipped(task.id, skipped, clock.millis())
    }
    private suspend fun operatingRule(id: String): TaskRepeatRule {
        val rules = planning.allRules().map(TaskPlanningMapper::toDomain)
        return currentRule(rules, id) ?: rules.filter { it.id == id }.minByOrNull { it.effectiveFrom } ?: error("规则不存在")
    }
    suspend fun complete(taskId: String, completed: Boolean): Boolean = database.withTransaction {
        val task = tasks.findById(taskId)?.let(TaskMapper::toDomain) ?: materialize(taskId) ?: return@withTransaction false
        val id = task.repeatRuleId ?: return@withTransaction false
        val date = requireNotNull(task.repeatOccurrenceDate)
        val rule = planning.findRule(id, date.toString())?.let(TaskPlanningMapper::toDomain) ?: error("规则不存在")
        if (completed) {
            require(date == TaskRepeatSchedule.today(rule, clock)) { "重复任务只能完成今天的实例" }
            require(!TaskRepeatSchedule.isOnBreak(planning.allBreaks().map { it.domain() }, id, date)) { "重复已暂停" }
            require(TaskRepeatSchedule.occurs(rule, date) || planning.allExceptions().any { it.ruleId == id && it.dateIso == date.toString() && !it.purged }) { "这个执行日已取消" }
            require(!task.isSkipped && task.deletedAt == null) { "跳过或封印的实例不能完成" }
        }
        tasks.setCompleted(task.id, completed, if (completed) clock.millis() else null, clock.millis())
        true
    }
    suspend fun pause(id: String, kind: TaskRepeatBreakKind = TaskRepeatBreakKind.PAUSE) = database.withTransaction {
        val rule = operatingRule(id)
        val date = TaskRepeatSchedule.today(rule, clock)
        val activeBreaks = planning.allBreaks().filter { it.ruleId == id && it.startDateIso <= date.toString() && (it.endExclusiveIso == null || it.endExclusiveIso > date.toString()) }
        if (activeBreaks.isEmpty() || kind == TaskRepeatBreakKind.STOP && activeBreaks.none { it.kind == TaskRepeatBreakKind.STOP.name }) {
            activeBreaks.forEach { planning.closeBreak(id, it.startDateIso, date.toString()) }
            planning.reopenBreak(TaskRepeatBreakEntity(id, date.toString(), null, kind.name))
        }
    }
    suspend fun resume(id: String) {
        database.withTransaction {
            val rule = operatingRule(id)
            val date = TaskRepeatSchedule.today(rule, clock)
            planning.allBreaks().filter { it.ruleId == id && it.startDateIso <= date.toString() && (it.endExclusiveIso == null || it.endExclusiveIso > date.toString()) }.forEach {
                planning.closeBreak(id, it.startDateIso, date.toString())
            }
            ensure(id, date)
        }
        checkedAt.value = clock.instant()
    }
    /** 替换生效日起版本；历史、完成/跳过/封印及单日例外保留。 */
    suspend fun editFuture(rule: TaskRepeatRule) {
        database.withTransaction {
            val original = operatingRule(rule.id)
            require(rule.effectiveFrom >= TaskRepeatSchedule.today(original, clock)) { "不能改写过去的规则" }
            require(rule.startDate == original.startDate)
            planning.deleteFutureVersions(rule.id, rule.effectiveFrom.toString())
            planning.saveRuleVersion(TaskPlanningMapper.toEntity(rule.copy(createdAt = original.createdAt, updatedAt = clock.instant())))
            val exceptions = planning.allExceptions().map { it.ruleId to it.dateIso }.toSet()
            tasks.allTasks().filter { it.repeatRuleId == rule.id && requireNotNull(it.repeatOccurrenceDateIso) >= rule.effectiveFrom.toString() && !it.isCompleted && !it.isSkipped && it.deletedAtEpochMillis == null && rule.id to it.repeatOccurrenceDateIso !in exceptions }.forEach { row ->
                val date = LocalDate.parse(row.repeatOccurrenceDateIso)
                // 取消的执行日仍保留原记录；观察时按新版本隐藏。
                if (TaskRepeatSchedule.occurs(rule, date)) {
                    val updated = TaskRepeatSchedule.instance(rule, date, clock).copy(id = row.id, createdAt = Instant.ofEpochMilli(row.createdAtEpochMillis))
                    tasks.upsert(TaskMapper.toEntity(updated, clock.millis()))
                }
            }
        }
        reconcile()
    }
}
