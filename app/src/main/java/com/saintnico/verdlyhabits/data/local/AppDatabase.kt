package com.saintnico.verdlyhabits.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saintnico.verdlyhabits.data.local.coins.ChallengeStakeEntity
import com.saintnico.verdlyhabits.data.local.coins.CoinDao
import com.saintnico.verdlyhabits.data.local.coins.CoinTransactionEntity
import com.saintnico.verdlyhabits.data.local.coins.CoinWalletEntity
import com.saintnico.verdlyhabits.data.local.coins.GiftEventEntity
import com.saintnico.verdlyhabits.data.local.coins.MIGRATION_3_4
import com.saintnico.verdlyhabits.data.local.focus.FocusSessionDao
import com.saintnico.verdlyhabits.data.local.focus.FocusSessionEntity
import com.saintnico.verdlyhabits.data.local.goals.CheckInEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalConfirmationEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalSilentGapEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalsDao
import com.saintnico.verdlyhabits.data.local.goals.MIGRATION_4_5
import com.saintnico.verdlyhabits.data.local.goals.MIGRATION_5_6
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.data.local.streak.CompletionRecordEntity
import com.saintnico.verdlyhabits.data.local.streak.DuoSwapEntity
import com.saintnico.verdlyhabits.data.local.streak.MIGRATION_2_3
import com.saintnico.verdlyhabits.data.local.streak.PendingOpEntity
import com.saintnico.verdlyhabits.data.local.streak.ShieldBalanceEntity
import com.saintnico.verdlyhabits.data.local.streak.ShieldGrantEntity
import com.saintnico.verdlyhabits.data.local.streak.ShieldSpendEntity
import com.saintnico.verdlyhabits.data.local.streak.StreakDao
import com.saintnico.verdlyhabits.data.local.streak.StreakLossEntity
import com.saintnico.verdlyhabits.data.local.MIGRATION_1_2

@Database(
    entities = [
        GoalEntity::class,
        MilestoneEntity::class,
        CheckInEntity::class,
        GoalConfirmationEntity::class,
        GoalSilentGapEntity::class,
        FocusSessionEntity::class,
        CompletionRecordEntity::class,
        ShieldBalanceEntity::class,
        ShieldSpendEntity::class,
        ShieldGrantEntity::class,
        StreakLossEntity::class,
        PendingOpEntity::class,
        DuoSwapEntity::class,
        CoinWalletEntity::class,
        CoinTransactionEntity::class,
        ChallengeStakeEntity::class,
        GiftEventEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalsDao(): GoalsDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun streakDao(): StreakDao
    abstract fun coinDao(): CoinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: openOrRecreate(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun openOrRecreate(context: Context): AppDatabase {
            return try {
                buildDb(context).also {
                    // Force-open so a schema mismatch fails here (recoverable) instead of
                    // killing the first Room Flow collector mid-launch.
                    it.openHelper.writableDatabase
                }
            } catch (e: Exception) {
                // Do not call backup/restore here — those re-enter getDatabase() while
                // INSTANCE is still null and can stack-overflow during sign-in.
                Log.e("AppDatabase", "schema open failed. recreating verdly_database", e)
                runCatching { context.deleteDatabase("verdly_database") }
                buildDb(context)
            }
        }

        private fun buildDb(context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "verdly_database",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                // Streak history now lives here, so a destructive fallback on upgrade
                // would be exactly the silent data loss this engine exists to prevent.
                // Downgrades stay destructive since they cannot be migrated forward.
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
