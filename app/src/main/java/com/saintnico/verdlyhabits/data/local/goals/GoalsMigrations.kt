package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN tinyVersionText TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN anchorCue TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN softReviewDate INTEGER")
        db.execSQL("ALTER TABLE goals ADD COLUMN paceProfile TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN lastLapseAt INTEGER")
        db.execSQL("ALTER TABLE goals ADD COLUMN longestCleanStreakDays INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN triggerText TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN replacementText TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN costPerOccurrence REAL")
        db.execSQL("ALTER TABLE goals ADD COLUMN costUnit TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN floorCount INTEGER")
        db.execSQL("ALTER TABLE goals ADD COLUMN floorPeriodDays INTEGER")
        db.execSQL("ALTER TABLE goals ADD COLUMN consecutiveWeeksHeld INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN lifetimeWeeksHeld INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN promotedFromGoalId TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN lastConfirmedAt INTEGER")
        db.execSQL("ALTER TABLE goals ADD COLUMN streakAnchorAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN confirmationStateCache TEXT NOT NULL DEFAULT 'STILL_CLEAR'")
        db.execSQL("ALTER TABLE goals ADD COLUMN recentUrgesUntil INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_confirmations` (
                `id` TEXT NOT NULL,
                `goalId` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_confirmations_goalId` ON `goal_confirmations` (`goalId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_confirmations_goalId_date` ON `goal_confirmations` (`goalId`, `date`)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_silent_gaps` (
                `id` TEXT NOT NULL,
                `goalId` TEXT NOT NULL,
                `gapStartDate` TEXT NOT NULL,
                `gapEndDate` TEXT,
                `resolution` TEXT NOT NULL,
                `resolvedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_silent_gaps_goalId` ON `goal_silent_gaps` (`goalId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goal_silent_gaps_resolution` ON `goal_silent_gaps` (`resolution`)",
        )
        val now = System.currentTimeMillis()
        db.execSQL("UPDATE goals SET streakAnchorAt = startDate WHERE streakAnchorAt = 0")
        // Room stores GoalType as the enum name string.
        db.execSQL(
            """
            UPDATE goals
            SET lastConfirmedAt = $now,
                confirmationStateCache = 'STILL_CLEAR'
            WHERE goalType = 'QUIT' AND isArchived = 0
            """.trimIndent(),
        )
    }
}

