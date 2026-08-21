package com.cch.momentmark.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.data.repository.TimeEventRepository
import com.cch.momentmark.data.repository.TimeEventMapper.toDomain
import com.cch.momentmark.data.repository.TimeEventMapper.toEntity
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardConfig
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentMarkDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun cleanDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun closeDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migration1To4AddsPersistenceMetadataColumns() {
        val legacy = context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null)
        legacy.execSQL(
            """
            CREATE TABLE `time_events` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `timeType` TEXT NOT NULL,
                `localDateIso` TEXT,
                `instantEpochMillis` INTEGER,
                `zoneId` TEXT,
                `note` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `paletteKey` TEXT NOT NULL,
                `templateKey` TEXT NOT NULL,
                `templateConfigJson` TEXT NOT NULL,
                `isPinned` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        legacy.version = 1
        legacy.close()

        val database = Room.databaseBuilder(
            context,
            MomentMarkDatabase::class.java,
            TEST_DATABASE,
        ).addMigrations(
            MomentMarkDatabase.MIGRATION_1_2,
            MomentMarkDatabase.MIGRATION_2_3,
            MomentMarkDatabase.MIGRATION_3_4,
        ).build()
        val cursor = database.openHelper.writableDatabase.query("PRAGMA table_info(`time_events`)")
        val columns = buildSet {
            cursor.use {
                val nameIndex = it.getColumnIndexOrThrow("name")
                while (it.moveToNext()) add(it.getString(nameIndex))
            }
        }

        assertTrue(columns.contains("groupId"))
        assertTrue(columns.contains("sortOrder"))
        assertTrue(columns.contains("deletedAt"))
        assertTrue(columns.contains("createdAt"))
        assertTrue(columns.contains("updatedAt"))
        assertTrue(columns.contains("subtitle"))
        assertTrue(columns.contains("advancedConfigJson"))
        database.close()
    }

    @Test
    fun repositoryRejectsAnInvalidTimeShape() {
        runBlocking {
            val database = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                "repository-test.db",
            ).build()
            val repository = TimeEventRepository(database.timeEventDao())
            val invalid = TimeEventEntity(
                id = "invalid",
                title = "Invalid",
                subtitle = "",
                timeType = "ALL_DAY",
                localDateIso = "2026-08-19",
                instantEpochMillis = Instant.parse("2026-08-19T00:00:00Z").toEpochMilli(),
                zoneId = null,
                note = "",
                iconKey = "calendar",
                paletteKey = "BLUE_WHITE",
                templateKey = "CLASSIC",
                templateConfigJson = "{\"schemaVersion\":1}",
                advancedConfigJson = "",
                groupId = null,
                isPinned = false,
                isArchived = false,
                sortOrder = 0,
                deletedAt = null,
                createdAt = 0,
                updatedAt = 0,
            )

            val error = runCatching { repository.save(invalid) }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)

            val valid = invalid.copy(
                id = "valid",
                title = "Valid",
                localDateIso = "2026-08-19",
                instantEpochMillis = null,
            )
            repository.save(valid)
            assertEquals(valid, repository.findById("valid"))

            database.close()
            context.deleteDatabase("repository-test.db")
        }
    }

    @Test
    fun domainEventRoundTripsThroughEntityMapper() {
        val event = TimeEvent(
            id = "mapper",
            title = "东京旅行",
            subtitle = "纪念日",
            groupLabel = "旅行与生活",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "",
            relativeLabel = "",
            icon = "✈",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            cardTemplateKey = EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL,
            localDate = LocalDate.of(2026, 9, 16),
            travelCardConfig = TravelCardConfig(
                title = "东京旅行",
                groupLabel = "旅行与生活",
                targetDate = LocalDate.of(2026, 9, 16),
            ),
        )

        val restored = event.toEntity(nowMillis = 123L).toDomain(
            Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC),
        )

        assertEquals(event.id, restored.id)
        assertEquals(event.title, restored.title)
        assertEquals(event.timeType, restored.timeType)
        assertEquals(event.localDate, restored.localDate)
        assertEquals(event.subtitle, restored.subtitle)
        assertEquals(event.groupLabel, restored.groupLabel)
        assertEquals(event.cardTemplateKey, restored.cardTemplateKey)
        assertEquals(event.travelCardConfig, restored.travelCardConfig)
    }

    @Test
    fun repositoryMutationsKeepArchivedAndDeletedEventsOutOfActiveFeed() {
        runBlocking {
            val database = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                "mutation-test.db",
            ).build()
            val repository = TimeEventRepository(database.timeEventDao())
            val event = TimeEventEntity(
                id = "mutation",
                title = "Mutation",
                subtitle = "",
                timeType = "ALL_DAY",
                localDateIso = "2026-08-19",
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
                sortOrder = 0,
                deletedAt = null,
                createdAt = 1,
                updatedAt = 1,
            )
            repository.save(event)
            repository.setPinned("mutation", true, 2)
            assertTrue(repository.findById("mutation")!!.isPinned)

            repository.setArchived("mutation", true, 3)
            assertTrue(repository.observeActive().first().isEmpty())
            repository.setArchived("mutation", false, 4)
            repository.softDelete("mutation", 5, 5)
            assertTrue(repository.observeActive().first().isEmpty())
            repository.restoreDeleted("mutation", 6)
            assertEquals(1, repository.observeActive().first().size)

            database.close()
            context.deleteDatabase("mutation-test.db")
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
