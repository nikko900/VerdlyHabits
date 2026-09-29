package com.saintnico.verdlyhabits.data.local.streak

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.saintnico.verdlyhabits.domain.streak.CompletionRecord
import com.saintnico.verdlyhabits.domain.streak.CompletionSource
import com.saintnico.verdlyhabits.domain.streak.ShieldLedger
import com.saintnico.verdlyhabits.domain.streak.StreakLossRecord
import java.time.Instant
import java.time.LocalDate

/**
 * One completed day. The composite primary key `(habitId, localDate)` is the whole reason
 * this table exists: it makes an upsert idempotent, so the same completion arriving twice
 * from two devices collapses into one row instead of inflating a run. That turns cross-device
 * sync into a set union, which cannot lose a completion the way the old whole-blob JSON
 * overwrite could.
 */
@Entity(
    tableName = "completion_records",
    primaryKeys = ["habitId", "localDate"],
    indices = [Index("habitId"), Index("syncedAt")],
)
data class CompletionRecordEntity(
    val habitId: String,
    /** ISO `yyyy-MM-dd`, locked to the zone the user was in when they completed. */
    val localDate: String,
    val completedAt: Long,
    val zoneId: String,
    val offsetSeconds: Int,
    val source: String,
    val proofUrl: String? = null,
    /** Null until the row has been pushed to Firestore. Drives the outbound sync query. */
    val syncedAt: Long? = null,
) {
    fun toDomain(): CompletionRecord? {
        val date = runCatching { LocalDate.parse(localDate) }.getOrNull() ?: return null
        return CompletionRecord(
            habitId = habitId,
            localDate = date,
            completedAt = Instant.ofEpochMilli(completedAt),
            zoneId = zoneId,
            offsetSeconds = offsetSeconds,
            source = runCatching { CompletionSource.valueOf(source) }
                .getOrDefault(CompletionSource.MANUAL),
            proofUrl = proofUrl,
        )
    }

    companion object {
        fun from(record: CompletionRecord, syncedAt: Long? = null) = CompletionRecordEntity(
            habitId = record.habitId,
            localDate = record.localDate.toString(),
            completedAt = record.completedAt.toEpochMilli(),
            zoneId = record.zoneId,
            offsetSeconds = record.offsetSeconds,
            source = record.source.name,
            proofUrl = record.proofUrl,
            syncedAt = syncedAt,
        )
    }
}

/**
 * The user's shield balance. Single row.
 *
 * The balance lives here rather than in DataStore so it stays transactionally consistent with
 * [ShieldSpendEntity] — a spend must decrement the balance and record the covered day
 * atomically, or a crash between the two writes would either lose a shield or grant a free
 * bridge. The legacy DataStore value is migrated in once on first read.
 */
@Entity(tableName = "shield_balance")
data class ShieldBalanceEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val available: Int = 0,
    val lifetimeEarned: Int = 0,
    val lifetimePurchased: Int = 0,
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}

/**
 * A shield committed against a specific missed day, keyed `habitId:localDate`.
 *
 * Keying on the covered day is what makes spending idempotent: re-evaluating a streak any
 * number of times can never double-spend, because the second attempt collides on the primary
 * key instead of inserting.
 */
@Entity(tableName = "shield_spends", indices = [Index("habitId")])
data class ShieldSpendEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val coveredDate: String,
    val committedAt: Long,
    val syncedAt: Long? = null,
) {
    companion object {
        fun idFor(habitId: String, date: LocalDate) = "$habitId:$date"
    }
}

/**
 * Records that a shield was already awarded for a given run milestone, so crossing seven days
 * grants exactly one shield no matter how many times the engine re-evaluates that run.
 */
@Entity(tableName = "shield_grants", primaryKeys = ["habitId", "milestone"])
data class ShieldGrantEntity(
    val habitId: String,
    val milestone: Int,
    val grantedAt: Long,
)

/**
 * A streak that ended.
 *
 * Persisted rather than derived for two reasons: the loss moment must be shown exactly once
 * even if the process is killed before the user sees it, and [shieldsAvailableAtExpiry] is a
 * fact about the past that cannot be reconstructed once the balance changes.
 */
@Entity(tableName = "streak_losses", indices = [Index("acknowledged")])
data class StreakLossEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val finalStreak: Int,
    val missedDate: String,
    val lastQualifyingDate: String? = null,
    val expiredAt: Long,
    val shieldsAvailableAtExpiry: Int,
    val acknowledged: Boolean = false,
    /** Set once the "streak lost" notification has fired, so it never fires twice. */
    val notifiedAt: Long? = null,
) {
    fun toDomain(): StreakLossRecord? {
        val missed = runCatching { LocalDate.parse(missedDate) }.getOrNull() ?: return null
        return StreakLossRecord(
            habitId = habitId,
            finalStreak = finalStreak,
            missedDate = missed,
            lastQualifyingDate = lastQualifyingDate?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            },
            expiredAt = Instant.ofEpochMilli(expiredAt),
            shieldsAvailableAtExpiry = shieldsAvailableAtExpiry,
            acknowledged = acknowledged,
        )
    }

    companion object {
        fun from(record: StreakLossRecord) = StreakLossEntity(
            id = record.identity,
            habitId = record.habitId,
            finalStreak = record.finalStreak,
            missedDate = record.missedDate.toString(),
            lastQualifyingDate = record.lastQualifyingDate?.toString(),
            expiredAt = record.expiredAt.toEpochMilli(),
            shieldsAvailableAtExpiry = record.shieldsAvailableAtExpiry,
            acknowledged = record.acknowledged,
        )
    }
}

/** Kinds of work that must survive being offline. */
enum class PendingOpKind {
    COMPLETION_UPSERT,
    SHIELD_SPEND,
    DUO_DAY_CLOSE,
    DUO_SWAP_GRANT,
}

/**
 * Durable outbox. Every remote write is enqueued here first and drained by a worker, so a
 * failed or offline call is retried rather than silently swallowed the way the old
 * fire-and-forget `try/catch` around `saveHabits` was.
 */
@Entity(tableName = "pending_ops", indices = [Index("kind")])
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    /** JSON payload; shape depends on [kind]. */
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val nextAttemptAt: Long = 0,
    val lastError: String? = null,
)

/**
 * Local cache of partner-gifted days. Firestore remains authoritative — this exists so the
 * streak engine can read swaps while offline.
 */
@Entity(tableName = "duo_swaps", primaryKeys = ["pairId", "toUid", "forDate"])
data class DuoSwapEntity(
    val pairId: String,
    val fromUid: String,
    val toUid: String,
    val forDate: String,
    val grantedAt: Long,
)

/** Domain view of the balance plus committed spends. */
fun buildShieldLedger(
    balance: ShieldBalanceEntity?,
    spends: List<ShieldSpendEntity>,
): ShieldLedger = ShieldLedger(
    available = balance?.available ?: 0,
    spentOn = spends.mapTo(HashSet()) { it.id },
    lifetimeEarned = balance?.lifetimeEarned ?: 0,
    lifetimePurchased = balance?.lifetimePurchased ?: 0,
)
