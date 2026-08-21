package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TimeEventEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeEventMapperTest {
    @Test
    fun persistenceMetadataPreservesStoredOrderAndLifecycleMetadata() {
        val previous = TimeEventEntity(
            id = "exam",
            title = "旧标题",
            subtitle = "",
            timeType = "ALL_DAY",
            localDateIso = "2026-12-26",
            instantEpochMillis = null,
            zoneId = null,
            note = "",
            iconKey = "calendar",
            paletteKey = "BLUE_WHITE",
            templateKey = "CLASSIC",
            templateConfigJson = "",
            advancedConfigJson = "",
            groupId = null,
            isPinned = false,
            isArchived = false,
            sortOrder = 42,
            deletedAt = 700L,
            createdAt = 100L,
            updatedAt = 200L,
        )
        val metadata = TimeEventMapper.persistenceMetadata(previous, nowMillis = 999L)

        assertEquals(42L, metadata.sortOrder)
        assertEquals(700L, metadata.deletedAt)
        assertEquals(100L, metadata.createdAt)
        assertEquals(999L, metadata.updatedAt)
    }
}
