package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TimeEventDao
import com.cch.momentmark.data.local.TimeEventEntity
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.data.repository.TimeEventMapper.toEntity
import kotlinx.coroutines.flow.Flow

/**
 * Stage 3 data boundary. UI integration and user-facing CRUD remain in the
 * next stage; this class keeps persistence rules out of Compose.
 */
class TimeEventRepository(
    private val dao: TimeEventDao,
) {
    fun observeActive(): Flow<List<TimeEventEntity>> = dao.observeActive()

    suspend fun findById(id: String): TimeEventEntity? = dao.findById(id)

    suspend fun seedIfEmpty(events: List<TimeEvent>) {
        if (dao.countAll() == 0) {
            val now = System.currentTimeMillis()
            saveAll(events.mapIndexed { index, event ->
                event.toEntity(now).copy(sortOrder = index.toLong())
            })
        }
    }

    suspend fun save(event: TimeEvent) = save(event.toEntity())

    suspend fun save(event: TimeEventEntity) {
        validateTimeShape(event)
        dao.upsert(event)
    }

    suspend fun saveAll(events: List<TimeEventEntity>) {
        events.forEach(::validateTimeShape)
        dao.upsertAll(events)
    }

    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long) =
        dao.setArchived(id, archived, updatedAt)

    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long) =
        dao.setPinned(id, pinned, updatedAt)

    suspend fun renameGroup(oldGroup: String, newGroup: String, updatedAt: Long) =
        dao.renameGroup(oldGroup, newGroup, updatedAt)

    suspend fun clearGroup(group: String, updatedAt: Long) =
        dao.clearGroup(group, updatedAt)

    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long) =
        dao.softDelete(id, deletedAt, updatedAt)

    suspend fun restoreDeleted(id: String, updatedAt: Long) =
        dao.restoreDeleted(id, updatedAt)

    suspend fun purgeDeleted() = dao.purgeDeleted()

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
