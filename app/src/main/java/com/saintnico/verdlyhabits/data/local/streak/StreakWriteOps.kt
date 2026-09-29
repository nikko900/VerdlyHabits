package com.saintnico.verdlyhabits.data.local.streak

import androidx.room.withTransaction
import com.saintnico.verdlyhabits.data.local.AppDatabase

/**
 * Composite streak writes. Kept outside [StreakDao] because Room/KSP cannot generate
 * `@Transaction` default bodies on DAO interfaces.
 */
class StreakWriteOps(private val db: AppDatabase) {

    private val dao = db.streakDao()

    /**
     * Spends a shield against one missed day. Atomic so a crash cannot decrement the balance
     * without recording the covered day, or vice versa. Returns false when the day is already
     * covered or the user holds nothing.
     */
    suspend fun spendShield(spend: ShieldSpendEntity): Boolean = db.withTransaction {
        val current = dao.balance() ?: ShieldBalanceEntity()
        if (current.available <= 0) return@withTransaction false
        if (dao.insertSpendIfAbsent(spend) == -1L) return@withTransaction false
        dao.putBalance(current.copy(available = current.available - 1))
        true
    }

    /**
     * Credits newly earned shields, clamped to [cap] so earned and purchased shields share one
     * ceiling. Idempotent per `(habitId, milestone)`.
     */
    suspend fun creditEarnedShields(
        habitId: String,
        milestones: List<Int>,
        cap: Int,
        now: Long,
    ): Int = db.withTransaction {
        var granted = 0
        for (milestone in milestones) {
            if (dao.insertGrantIfAbsent(ShieldGrantEntity(habitId, milestone, now)) != -1L) granted++
        }
        if (granted == 0) return@withTransaction 0
        val current = dao.balance() ?: ShieldBalanceEntity()
        val room = (cap - current.available).coerceAtLeast(0)
        val applied = granted.coerceAtMost(room)
        dao.putBalance(
            current.copy(
                available = current.available + applied,
                lifetimeEarned = current.lifetimeEarned + granted,
            ),
        )
        applied
    }

    /** Adds purchased shields, respecting the same cap as earned ones. */
    suspend fun creditPurchasedShields(count: Int, cap: Int): Int = db.withTransaction {
        if (count <= 0) return@withTransaction 0
        val current = dao.balance() ?: ShieldBalanceEntity()
        val room = (cap - current.available).coerceAtLeast(0)
        val applied = count.coerceAtMost(room)
        if (applied <= 0) return@withTransaction 0
        dao.putBalance(
            current.copy(
                available = current.available + applied,
                lifetimePurchased = current.lifetimePurchased + applied,
            ),
        )
        applied
    }

    /** Records a completion and enqueues its remote push in one transaction. */
    suspend fun recordCompletion(row: CompletionRecordEntity, op: PendingOpEntity) {
        db.withTransaction {
            dao.upsertCompletion(row)
            dao.enqueue(op)
        }
    }
}
