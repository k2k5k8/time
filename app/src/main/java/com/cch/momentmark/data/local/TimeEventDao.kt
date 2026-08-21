package com.cch.momentmark.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEventDao {
    @Query("SELECT * FROM time_events WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY isPinned DESC, sortOrder ASC, createdAt ASC")
    fun observeActive(): Flow<List<TimeEventEntity>>

    @Query("SELECT * FROM time_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TimeEventEntity?

    @Query("SELECT COUNT(*) FROM time_events")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: TimeEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<TimeEventEntity>)

    @Query("UPDATE time_events SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("UPDATE time_events SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE time_events SET groupId = :newGroup, updatedAt = :updatedAt WHERE groupId = :oldGroup")
    suspend fun renameGroup(oldGroup: String, newGroup: String, updatedAt: Long)

    @Query("UPDATE time_events SET groupId = NULL, updatedAt = :updatedAt WHERE groupId = :group")
    suspend fun clearGroup(group: String, updatedAt: Long)

    @Query("UPDATE time_events SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE time_events SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreDeleted(id: String, updatedAt: Long)

    @Query("DELETE FROM time_events WHERE deletedAt IS NOT NULL")
    suspend fun purgeDeleted()
}
