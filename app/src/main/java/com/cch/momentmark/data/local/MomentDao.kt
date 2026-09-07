package com.cch.momentmark.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments WHERE deletedAt IS NULL")
    fun observeActive(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MomentEntity?

    @Query("SELECT COUNT(*) FROM moments")
    suspend fun countAll(): Int

    @Query("SELECT MAX(pinnedOrder) FROM moments WHERE deletedAt IS NULL AND isPinned = 1")
    suspend fun maxPinnedOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(moment: MomentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(moments: List<MomentEntity>)

    @Query(
        "UPDATE moments SET isPinned = :pinned, pinnedOrder = :pinnedOrder, updatedAt = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun setPinned(id: String, pinned: Boolean, pinnedOrder: Int?, updatedAt: Long)

    @Query(
        "UPDATE moments SET isPinned = 1, pinnedOrder = :pinnedOrder, updatedAt = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun setPinnedOrder(id: String, pinnedOrder: Int, updatedAt: Long)

    @Query("UPDATE moments SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE moments SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM moments WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun permanentlyDelete(id: String)

    @Query("DELETE FROM moments WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoffMillis")
    suspend fun purgeDeletedBefore(cutoffMillis: Long)

    @Query("DELETE FROM moments WHERE deletedAt IS NOT NULL")
    suspend fun purgeAllDeleted()

    @Query("UPDATE moments SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: String)
}
