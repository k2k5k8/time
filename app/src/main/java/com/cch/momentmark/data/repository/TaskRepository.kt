package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TaskDao
import com.cch.momentmark.data.local.TaskEntity
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface TaskRepositoryPort {
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
) : TaskRepositoryPort {
    override fun observeActive(): Flow<List<Task>> =
        dao.observeActive().map { tasks -> tasks.map(TaskMapper::toDomain) }

    override fun observeForDate(date: LocalDate): Flow<List<Task>> =
        dao.observeForDate(date.toString()).map { tasks -> tasks.map(TaskMapper::toDomain) }

    override fun observeDeleted(): Flow<List<Task>> =
        dao.observeDeleted().map { tasks -> tasks.map(TaskMapper::toDomain) }

    override suspend fun findById(id: String): Task? = dao.findById(id)?.let(TaskMapper::toDomain)

    /** 编辑保留既有创建时间、完成状态与软删除状态，避免表单意外重置生命周期。 */
    override suspend fun save(task: Task) {
        val previous = dao.findById(task.id)
        dao.upsert(TaskMapper.toEntity(task, clock.millis(), previous))
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        val now = clock.millis()
        dao.setCompleted(id, completed, if (completed) now else null, now)
    }

    override suspend fun softDelete(id: String) {
        val now = clock.millis()
        dao.softDelete(id, deletedAt = now, updatedAt = now)
    }

    override suspend fun restore(id: String) {
        dao.restore(id, updatedAt = clock.millis())
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
    )
}
