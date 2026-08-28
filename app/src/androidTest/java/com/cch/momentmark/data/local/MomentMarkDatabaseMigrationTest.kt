package com.cch.momentmark.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.sqlite.db.SupportSQLiteDatabase

@RunWith(AndroidJUnit4::class)
class MomentMarkDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MomentMarkDatabase::class.java,
    )

    @Before
    fun cleanDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun closeDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migration1To4PreservesLegacyRecordAndValidatesTargetSchema() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            createV1Fixture(this)
            insertLegacyRecord(
                database = this,
                subtitle = null,
                groupId = null,
                sortOrder = null,
                createdAt = null,
                updatedAt = null,
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            MomentMarkDatabase.MIGRATION_1_2,
            MomentMarkDatabase.MIGRATION_2_3,
            MomentMarkDatabase.MIGRATION_3_4,
        )

        assertV4Columns(database)
        assertLegacyRecord(
            database = database,
            expectedSubtitle = "",
            expectedGroupId = null,
            expectedSortOrder = 0,
            expectedCreatedAt = 0,
            expectedUpdatedAt = 0,
        )
        database.close()
    }

    @Test
    fun migration2To4PreservesExistingValuesAndAddsNewColumns() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            createV2Fixture(this)
            insertLegacyRecord(
                database = this,
                subtitle = null,
                groupId = "travel",
                sortOrder = 7,
                createdAt = 101,
                updatedAt = 202,
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            MomentMarkDatabase.MIGRATION_2_3,
            MomentMarkDatabase.MIGRATION_3_4,
        )

        assertV4Columns(database)
        assertLegacyRecord(
            database = database,
            expectedSubtitle = "",
            expectedGroupId = "travel",
            expectedSortOrder = 7,
            expectedCreatedAt = 101,
            expectedUpdatedAt = 202,
        )
        database.close()
    }

    @Test
    fun migration3To4PreservesSubtitleAndExistingValues() {
        helper.createDatabase(TEST_DATABASE, 3).apply {
            createV3Fixture(this)
            insertLegacyRecord(
                database = this,
                subtitle = "纪念日",
                groupId = "personal",
                sortOrder = 3,
                createdAt = 303,
                updatedAt = 404,
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            MomentMarkDatabase.MIGRATION_3_4,
        )

        assertV4Columns(database)
        assertLegacyRecord(
            database = database,
            expectedSubtitle = "纪念日",
            expectedGroupId = "personal",
            expectedSortOrder = 3,
            expectedCreatedAt = 303,
            expectedUpdatedAt = 404,
        )
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
            repository.save(event.toDomain().copy(title = "Renamed mutation"))
            val edited = repository.findById("mutation")!!
            assertEquals("Renamed mutation", edited.title)
            assertEquals(event.sortOrder, edited.sortOrder)
            assertEquals(event.createdAt, edited.createdAt)
            assertEquals(event.deletedAt, edited.deletedAt)

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

    @Test
    fun deletedEventsCanBeRestoredAndReturnToActiveFeed() {
        runBlocking {
            val database = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                "recycle-restore-test.db",
            ).build()
            try {
                val repository = TimeEventRepository(database.timeEventDao())
                repository.save(testEntity("restore"))
                repository.softDelete("restore", deletedAt = 10, updatedAt = 10)

                assertTrue(repository.observeActive().first().isEmpty())
                assertEquals(listOf("restore"), repository.observeDeleted().first().map { it.id })

                repository.restoreDeleted("restore", updatedAt = 11)

                assertTrue(repository.observeDeleted().first().isEmpty())
                assertEquals(listOf("restore"), repository.observeActive().first().map { it.id })
            } finally {
                database.close()
                context.deleteDatabase("recycle-restore-test.db")
            }
        }
    }

    @Test
    fun permanentlyDeletingOneEventKeepsOtherDeletedEvents() {
        runBlocking {
            val database = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                "recycle-permanent-test.db",
            ).build()
            try {
                val repository = TimeEventRepository(database.timeEventDao())
                repository.saveAll(listOf(testEntity("keep"), testEntity("remove")))
                repository.softDelete("keep", deletedAt = 20, updatedAt = 20)
                repository.softDelete("remove", deletedAt = 21, updatedAt = 21)

                repository.permanentlyDelete("remove")

                assertEquals(listOf("keep"), repository.observeDeleted().first().map { it.id })
                assertTrue(repository.findById("remove") == null)
            } finally {
                database.close()
                context.deleteDatabase("recycle-permanent-test.db")
            }
        }
    }

    @Test
    fun purgingDeletedEventsLeavesAnEmptyRecycleBin() {
        runBlocking {
            val database = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                "recycle-purge-test.db",
            ).build()
            try {
                val repository = TimeEventRepository(database.timeEventDao())
                assertTrue(repository.observeDeleted().first().isEmpty())

                repository.save(testEntity("purge"))
                repository.softDelete("purge", deletedAt = 30, updatedAt = 30)
                assertFalse(repository.observeDeleted().first().isEmpty())

                repository.purgeDeleted()

                assertTrue(repository.observeDeleted().first().isEmpty())
                assertTrue(repository.findById("purge") == null)
            } finally {
                database.close()
                context.deleteDatabase("recycle-purge-test.db")
            }
        }
    }

    @Test
    fun deletedEventsRemainVisibleAfterDatabaseReopen() {
        val databaseName = "recycle-restart-test.db"
        runBlocking {
            val firstDatabase = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                databaseName,
            ).build()
            try {
                val repository = TimeEventRepository(firstDatabase.timeEventDao())
                repository.save(testEntity("restart"))
                repository.softDelete("restart", deletedAt = 40, updatedAt = 40)
            } finally {
                firstDatabase.close()
            }

            val reopenedDatabase = Room.databaseBuilder(
                context,
                MomentMarkDatabase::class.java,
                databaseName,
            ).build()
            try {
                val repository = TimeEventRepository(reopenedDatabase.timeEventDao())
                assertEquals(listOf("restart"), repository.observeDeleted().first().map { it.id })
            } finally {
                reopenedDatabase.close()
                context.deleteDatabase(databaseName)
            }
        }
    }

    private fun testEntity(id: String) = TimeEventEntity(
        id = id,
        title = id,
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

    /** Keep the historical v1 table shape explicit even though its schema is also exported for the helper. */
    private fun createV1Fixture(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `time_events`")
        database.execSQL(
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
    }

    private fun createV2Fixture(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `time_events`")
        database.execSQL(
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
                `groupId` TEXT,
                `isPinned` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }

    private fun createV3Fixture(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `time_events`")
        database.execSQL(
            """
            CREATE TABLE `time_events` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `subtitle` TEXT NOT NULL,
                `timeType` TEXT NOT NULL,
                `localDateIso` TEXT,
                `instantEpochMillis` INTEGER,
                `zoneId` TEXT,
                `note` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `paletteKey` TEXT NOT NULL,
                `templateKey` TEXT NOT NULL,
                `templateConfigJson` TEXT NOT NULL,
                `groupId` TEXT,
                `isPinned` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }

    private fun insertLegacyRecord(
        database: SupportSQLiteDatabase,
        subtitle: String?,
        groupId: String?,
        sortOrder: Long?,
        createdAt: Long?,
        updatedAt: Long?,
    ) {
        val values = ContentValues().apply {
            put("id", "legacy-event")
            put("title", "Legacy title")
            put("timeType", "ALL_DAY")
            put("localDateIso", "2026-08-20")
            putNull("instantEpochMillis")
            putNull("zoneId")
            put("note", "legacy note")
            put("iconKey", "calendar")
            put("paletteKey", "BLUE_WHITE")
            put("templateKey", "CLASSIC")
            put("templateConfigJson", "legacy-config")
            subtitle?.let { put("subtitle", it) }
            groupId?.let { put("groupId", it) }
            put("isPinned", 1)
            put("isArchived", 0)
            sortOrder?.let { put("sortOrder", it) }
            createdAt?.let { put("createdAt", it) }
            updatedAt?.let { put("updatedAt", it) }
        }
        assertTrue(database.insert("time_events", 0, values) != -1L)
    }

    private fun assertV4Columns(database: SupportSQLiteDatabase) {
        val expected = setOf(
            "id",
            "title",
            "subtitle",
            "timeType",
            "localDateIso",
            "instantEpochMillis",
            "zoneId",
            "note",
            "iconKey",
            "paletteKey",
            "templateKey",
            "templateConfigJson",
            "advancedConfigJson",
            "groupId",
            "isPinned",
            "isArchived",
            "sortOrder",
            "deletedAt",
            "createdAt",
            "updatedAt",
        )
        database.query("PRAGMA table_info(`time_events`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val actual = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
            assertEquals(expected, actual)
        }
    }

    private fun assertLegacyRecord(
        database: SupportSQLiteDatabase,
        expectedSubtitle: String,
        expectedGroupId: String?,
        expectedSortOrder: Long,
        expectedCreatedAt: Long,
        expectedUpdatedAt: Long,
    ) {
        database.query(
            """
            SELECT id, title, subtitle, groupId, sortOrder, createdAt, updatedAt, advancedConfigJson,
                   isPinned, isArchived
            FROM time_events WHERE id = ?
            """.trimIndent(),
            arrayOf("legacy-event"),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("legacy-event", cursor.getString(0))
            assertEquals("Legacy title", cursor.getString(1))
            assertEquals(expectedSubtitle, cursor.getString(2))
            assertEquals(expectedGroupId, cursor.getString(3))
            assertEquals(expectedSortOrder, cursor.getLong(4))
            assertEquals(expectedCreatedAt, cursor.getLong(5))
            assertEquals(expectedUpdatedAt, cursor.getLong(6))
            assertEquals("", cursor.getString(7))
            assertEquals(1, cursor.getInt(8))
            assertEquals(0, cursor.getInt(9))
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
