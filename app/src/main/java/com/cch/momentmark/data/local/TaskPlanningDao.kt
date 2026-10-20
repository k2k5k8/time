package com.cch.momentmark.data.local

import androidx.room.Upsert
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskPlanningDao {
    @Query("SELECT * FROM task_repeat_rules ORDER BY ruleId, effectiveFromIso")
    abstract fun observeRules(): Flow<List<TaskRepeatRuleEntity>>
    @Query("SELECT * FROM task_repeat_rules ORDER BY ruleId, effectiveFromIso")
    abstract suspend fun allRules(): List<TaskRepeatRuleEntity>
    @Query("SELECT * FROM task_repeat_breaks ORDER BY startDateIso")
    abstract fun observeAllBreaks(): Flow<List<TaskRepeatBreakEntity>>
    @Query("SELECT * FROM task_repeat_breaks ORDER BY startDateIso")
    abstract suspend fun allBreaks(): List<TaskRepeatBreakEntity>
    @Query("SELECT * FROM task_repeat_exceptions")
    abstract fun observeExceptions(): Flow<List<TaskRepeatExceptionEntity>>
    @Query("SELECT * FROM task_repeat_exceptions")
    abstract suspend fun allExceptions(): List<TaskRepeatExceptionEntity>
    @Upsert
    abstract suspend fun saveException(value: TaskRepeatExceptionEntity)
    @Query("DELETE FROM task_repeat_rules WHERE ruleId = :id AND effectiveFromIso >= :dateIso")
    abstract suspend fun deleteFutureVersions(id: String, dateIso: String)
    @Query("DELETE FROM task_repeat_breaks WHERE ruleId = :id AND startDateIso = :dateIso")
    abstract suspend fun deleteBreak(id: String, dateIso: String)

    @Query("SELECT * FROM task_repeat_rules WHERE ruleId = :id ORDER BY effectiveFromIso")
    abstract fun observeRuleVersions(id: String): Flow<List<TaskRepeatRuleEntity>>

    @Query("SELECT * FROM task_repeat_rules WHERE ruleId = :id AND effectiveFromIso <= :dateIso ORDER BY effectiveFromIso DESC LIMIT 1")
    abstract suspend fun findRule(id: String, dateIso: String): TaskRepeatRuleEntity?

    @Upsert
    abstract suspend fun insertRule(rule: TaskRepeatRuleEntity)

    @Query("SELECT * FROM task_repeat_breaks WHERE ruleId = :id ORDER BY startDateIso")
    abstract fun observeBreaks(id: String): Flow<List<TaskRepeatBreakEntity>>

    @Query("SELECT * FROM task_repeat_breaks WHERE ruleId = :id AND startDateIso = :startIso LIMIT 1")
    abstract suspend fun findBreak(id: String, startIso: String): TaskRepeatBreakEntity?

    @Upsert
    abstract suspend fun reopenBreak(value: TaskRepeatBreakEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertBreak(value: TaskRepeatBreakEntity)

    @Query("UPDATE task_repeat_breaks SET endExclusiveIso = :endIso WHERE ruleId = :id AND startDateIso = :startIso AND endExclusiveIso IS NULL")
    abstract suspend fun closeBreak(id: String, startIso: String, endIso: String)

    @Query("SELECT * FROM task_schedule_changes WHERE taskId = :id ORDER BY changedAtEpochMillis, id")
    abstract fun observeScheduleChanges(id: String): Flow<List<TaskScheduleChangeEntity>>

    @Query("SELECT * FROM task_repeat_rules WHERE ruleId = :id LIMIT 1")
    abstract suspend fun anyRule(id: String): TaskRepeatRuleEntity?

    @Transaction
    open suspend fun saveRuleVersion(rule: TaskRepeatRuleEntity) {
        val original = anyRule(rule.ruleId)
        require(original == null || original.startDateIso == rule.startDateIso) { "系列初始日期不可修改" }
        // 不同生效日保留独立版本；同日设置允许再次编辑。
        insertRule(rule.copy(createdAtEpochMillis = original?.createdAtEpochMillis ?: rule.createdAtEpochMillis))
    }

    @Transaction
    open suspend fun saveBreak(value: TaskRepeatBreakEntity) {
        require(anyRule(value.ruleId) != null) { "重复规则不存在" }
        val previous = findBreak(value.ruleId, value.startDateIso)
        if (previous == null) insertBreak(value)
        else {
            require(previous.kind == value.kind && previous.endExclusiveIso == null && value.endExclusiveIso != null)
            closeBreak(value.ruleId, value.startDateIso, requireNotNull(value.endExclusiveIso))
        }
    }
}
