package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TaskEntity
import com.cch.momentmark.domain.model.TaskType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMapperTest {
    @Test
    fun explicitTaskColumnsRoundTripWithoutDerivedDisplayState() {
        val entity = TaskEntity(
            id = "task-1", title = "买礼物", dueLocalDateIso = "2026-08-30",
            dueInstantEpochMillis = Instant.parse("2026-08-30T10:00:00Z").toEpochMilli(), zoneId = "UTC",
            note = "LORE", taskType = "LIMITED", difficulty = 2, groupId = "生活",
            isCompleted = false, completedAtEpochMillis = null, showOnHome = false,
            deletedAtEpochMillis = null, createdAtEpochMillis = 100L, updatedAtEpochMillis = 200L,
        )

        val task = TaskMapper.toDomain(entity)

        assertEquals(TaskType.LIMITED, task.taskType)
        assertEquals("2026-08-30", task.dueLocalDate.toString())
        assertEquals("UTC", task.zoneId?.id)
        assertFalse(task.showOnHome)
        assertNull(task.completedAt)
    }
}
