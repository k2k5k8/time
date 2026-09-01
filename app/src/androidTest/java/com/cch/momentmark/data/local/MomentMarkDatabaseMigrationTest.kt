package com.cch.momentmark.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the explicit legacy-table exit without destructive fallback migration. */
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
    fun migration1To7RetiresLegacyTableAndValidatesCurrentSchema() {
        helper.createDatabase(TEST_DATABASE, 1).close()

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            MomentMarkDatabase.MIGRATION_1_2,
            MomentMarkDatabase.MIGRATION_2_3,
            MomentMarkDatabase.MIGRATION_3_4,
            MomentMarkDatabase.MIGRATION_4_5,
            MomentMarkDatabase.MIGRATION_5_6,
            MomentMarkDatabase.MIGRATION_6_7,
        )

        assertCurrentTables(database)
        database.close()
    }

    @Test
    fun migration6To7KeepsMomentAndTaskFactsWhileDroppingOnlyLegacyEvents() {
        helper.createDatabase(TEST_DATABASE, 6).apply {
            execSQL(
                """
                INSERT INTO moments (id, title, note, creationDirection, anchorDateIso, groupId, rarity,
                    isPinned, pinnedOrder, deletedAt, createdAt, updatedAt)
                VALUES ('m-1', '春节', 'LORE', 'FUTURE_COUNTDOWN', '2027-02-06', '家庭', 2, 1, 0, NULL, 100, 100)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO tasks (id, title, dueLocalDateIso, dueInstantEpochMillis, zoneId, note, taskType,
                    difficulty, groupId, isCompleted, completedAtEpochMillis, showOnHome, deletedAtEpochMillis,
                    createdAtEpochMillis, updatedAtEpochMillis)
                VALUES ('t-1', '提交周报', '2026-08-29', NULL, NULL, '', 'SIDE', NULL, NULL, 0, NULL, 1, NULL, 10, 10)
                """.trimIndent(),
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            MomentMarkDatabase.MIGRATION_6_7,
        )

        database.query("SELECT title, isPinned FROM moments WHERE id = 'm-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("春节", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        database.query("SELECT title, showOnHome FROM tasks WHERE id = 't-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("提交周报", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        assertCurrentTables(database)
        database.close()
    }

    @Test
    fun migration7To8AddsGroupsWithoutChangingMomentOrTaskFacts() {
        helper.createDatabase(TEST_DATABASE, 7).apply {
            execSQL("INSERT INTO moments (id,title,note,creationDirection,anchorDateIso,groupId,rarity,isPinned,pinnedOrder,deletedAt,createdAt,updatedAt) VALUES ('m','M','','FUTURE_COUNTDOWN','2026-09-01','legacy-name',NULL,0,NULL,NULL,1,1)")
            close()
        }
        val database = helper.runMigrationsAndValidate(TEST_DATABASE, 8, true, MomentMarkDatabase.MIGRATION_7_8)
        assertTrue(database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='groups'").use { it.moveToFirst() })
        database.query("SELECT groupId FROM moments WHERE id='m'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("legacy-name", cursor.getString(0))
        }
        database.close()
    }

    private fun assertCurrentTables(database: SupportSQLiteDatabase) {
        fun hasTable(name: String): Boolean = database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$name'",
        ).use { it.moveToFirst() }

        assertTrue(hasTable("moments"))
        assertTrue(hasTable("tasks"))
        assertFalse(hasTable("time_events"))
    }

    private companion object {
        const val TEST_DATABASE = "moment-mark-migration-test.db"
    }
}
