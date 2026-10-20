package com.cch.momentmark.data.local

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Transaction
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks")
    abstract fun observeScheduledTasks(): Flow<List<TaskWithScheduleChanges>>

    @Transaction
    @Query("SELECT * FROM tasks")
    abstract suspend fun calendarTasks(): List<TaskWithScheduleChanges>

    @Transaction
    open suspend fun move(id: String, dateIso: String, now: Long, reason: String, automatic: Boolean = false): TaskEntity? {
        val row = findById(id) ?: error("任务不存在")
        if (automatic && (row.repeatRuleId != null || row.isCompleted || row.deletedAtEpochMillis != null)) return null
        require(row.repeatRuleId == null && !row.isCompleted && row.deletedAtEpochMillis == null) { "只能安排未完成的单次任务" }
        if (row.dueLocalDateIso == dateIso) return null
        if (automatic && (!row.autoDefer || row.dueLocalDateIso >= dateIso || row.restoredOnIso == dateIso)) return null
        val plan = com.cch.momentmark.domain.model.TaskDeferral.planAfterMove(com.cch.momentmark.domain.model.TaskPlan(com.cch.momentmark.domain.model.TaskPlanPeriod.valueOf(row.planPeriod), row.planTime?.let(java.time.LocalTime::parse)), automatic)
        val changed = row.copy(dueLocalDateIso = dateIso, planTime = null,
            planPeriod = plan.period.name, updatedAtEpochMillis = now,
            initialScheduledDateIso = row.initialScheduledDateIso ?: row.dueLocalDateIso)
        upsert(changed)
        insertScheduleChange(TaskScheduleChangeEntity(java.util.UUID.randomUUID().toString(), id, row.dueLocalDateIso, dateIso, now, reason))
        return row
    }

    @Transaction
    open suspend fun undoMove(original: TaskEntity, targetIso: String, revision: Long, now: Long) {
        val current = findById(original.id) ?: error("任务不存在")
        require(current.updatedAtEpochMillis == revision && current.dueLocalDateIso == targetIso && !current.isCompleted && current.deletedAtEpochMillis == null) { "任务已经变化，不能撤销这次安排" }
        upsert(current.copy(dueLocalDateIso = original.dueLocalDateIso, planPeriod = original.planPeriod, planTime = original.planTime, updatedAtEpochMillis = now))
        insertScheduleChange(TaskScheduleChangeEntity(java.util.UUID.randomUUID().toString(), current.id, targetIso, original.dueLocalDateIso, now, "UNDO"))
    }
    @Query("UPDATE tasks SET autoDefer = :enabled, updatedAtEpochMillis = :now WHERE id = :id AND repeatRuleId IS NULL AND deletedAtEpochMillis IS NULL")
    abstract suspend fun setAutoDefer(id: String, enabled: Boolean, now: Long)

    @Query("SELECT * FROM tasks")
    abstract fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks")
    abstract suspend fun allTasks(): List<TaskEntity>
    @Query("UPDATE tasks SET isSkipped = :skipped, updatedAtEpochMillis = :now WHERE id = :id AND repeatRuleId IS NOT NULL AND deletedAtEpochMillis IS NULL AND isCompleted = 0")
    abstract suspend fun setSkipped(id: String, skipped: Boolean, now: Long)
    @Upsert
    abstract suspend fun saveRepeatException(value: TaskRepeatExceptionEntity)

    @Query("SELECT * FROM tasks WHERE deletedAtEpochMillis IS NULL AND isSkipped = 0")
    abstract fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAtEpochMillis IS NOT NULL ORDER BY deletedAtEpochMillis DESC")
    abstract fun observeDeleted(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE deletedAtEpochMillis IS NULL AND isSkipped = 0 AND dueLocalDateIso = :dateIso " +
            "ORDER BY isCompleted ASC, dueInstantEpochMillis IS NULL ASC, dueInstantEpochMillis ASC, createdAtEpochMillis ASC",
    )
    abstract fun observeForDate(dateIso: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: String): TaskEntity?

    @Upsert
    abstract suspend fun upsert(task: TaskEntity)

    @Query(
        "UPDATE tasks SET isCompleted = :completed, completedAtEpochMillis = :completedAt, " +
            "updatedAtEpochMillis = :updatedAt WHERE id = :id AND deletedAtEpochMillis IS NULL AND isSkipped = 0",
    )
    abstract suspend fun setCompleted(id: String, completed: Boolean, completedAt: Long?, updatedAt: Long)

    @Query(
        "UPDATE tasks SET deletedAtEpochMillis = :deletedAt, updatedAtEpochMillis = :updatedAt " +
            "WHERE id = :id AND deletedAtEpochMillis IS NULL",
    )
    abstract suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        "UPDATE tasks SET deletedAtEpochMillis = NULL, restoredOnIso = :restoredOnIso, updatedAtEpochMillis = :updatedAt " +
            "WHERE id = :id AND deletedAtEpochMillis IS NOT NULL",
    )
    abstract suspend fun restore(id: String, updatedAt: Long, restoredOnIso: String)

    @Query("DELETE FROM tasks WHERE id = :id AND deletedAtEpochMillis IS NOT NULL")
    abstract suspend fun deleteSealed(id: String)

    @Query("DELETE FROM tasks WHERE deletedAtEpochMillis IS NOT NULL AND deletedAtEpochMillis <= :cutoffMillis")
    abstract suspend fun deleteSealedBefore(cutoffMillis: Long)

    @Query("DELETE FROM tasks WHERE deletedAtEpochMillis IS NOT NULL")
    abstract suspend fun deleteAllSealed()

    private suspend fun rememberPurge(rows: List<TaskEntity>) {
        rows.filter { it.repeatRuleId != null }.forEach {
            saveRepeatException(TaskRepeatExceptionEntity(requireNotNull(it.repeatRuleId), requireNotNull(it.repeatOccurrenceDateIso), true))
        }
    }
    @Transaction
    open suspend fun permanentlyDelete(id: String) {
        rememberPurge(listOfNotNull(findById(id)).filter { it.deletedAtEpochMillis != null })
        deleteSealed(id)
    }
    @Transaction
    open suspend fun purgeDeletedBefore(cutoffMillis: Long) {
        rememberPurge(allTasks().filter { it.deletedAtEpochMillis?.let { date -> date <= cutoffMillis } == true })
        deleteSealedBefore(cutoffMillis)
    }
    @Transaction
    open suspend fun purgeAllDeleted() {
        rememberPurge(allTasks().filter { it.deletedAtEpochMillis != null })
        deleteAllSealed()
    }

    @Query("SELECT * FROM tasks WHERE repeatRuleId = :ruleId AND repeatOccurrenceDateIso = :dateIso LIMIT 1")
    abstract suspend fun findOccurrence(ruleId: String, dateIso: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM task_repeat_rules WHERE ruleId = :ruleId AND effectiveFromIso <= :dateIso")
    abstract suspend fun countRules(ruleId: String, dateIso: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertScheduleChange(change: TaskScheduleChangeEntity)

    @Transaction
    open suspend fun saveWithHistory(task: TaskEntity, changeId: String, reason: String) {
        val previous = findById(task.id)
        val ruleId = task.repeatRuleId
        if (previous != null) {
            require(previous.repeatRuleId == ruleId && previous.repeatOccurrenceDateIso == task.repeatOccurrenceDateIso) { "不能改变实例身份" }
        }
        if (ruleId != null) {
            val date = requireNotNull(task.repeatOccurrenceDateIso)
            require(countRules(ruleId, date) > 0) { "重复规则不存在或尚未生效" }
            val occurrence = findOccurrence(ruleId, date)
            require(occurrence == null || occurrence.id == task.id) { "同日实例已经存在" }
        }
        val preserved = task.copy(
            initialScheduledDateIso = previous?.initialScheduledDateIso ?: previous?.dueLocalDateIso ?: task.initialScheduledDateIso ?: task.dueLocalDateIso,
            createdAtEpochMillis = previous?.createdAtEpochMillis ?: task.createdAtEpochMillis,
            isCompleted = previous?.isCompleted ?: task.isCompleted,
            completedAtEpochMillis = if (previous != null) previous.completedAtEpochMillis else task.completedAtEpochMillis,
            deletedAtEpochMillis = if (previous != null) previous.deletedAtEpochMillis else task.deletedAtEpochMillis,
            isSkipped = previous?.isSkipped ?: task.isSkipped,
            restoredOnIso = if (previous != null) previous.restoredOnIso else task.restoredOnIso,
        )
        upsert(preserved)
        if (ruleId != null) saveRepeatException(TaskRepeatExceptionEntity(ruleId, requireNotNull(task.repeatOccurrenceDateIso), false))
        if (previous != null && previous.dueLocalDateIso != task.dueLocalDateIso) {
            insertScheduleChange(TaskScheduleChangeEntity(changeId, task.id, previous.dueLocalDateIso, task.dueLocalDateIso, task.updatedAtEpochMillis, reason))
        }
    }

    @Query("UPDATE tasks SET groupId = NULL WHERE groupId = :groupId")
    abstract suspend fun clearGroup(groupId: String)
}

/** 当前安排与历史同一次 Room 快照读取，避免旧行与新历史短暂重叠。 */
data class TaskWithScheduleChanges(
    @androidx.room.Embedded val task: TaskEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "taskId") val changes: List<TaskScheduleChangeEntity>,
)
