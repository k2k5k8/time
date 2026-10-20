package com.cch.momentmark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema versioning contract: every version bump must include its migration,
 * exported schema JSON, and a MigrationTestHelper path from the previous version.
 */
@Database(
    entities = [MomentEntity::class, TaskEntity::class, GroupEntity::class, TaskRepeatRuleEntity::class, TaskRepeatBreakEntity::class, TaskScheduleChangeEntity::class, TaskRepeatExceptionEntity::class],
    version = 10,
    exportSchema = true,
)
abstract class MomentMarkDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
    abstract fun taskDao(): TaskDao
    abstract fun taskPlanningDao(): TaskPlanningDao
    abstract fun groupDao(): GroupDao

    companion object {
        const val DATABASE_NAME = "moment_mark.db"

        /** Adds organization and recovery metadata to the original event table. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `groupId` TEXT")
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `deletedAt` INTEGER")
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds the shared small-title field without changing existing card visuals. */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `subtitle` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Stores repeat, calendar, reminder, cover and notification settings. */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `time_events` ADD COLUMN `advancedConfigJson` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * J 最终设计：新增独立的 moments 表（PRD 双体系）。
         * 已确认当前库内只有样例/开发数据，不搬运 time_events 数据（ARCHITECTURE §5.1）；
         * 旧表原样保留，待旧 UI 退场时再一并清理。
         */
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `moments` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `creationDirection` TEXT NOT NULL,
                        `anchorDateIso` TEXT NOT NULL,
                        `groupId` TEXT,
                        `rarity` INTEGER,
                        `isPinned` INTEGER NOT NULL,
                        `pinnedOrder` INTEGER,
                        `deletedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** P0 Task 独立建表；不从原型 Daybook 或 TimeEvent 猜测、搬运数据。 */
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `dueLocalDateIso` TEXT NOT NULL,
                        `dueInstantEpochMillis` INTEGER,
                        `zoneId` TEXT,
                        `note` TEXT NOT NULL,
                        `taskType` TEXT,
                        `difficulty` INTEGER,
                        `groupId` TEXT,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAtEpochMillis` INTEGER,
                        `showOnHome` INTEGER NOT NULL,
                        `deletedAtEpochMillis` INTEGER,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * J 最终设计不再保留旧 TimeEvent / 模板卡片体系。
         * 已确认只有样例与开发数据，因此只移除遗留表；Moment 与 Task 表不受影响。
         */
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `time_events`")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `groups` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorToken` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** DB-09：仅增列/增表，旧任务保持独立单次语义与原始截止。 */
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `planPeriod` TEXT NOT NULL DEFAULT 'ANYTIME'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `planTime` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `important` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `repeatRuleId` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `repeatOccurrenceDateIso` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `isSkipped` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `initialScheduledDateIso` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `autoDefer` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `restoredOnIso` TEXT")
                db.execSQL("UPDATE tasks SET initialScheduledDateIso = dueLocalDateIso")
                db.execSQL("CREATE UNIQUE INDEX `index_tasks_repeatRuleId_repeatOccurrenceDateIso` ON `tasks` (`repeatRuleId`, `repeatOccurrenceDateIso`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `task_repeat_rules` (`ruleId` TEXT NOT NULL, `effectiveFromIso` TEXT NOT NULL, `startDateIso` TEXT NOT NULL, `endDateIso` TEXT, `weekdaysMask` INTEGER NOT NULL, `zoneId` TEXT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `planPeriod` TEXT NOT NULL, `planTime` TEXT, `deadlineTime` TEXT, `important` INTEGER NOT NULL, `difficulty` INTEGER, `taskType` TEXT, `groupId` TEXT, `showOnHome` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`ruleId`, `effectiveFromIso`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `task_repeat_breaks` (`ruleId` TEXT NOT NULL, `startDateIso` TEXT NOT NULL, `endExclusiveIso` TEXT, `kind` TEXT NOT NULL, PRIMARY KEY(`ruleId`, `startDateIso`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `task_schedule_changes` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `fromDateIso` TEXT NOT NULL, `toDateIso` TEXT NOT NULL, `changedAtEpochMillis` INTEGER NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX `index_task_schedule_changes_taskId` ON `task_schedule_changes` (`taskId`)")
            }
        }

        /** DB-10：持久化单日例外/净化墓碑；既有任务与规则原样保留。 */
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `task_repeat_exceptions` (`ruleId` TEXT NOT NULL, `dateIso` TEXT NOT NULL, `purged` INTEGER NOT NULL, PRIMARY KEY(`ruleId`, `dateIso`))")
                // 既有实例可能已经逐日编辑，保守保留为单日例外。
                db.execSQL("INSERT INTO task_repeat_exceptions SELECT repeatRuleId, repeatOccurrenceDateIso, 0 FROM tasks WHERE repeatRuleId IS NOT NULL")
            }
        }

        fun create(context: Context): MomentMarkDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MomentMarkDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(MIGRATION_9_10)
                .build()
    }
}
