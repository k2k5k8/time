package com.cch.momentmark.data.repository

import androidx.room.withTransaction
import com.cch.momentmark.data.local.GroupDao
import com.cch.momentmark.data.local.GroupEntity
import com.cch.momentmark.data.local.MomentDao
import com.cch.momentmark.data.local.TaskDao
import com.cch.momentmark.data.local.MomentMarkDatabase
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Group(
    val id: String,
    val name: String,
    val colorToken: String,
    val sortOrder: Int,
)

interface GroupRepositoryPort {
    fun observeAll(): Flow<List<Group>> = kotlinx.coroutines.flow.emptyFlow()
    suspend fun create(name: String): Result<Group> = Result.failure(UnsupportedOperationException())
    suspend fun rename(id: String, name: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun reorder(orderedIds: List<String>): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun dissolve(id: String): Result<Unit> = Result.failure(UnsupportedOperationException())
}

class GroupRepository(
    private val database: MomentMarkDatabase,
    private val groupDao: GroupDao,
    private val momentDao: MomentDao,
    private val taskDao: TaskDao,
    private val nowMillis: () -> Long,
) : GroupRepositoryPort {
    override fun observeAll(): Flow<List<Group>> =
        groupDao.observeAll().map { list -> list.map { Group(it.id, it.name, it.colorToken, it.sortOrder) } }

    override suspend fun create(name: String): Result<Group> = runCatching {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "分组名称不能为空。" }
        require(groupDao.findByNormalizedName(normalized) == null) { "分组名称已存在。" }
        val now = nowMillis()
        val sortOrder = groupDao.nextSortOrder()
        val entity = GroupEntity(
            id = UUID.randomUUID().toString(), name = normalized,
            colorToken = COLOR_TOKENS[sortOrder % COLOR_TOKENS.size],
            sortOrder = sortOrder, createdAt = now, updatedAt = now,
        )
        groupDao.insert(entity)
        Group(entity.id, entity.name, entity.colorToken, entity.sortOrder)
    }

    override suspend fun rename(id: String, name: String): Result<Unit> = runCatching {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "分组名称不能为空。" }
        val existing = groupDao.findByNormalizedName(normalized)
        require(existing == null || existing.id == id) { "分组名称已存在。" }
        require(groupDao.findById(id) != null) { "分组不存在。" }
        groupDao.rename(id, normalized, nowMillis())
    }

    override suspend fun reorder(orderedIds: List<String>): Result<Unit> = runCatching {
        orderedIds.forEachIndexed { index, id -> groupDao.updateSortOrder(id, index, nowMillis()) }
    }

    override suspend fun dissolve(id: String): Result<Unit> = runCatching {
        database.withTransaction {
            require(groupDao.findById(id) != null) { "分组不存在。" }
            momentDao.clearGroup(id)
            taskDao.clearGroup(id)
            groupDao.delete(id)
        }
    }

    companion object {
        val COLOR_TOKENS = listOf("manaPurple", "amber", "hpRed", "goldInk", "xpBlue")
    }
}
