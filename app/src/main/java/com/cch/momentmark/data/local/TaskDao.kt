package com.cch.momentmark.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAtEpochMillis IS NULL")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAtEpochMillis IS NOT NULL ORDER BY deletedAtEpochMillis DESC")
    fun observeDeleted(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE deletedAtEpochMillis IS NULL AND dueLocalDateIso = :dateIso " +
            "ORDER BY isCompleted ASC, dueInstantEpochMillis IS NULL ASC, dueInstantEpochMillis ASC, createdAtEpochMillis ASC",
    )
    fun observeForDate(dateIso: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query(
        "UPDATE tasks SET isCompleted = :completed, completedAtEpochMillis = :completedAt, " +
            "updatedAtEpochMillis = :updatedAt WHERE id = :id AND deletedAtEpochMillis IS NULL",
    )
    suspend fun setCompleted(id: String, completed: Boolean, completedAt: Long?, updatedAt: Long)

    @Query(
        "UPDATE tasks SET deletedAtEpochMillis = :deletedAt, updatedAtEpochMillis = :updatedAt " +
            "WHERE id = :id AND deletedAtEpochMillis IS NULL",
    )
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        "UPDATE tasks SET deletedAtEpochMillis = NULL, updatedAtEpochMillis = :updatedAt " +
            "WHERE id = :id AND deletedAtEpochMillis IS NOT NULL",
    )
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id = :id AND deletedAtEpochMillis IS NOT NULL")
    suspend fun permanentlyDelete(id: String)

    @Query("DELETE FROM tasks WHERE deletedAtEpochMillis IS NOT NULL AND deletedAtEpochMillis <= :cutoffMillis")
    suspend fun purgeDeletedBefore(cutoffMillis: Long)

    @Query("DELETE FROM tasks WHERE deletedAtEpochMillis IS NOT NULL")
    suspend fun purgeAllDeleted()

    @Query("UPDATE tasks SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: String)
}
