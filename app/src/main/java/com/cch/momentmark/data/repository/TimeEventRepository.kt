package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TimeEventDao
import com.cch.momentmark.data.local.TimeEventEntity
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.data.repository.TimeEventMapper.toEntity
import kotlinx.coroutines.flow.Flow

/** Keeps persistence rules and the recovery-bin lifecycle out of Compose. */
interface TimeEventRepositoryPort {
    fun observeActive(): Flow<List<TimeEventEntity>>
    fun observeDeleted(): Flow<List<TimeEventEntity>>
    suspend fun seedIfEmpty(events: List<TimeEvent>)
    suspend fun save(event: TimeEvent)
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)
    suspend fun renameGroup(oldGroup: String, newGroup: String, updatedAt: Long)
    suspend fun clearGroup(group: String, updatedAt: Long)
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
    suspend fun restoreDeleted(id: String, updatedAt: Long)
    suspend fun permanentlyDelete(id: String)
    suspend fun purgeDeleted()
}

class TimeEventRepository(
    private val dao: TimeEventDao,
) : TimeEventRepositoryPort {
    override fun observeActive(): Flow<List<TimeEventEntity>> = dao.observeActive()

    override fun observeDeleted(): Flow<List<TimeEventEntity>> = dao.observeDeleted()

    suspend fun findById(id: String): TimeEventEntity? = dao.findById(id)

    override suspend fun seedIfEmpty(events: List<TimeEvent>) {
        if (dao.countAll() == 0) {
            val now = System.currentTimeMillis()
            saveAll(events.mapIndexed { index, event ->
                event.toEntity(now).copy(sortOrder = index.toLong())
            })
        }
    }

    /**
     * A user edit changes event facts, not its board position or lifecycle.
     * Preserve those fields from the stored row so REPLACE cannot silently
     * move a card or revive a deleted event.
     */
    override suspend fun save(event: TimeEvent) {
        val now = System.currentTimeMillis()
        save(event.toEntity(nowMillis = now, previous = dao.findById(event.id)))
    }

    suspend fun save(event: TimeEventEntity) {
        validateTimeShape(event)
        dao.upsert(event)
    }

    suspend fun saveAll(events: List<TimeEventEntity>) {
        events.forEach(::validateTimeShape)
        dao.upsertAll(events)
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long) =
        dao.setArchived(id, archived, updatedAt)

    override suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long) =
        dao.setPinned(id, pinned, updatedAt)

    override suspend fun renameGroup(oldGroup: String, newGroup: String, updatedAt: Long) =
        dao.renameGroup(oldGroup, newGroup, updatedAt)

    override suspend fun clearGroup(group: String, updatedAt: Long) =
        dao.clearGroup(group, updatedAt)

    override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long) =
        dao.softDelete(id, deletedAt, updatedAt)

    override suspend fun restoreDeleted(id: String, updatedAt: Long) =
        dao.restoreDeleted(id, updatedAt)

    override suspend fun permanentlyDelete(id: String) = dao.permanentlyDelete(id)

    override suspend fun purgeDeleted() = dao.purgeDeleted()

    private fun validateTimeShape(event: TimeEventEntity) {
        when (event.timeType) {
            "ALL_DAY" -> {
                require(!event.localDateIso.isNullOrBlank()) { "ALL_DAY requires localDateIso" }
                require(event.instantEpochMillis == null) { "ALL_DAY cannot contain instantEpochMillis" }
                require(event.zoneId == null) { "ALL_DAY cannot contain zoneId" }
            }

            "TIMED" -> {
                require(event.localDateIso == null) { "TIMED cannot contain localDateIso" }
                require(event.instantEpochMillis != null) { "TIMED requires instantEpochMillis" }
                require(!event.zoneId.isNullOrBlank()) { "TIMED requires zoneId" }
            }

            else -> error("Unknown timeType: ${event.timeType}")
        }
    }
}
