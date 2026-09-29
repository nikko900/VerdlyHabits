package com.saintnico.verdlyhabits.data.local.coins

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Pure Room queries. Blocking (non-suspend) methods avoid Room + KSP2 generation
 * bugs (`unexpected jvm signature V`). Call from an IO dispatcher — the coin
 * repository already does.
 */
@Dao
interface CoinDao {

    // ── Wallet ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM coin_wallet WHERE userId = :userId")
    fun observeWallet(userId: String): Flow<CoinWalletEntity?>

    @Query("SELECT * FROM coin_wallet WHERE userId = :userId")
    fun getWallet(userId: String): CoinWalletEntity?

    @Upsert
    fun upsertWallet(wallet: CoinWalletEntity)

    // ── Transactions ─────────────────────────────────────────────────────────

    /** IGNORE so replayed gift/bonus events can never double-credit. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTransactionIfAbsent(tx: CoinTransactionEntity): Long

    @Query("SELECT * FROM coin_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun observeTransactions(userId: String, limit: Int): Flow<List<CoinTransactionEntity>>

    @Query("SELECT * FROM coin_transactions WHERE synced = 0 AND userId = :userId")
    fun unsyncedTransactions(userId: String): List<CoinTransactionEntity>

    @Query("UPDATE coin_transactions SET synced = 1 WHERE id IN (:ids)")
    fun markTransactionsSynced(ids: List<String>)

    // ── Stakes ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertStakeIfAbsent(stake: ChallengeStakeEntity): Long

    @Upsert
    fun upsertStake(stake: ChallengeStakeEntity)

    @Query("SELECT * FROM challenge_stakes WHERE userId = :userId AND status = 'ACTIVE'")
    fun observeActiveStakes(userId: String): Flow<List<ChallengeStakeEntity>>

    @Query("SELECT COUNT(*) FROM challenge_stakes WHERE userId = :userId AND status = 'CONVERTED'")
    fun convertedStakeCount(userId: String): Int

    @Query("SELECT * FROM challenge_stakes WHERE challengeId = :challengeId AND userId = :userId AND status = 'ACTIVE' LIMIT 1")
    fun activeStakeFor(challengeId: String, userId: String): ChallengeStakeEntity?

    @Query("SELECT * FROM challenge_stakes WHERE challengeId = :challengeId AND userId = :userId AND status = 'ACTIVE' LIMIT 1")
    fun observeActiveStakeFor(challengeId: String, userId: String): Flow<ChallengeStakeEntity?>

    @Query("SELECT * FROM challenge_stakes WHERE synced = 0 AND userId = :userId")
    fun unsyncedStakes(userId: String): List<ChallengeStakeEntity>

    @Query("UPDATE challenge_stakes SET synced = 1 WHERE id IN (:ids)")
    fun markStakesSynced(ids: List<String>)

    // ── Gifts ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertGiftIfAbsent(gift: GiftEventEntity): Long

    @Query("SELECT * FROM gift_events WHERE challengeId = :challengeId")
    fun observeGiftsForChallenge(challengeId: String): Flow<List<GiftEventEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM gift_events WHERE challengeId = :challengeId AND receiverId = :userId")
    fun giftsReceivedInChallenge(challengeId: String, userId: String): Int
}
