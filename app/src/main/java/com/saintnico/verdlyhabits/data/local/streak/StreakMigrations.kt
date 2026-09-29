package com.saintnico.verdlyhabits.data.local.streak

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the streak engine's tables. Purely additive — no existing goal, check-in, or focus
 * table is touched, so nothing already on device can be lost by running this.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `completion_records` (
                `habitId` TEXT NOT NULL,
                `localDate` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                `zoneId` TEXT NOT NULL,
                `offsetSeconds` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `proofUrl` TEXT,
                `syncedAt` INTEGER,
                PRIMARY KEY(`habitId`, `localDate`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_completion_records_habitId` ON `completion_records` (`habitId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_completion_records_syncedAt` ON `completion_records` (`syncedAt`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shield_balance` (
                `id` INTEGER NOT NULL,
                `available` INTEGER NOT NULL,
                `lifetimeEarned` INTEGER NOT NULL,
                `lifetimePurchased` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shield_spends` (
                `id` TEXT NOT NULL,
                `habitId` TEXT NOT NULL,
                `coveredDate` TEXT NOT NULL,
                `committedAt` INTEGER NOT NULL,
                `syncedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shield_spends_habitId` ON `shield_spends` (`habitId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shield_grants` (
                `habitId` TEXT NOT NULL,
                `milestone` INTEGER NOT NULL,
                `grantedAt` INTEGER NOT NULL,
                PRIMARY KEY(`habitId`, `milestone`)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `streak_losses` (
                `id` TEXT NOT NULL,
                `habitId` TEXT NOT NULL,
                `finalStreak` INTEGER NOT NULL,
                `missedDate` TEXT NOT NULL,
                `lastQualifyingDate` TEXT,
                `expiredAt` INTEGER NOT NULL,
                `shieldsAvailableAtExpiry` INTEGER NOT NULL,
                `acknowledged` INTEGER NOT NULL,
                `notifiedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_streak_losses_acknowledged` ON `streak_losses` (`acknowledged`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_ops` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `kind` TEXT NOT NULL,
                `payload` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL,
                `nextAttemptAt` INTEGER NOT NULL,
                `lastError` TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_ops_kind` ON `pending_ops` (`kind`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `duo_swaps` (
                `pairId` TEXT NOT NULL,
                `fromUid` TEXT NOT NULL,
                `toUid` TEXT NOT NULL,
                `forDate` TEXT NOT NULL,
                `grantedAt` INTEGER NOT NULL,
                PRIMARY KEY(`pairId`, `toUid`, `forDate`)
            )
            """.trimIndent(),
        )
    }
}
