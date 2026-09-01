package com.cch.momentmark.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE lower(trim(name)) = lower(trim(:name)) LIMIT 1")
    suspend fun findByNormalizedName(name: String): GroupEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM groups")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: GroupEntity)

    @Query("UPDATE groups SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE groups SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Long)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)
}
