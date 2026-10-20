package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TaskDao
import com.cch.momentmark.data.local.TaskEntity
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.domain.model.TaskPlan
import com.cch.momentmark.domain.model.TaskPlanPeriod
import java.time.LocalTime
import java.util.UUID
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface TaskRepositoryPort {
    fun observePending(): Flow<List<Task>> = flowOf(emptyList())
    suspend fun moveTask(id: String, date: LocalDate): Boolean = false
    suspend fun undoTaskMove() = Unit
    suspend fun setAutoDefer(id: String, enabled: Boolean) = Unit
    suspend fun makeUp(id: String) = Unit
    suspend fun reconcileRepeats() = Unit
    fun observeRepeatSeries(): Flow<List<TaskRepeatRepository.Series>> = flowOf(emptyList())
    fun observeCalendarSummary(month: java.time.YearMonth): Flow<com.cch.momentmark.domain.model.CalendarSummaryRange> =
        flowOf(com.cch.momentmark.domain.model.CalendarSummaryRange(emptyMap()))
    fun observeCalendarDates(month: java.time.YearMonth): Flow<Set<LocalDate>> = observeActive().map { rows -> rows.map { it.dueLocalDate }.filter { java.time.YearMonth.from(it) == month }.toSet() }
    suspend fun skipRepeat(id: String, skipped: Boolean) = Unit
    suspend fun pauseRepeat(id: String, stop: Boolean = false) = Unit
    suspend fun resumeRepeat(id: String) = Unit
    suspend fun editRepeatFuture(rule: com.cch.momentmark.domain.model.TaskRepeatRule) = Unit
    fun observeActive(): Flow<List<Task>> = flowOf(emptyList())
    fun observeForDate(date: LocalDate): Flow<List<Task>>
    fun observeDeleted(): Flow<List<Task>> = flowOf(emptyList())
    suspend fun findById(id: String): Task?
    suspend fun save(task: Task)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun softDelete(id: String) = Unit
    suspend fun restore(id: String) = Unit
    suspend fun permanentlyDelete(id: String) = Unit
    suspend fun purgeDeletedBefore(cutoffMillis: Long) = Unit
    suspend fun purgeAllDeleted() = Unit
}

class TaskRepository(
    private val dao: TaskDao,
    private val clock: Clock,
    private val repeats: TaskRepeatRepository? = null,
) : TaskRepositoryPort {
    private var undo: Triple<TaskEntity, String, Long>? = null
    override fun observePending() = dao.observeAll().map { rows -> rows.filter { it.repeatRuleId == null && !it.isCompleted && it.deletedAtEpochMillis == null }.map(TaskMapper::toDomain) }
    override suspend fun moveTask(id: String, date: LocalDate): Boolean {
        val now = clock.millis()
        val original = dao.move(id, date.toString(), now, "MANUAL") ?: return false
        undo = Triple(original, date.toString(), now)
        return true
    }
    override suspend fun undoTaskMove() {
        val value = undo ?: return
        dao.undoMove(value.first, value.second, value.third, clock.millis()); undo = null
    }
    override suspend fun setAutoDefer(id: String, enabled: Boolean) { dao.setAutoDefer(id, enabled, clock.millis()) }
    override suspend fun makeUp(id: String) {
        val task = findById(id) ?: error("任务不存在")
        require(task.repeatRuleId != null && !task.isCompleted && task.deletedAt == null)
        val tomorrow = LocalDate.now(clock).plusDays(1)
        save(task.copy(id = UUID.randomUUID().toString(), dueLocalDate = tomorrow, initialScheduledDate = tomorrow,
            repeatRuleId = null, repeatOccurrenceDate = null, isSkipped = false, autoDefer = false,
            repeatDisplay = com.cch.momentmark.domain.model.RepeatDisplay.STORED,
            note = listOf(task.note, "补做来源：${task.title} · ${task.repeatOccurrenceDate}").filter { it.isNotBlank() }.joinToString("\n"),
            plan = task.plan.copy(time = null), createdAt = clock.instant(), updatedAt = clock.instant()))
    }
    override suspend fun reconcileRepeats() {
        repeats?.reconcile()
        val today = LocalDate.now(clock).toString()
        dao.allTasks().filter { com.cch.momentmark.domain.model.TaskDeferral.eligible(TaskMapper.toDomain(it), clock) }
            .forEach { dao.move(it.id, today, clock.millis(), "AUTO", true) }
    }
    override fun observeRepeatSeries() = repeats?.observeSeries() ?: flowOf(emptyList())
    override fun observeCalendarSummary(month: java.time.YearMonth) = repeats?.observeCalendarSummary(month)
        ?: dao.observeScheduledTasks().map { rows ->
            val history = rows.filter { it.task.deletedAtEpochMillis == null }.flatMap { row ->
                row.changes.filter { it.fromDateIso != row.task.dueLocalDateIso }.map { LocalDate.parse(it.fromDateIso) to row.task.id }
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
            com.cch.momentmark.domain.model.projectCalendarSummary(month, rows.map { TaskMapper.toDomain(it.task) }, emptyList(), emptyList(), emptySet(), emptySet(), history, clock)
        }
    override fun observeCalendarDates(month: java.time.YearMonth) = kotlinx.coroutines.flow.combine(
        repeats?.observeCalendar(month) ?: super.observeCalendarDates(month), dao.observeScheduledTasks(),
    ) { dates, snapshot ->
        dates + snapshot.filter { it.task.repeatRuleId == null && it.task.deletedAtEpochMillis == null }
            .flatMap { row -> row.changes.map { LocalDate.parse(it.fromDateIso) } }
            .filter { java.time.YearMonth.from(it) == month }
    }
    override suspend fun skipRepeat(id: String, skipped: Boolean) { repeats?.skip(id, skipped) }
    override suspend fun pauseRepeat(id: String, stop: Boolean) { repeats?.pause(id, if (stop) com.cch.momentmark.domain.model.TaskRepeatBreakKind.STOP else com.cch.momentmark.domain.model.TaskRepeatBreakKind.PAUSE) }
    override suspend fun resumeRepeat(id: String) { repeats?.resume(id) }
    override suspend fun editRepeatFuture(rule: com.cch.momentmark.domain.model.TaskRepeatRule) { repeats?.editFuture(rule) }
    override fun observeActive(): Flow<List<Task>> = repeats?.observeActive() ?:
        dao.observeActive().map { tasks -> tasks.map(TaskMapper::toDomain) }

    override fun observeForDate(date: LocalDate): Flow<List<Task>> =
        kotlinx.coroutines.flow.combine(
            repeats?.observeForDate(date) ?: dao.observeForDate(date.toString()).map { tasks -> tasks.map(TaskMapper::toDomain) },
            dao.observeScheduledTasks(),
        ) { current, snapshot ->
            val singles = snapshot.filter { it.task.repeatRuleId == null && it.task.deletedAtEpochMillis == null }
            val scheduled = singles.filter { it.task.dueLocalDateIso == date.toString() }.map { TaskMapper.toDomain(it.task) }
                .sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.dueInstant == null }.thenBy { it.dueInstant }.thenBy { it.createdAt })
            val moved = singles.filter { it.task.dueLocalDateIso != date.toString() && it.changes.any { change -> change.fromDateIso == date.toString() } }
                .map { TaskMapper.toDomain(it.task).copy(movedTo = LocalDate.parse(it.task.dueLocalDateIso)) }
            current.filter { it.repeatRuleId != null } + scheduled + moved
        }

    override fun observeDeleted(): Flow<List<Task>> =
        dao.observeDeleted().map { tasks -> tasks.map(TaskMapper::toDomain) }

    override suspend fun findById(id: String): Task? = dao.findById(id)?.let(TaskMapper::toDomain) ?: repeats?.materialize(id)

    /** 编辑保留既有创建时间、完成状态与软删除状态，避免表单意外重置生命周期。 */
    override suspend fun save(task: Task) {
        dao.saveWithHistory(TaskMapper.toEntity(task, clock.millis()), UUID.randomUUID().toString(), "EDIT")
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        if (repeats?.complete(id, completed) == true) return
        val now = clock.millis()
        dao.setCompleted(id, completed, if (completed) now else null, now)
    }

    override suspend fun softDelete(id: String) {
        if (repeats != null) findById(id) ?: error("任务不存在")
        val now = clock.millis()
        dao.softDelete(id, deletedAt = now, updatedAt = now)
    }

    override suspend fun restore(id: String) {
        dao.restore(id, updatedAt = clock.millis(), restoredOnIso = LocalDate.now(clock).toString())
    }

    override suspend fun permanentlyDelete(id: String) {
        dao.permanentlyDelete(id)
    }

    override suspend fun purgeDeletedBefore(cutoffMillis: Long) {
        dao.purgeDeletedBefore(cutoffMillis)
    }

    override suspend fun purgeAllDeleted() {
        dao.purgeAllDeleted()
    }
}

object TaskMapper {
    fun toEntity(task: Task, nowMillis: Long, previous: TaskEntity? = null): TaskEntity = TaskEntity(
        id = task.id,
        title = task.title,
        dueLocalDateIso = task.dueLocalDate.toString(),
        dueInstantEpochMillis = task.dueInstant?.toEpochMilli(),
        zoneId = task.zoneId?.id,
        note = task.note,
        taskType = task.taskType?.name,
        difficulty = task.difficulty,
        groupId = task.groupId,
        isCompleted = task.isCompleted,
        completedAtEpochMillis = task.completedAt?.toEpochMilli(),
        showOnHome = task.showOnHome,
        deletedAtEpochMillis = task.deletedAt?.toEpochMilli() ?: previous?.deletedAtEpochMillis,
        createdAtEpochMillis = if (task.createdAt.toEpochMilli() > 0L) task.createdAt.toEpochMilli()
        else previous?.createdAtEpochMillis ?: nowMillis,
        updatedAtEpochMillis = nowMillis,
        planPeriod = task.plan.period.name, planTime = task.plan.time?.toString(), important = task.important,
        repeatRuleId = task.repeatRuleId, repeatOccurrenceDateIso = task.repeatOccurrenceDate?.toString(),
        isSkipped = task.isSkipped, initialScheduledDateIso = previous?.initialScheduledDateIso ?: task.initialScheduledDate.toString(),
        autoDefer = task.autoDefer, restoredOnIso = task.restoredOn?.toString(),
    )

    fun toDomain(entity: TaskEntity): Task = Task(
        id = entity.id,
        title = entity.title,
        dueLocalDate = LocalDate.parse(entity.dueLocalDateIso),
        dueInstant = entity.dueInstantEpochMillis?.let(Instant::ofEpochMilli),
        zoneId = entity.zoneId?.let(ZoneId::of),
        note = entity.note,
        taskType = entity.taskType?.let { raw ->
            requireNotNull(TaskType.entries.firstOrNull { it.name == raw }) { "Unknown taskType: $raw" }
        },
        difficulty = entity.difficulty,
        groupId = entity.groupId,
        isCompleted = entity.isCompleted,
        completedAt = entity.completedAtEpochMillis?.let(Instant::ofEpochMilli),
        showOnHome = entity.showOnHome,
        deletedAt = entity.deletedAtEpochMillis?.let(Instant::ofEpochMilli),
        createdAt = Instant.ofEpochMilli(entity.createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(entity.updatedAtEpochMillis),
        plan = TaskPlan(TaskPlanPeriod.valueOf(entity.planPeriod), entity.planTime?.let(LocalTime::parse)), important = entity.important,
        repeatRuleId = entity.repeatRuleId, repeatOccurrenceDate = entity.repeatOccurrenceDateIso?.let(LocalDate::parse),
        isSkipped = entity.isSkipped, initialScheduledDate = LocalDate.parse(entity.initialScheduledDateIso ?: entity.dueLocalDateIso),
        autoDefer = entity.autoDefer, restoredOn = entity.restoredOnIso?.let(LocalDate::parse),
    )
}
