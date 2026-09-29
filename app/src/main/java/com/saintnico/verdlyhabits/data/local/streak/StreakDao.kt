package com.saintnico.verdlyhabits.data.local.streak

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Pure Room queries. All blocking methods are non-suspend to avoid Room 2.6.1 + KSP2
 * generation bugs (`unexpected jvm signature V` / Continuation name clashes). Call from
 * an IO dispatcher — [StreakRepository] already does.
 */
@Dao
interface StreakDao {

    @Query("SELECT * FROM completion_records")
    fun observeCompletions(): Flow<List<CompletionRecordEntity>>

    @Query("SELECT * FROM completion_records")
    fun allCompletions(): List<CompletionRecordEntity>

    @Query("SELECT * FROM completion_records WHERE habitId = :habitId")
    fun completionsFor(habitId: String): List<CompletionRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCompletions(rows: List<CompletionRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCompletion(row: CompletionRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun mergeRemoteCompletions(rows: List<CompletionRecordEntity>)

    @Query("DELETE FROM completion_records WHERE habitId = :habitId AND localDate = :localDate")
    fun deleteCompletion(habitId: String, localDate: String)

    @Query("DELETE FROM completion_records WHERE habitId = :habitId")
    fun deleteCompletionsFor(habitId: String)

    @Query("SELECT * FROM completion_records WHERE syncedAt IS NULL")
    fun unsyncedCompletions(): List<CompletionRecordEntity>

    @Query("UPDATE completion_records SET syncedAt = :at WHERE habitId = :habitId AND localDate = :localDate")
    fun markCompletionSynced(habitId: String, localDate: String, at: Long)

    @Query("SELECT COUNT(*) FROM completion_records")
    fun completionCount(): Int

    @Query("SELECT * FROM shield_balance WHERE id = 1")
    fun observeBalance(): Flow<ShieldBalanceEntity?>

    @Query("SELECT * FROM shield_balance WHERE id = 1")
    fun balance(): ShieldBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putBalance(row: ShieldBalanceEntity)

    @Query("SELECT * FROM shield_spends")
    fun observeSpends(): Flow<List<ShieldSpendEntity>>

    @Query("SELECT * FROM shield_spends")
    fun allSpends(): List<ShieldSpendEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSpendIfAbsent(row: ShieldSpendEntity): Long

    @Query("SELECT * FROM shield_spends WHERE syncedAt IS NULL")
    fun unsyncedSpends(): List<ShieldSpendEntity>

    @Query("UPDATE shield_spends SET syncedAt = :at WHERE id = :id")
    fun markSpendSynced(id: String, at: Long)

    @Query("SELECT * FROM shield_grants WHERE habitId = :habitId")
    fun grantsFor(habitId: String): List<ShieldGrantEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertGrantIfAbsent(row: ShieldGrantEntity): Long

    @Query("SELECT * FROM streak_losses")
    fun observeLosses(): Flow<List<StreakLossEntity>>

    @Query("SELECT * FROM streak_losses WHERE acknowledged = 0 ORDER BY finalStreak DESC")
    fun observeUnacknowledgedLosses(): Flow<List<StreakLossEntity>>

    @Query("SELECT * FROM streak_losses")
    fun allLosses(): List<StreakLossEntity>

    @Query("SELECT * FROM streak_losses WHERE id = :id")
    fun loss(id: String): StreakLossEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLossIfAbsent(row: StreakLossEntity): Long

    @Query("UPDATE streak_losses SET acknowledged = 1 WHERE id = :id")
    fun acknowledgeLoss(id: String)

    @Query("UPDATE streak_losses SET notifiedAt = :at WHERE id = :id")
    fun markLossNotified(id: String, at: Long)

    @Query("SELECT * FROM streak_losses WHERE notifiedAt IS NULL")
    fun unnotifiedLosses(): List<StreakLossEntity>

    @Insert
    fun enqueue(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops WHERE nextAttemptAt <= :now ORDER BY createdAt ASC LIMIT :limit")
    fun dueOps(now: Long, limit: Int): List<PendingOpEntity>

    @Query("SELECT COUNT(*) FROM pending_ops")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM pending_ops WHERE id = :id")
    fun deleteOp(id: Long)

    @Query(
        "UPDATE pending_ops SET attempts = :attempts, nextAttemptAt = :nextAt, lastError = :error WHERE id = :id",
    )
    fun rescheduleOp(id: Long, attempts: Int, nextAt: Long, error: String?)

    @Query("SELECT * FROM duo_swaps WHERE pairId = :pairId")
    fun observeSwaps(pairId: String): Flow<List<DuoSwapEntity>>

    @Query("SELECT * FROM duo_swaps WHERE pairId = :pairId")
    fun swapsFor(pairId: String): List<DuoSwapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSwaps(rows: List<DuoSwapEntity>)

    @Query("DELETE FROM duo_swaps WHERE pairId = :pairId")
    fun clearSwaps(pairId: String)
}
