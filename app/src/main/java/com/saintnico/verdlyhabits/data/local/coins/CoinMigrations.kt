package com.saintnico.verdlyhabits.data.local.coins

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS coin_wallet (
                userId TEXT NOT NULL PRIMARY KEY,
                balance INTEGER NOT NULL,
                lifetimeEarned INTEGER NOT NULL,
                lifetimeSpent INTEGER NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS coin_transactions (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                challengeId TEXT,
                counterpartyUserId TEXT,
                timestamp INTEGER NOT NULL,
                synced INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_coin_transactions_userId ON coin_transactions(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_coin_transactions_challengeId ON coin_transactions(challengeId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS challenge_stakes (
                id TEXT NOT NULL PRIMARY KEY,
                challengeId TEXT NOT NULL,
                userId TEXT NOT NULL,
                stakedAmount INTEGER NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                resolvedAt INTEGER,
                synced INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_challenge_stakes_challengeId ON challenge_stakes(challengeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_challenge_stakes_userId ON challenge_stakes(userId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gift_events (
                id TEXT NOT NULL PRIMARY KEY,
                challengeId TEXT NOT NULL,
                senderId TEXT NOT NULL,
                receiverId TEXT NOT NULL,
                amount INTEGER NOT NULL,
                message TEXT,
                timestamp INTEGER NOT NULL,
                synced INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gift_events_challengeId ON gift_events(challengeId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gift_events_receiverId ON gift_events(receiverId)")
    }
}
