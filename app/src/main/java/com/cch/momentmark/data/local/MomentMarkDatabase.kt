package com.cch.momentmark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TimeEventEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class MomentMarkDatabase : RoomDatabase() {
    abstract fun timeEventDao(): TimeEventDao

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

        fun create(context: Context): MomentMarkDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MomentMarkDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .build()
    }
}
