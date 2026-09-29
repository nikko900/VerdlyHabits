package com.saintnico.verdlyhabits.domain.streak

import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.HabitScheduling
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * How a day came to be satisfied.
 *
 * Every source here represents a day the user genuinely holds, so all of them advance the
 * run. Shields are deliberately *not* modelled as a completion source — they live in
 * [ShieldLedger] and bridge a gap without incrementing, which is a different operation.
 */
enum class CompletionSource {
    /** Normal in-app check-off. */
    MANUAL,

    /** Check-off that carried a photo proof. */
    PROOF,

    /** Backfilled from a legacy `completedDates` string during migration. */
    MIGRATED,

    /** A partner gifted this day via swap-a-day. Counts as complete, no proof needed. */
    DUO_SWAP,
}

/**
 * One immutable fact: a habit was satisfied on a given local day.
 *
 * [localDate] is decided once, at completion time, in the zone the user was actually
 * standing in, and is never re-derived from [completedAt] afterwards. That is what makes
 * the streak survive timezone travel — crossing a boundary cannot retroactively move a
 * completion into a different day.
 *
 * Identity is `habitId + localDate`, which makes the sync merge a set union: commutative
 * and idempotent, so two devices reconciling after offline use cannot lose a completion.
 */
data class CompletionRecord(
    val habitId: String,
    val localDate: LocalDate,
    val completedAt: Instant,
    val zoneId: String,
    val offsetSeconds: Int,
    val source: CompletionSource = CompletionSource.MANUAL,
    val proofUrl: String? = null,
) {
    val identity: String get() = "$habitId:$localDate"
}

/**
 * Shields held and spent. A shield bridges one missed expected day, preserving the run
 * without incrementing it.
 *
 * [spentOn] is keyed by `habitId:localDate`, which makes committing a shield idempotent:
 * the engine can be re-run any number of times and will never double-spend.
 */
data class ShieldLedger(
    val available: Int = 0,
    val spentOn: Set<String> = emptySet(),
    val lifetimeEarned: Int = 0,
    val lifetimePurchased: Int = 0,
) {
    fun isShielded(habitId: String, date: LocalDate): Boolean = "$habitId:$date" in spentOn

    fun roomToHold(config: StreakConfig): Int = (config.shieldCap - available).coerceAtLeast(0)

    fun atCap(config: StreakConfig): Boolean = available >= config.shieldCap
}

/** A day gifted by a duo partner. Unlimited, but bucketed by month for display. */
data class DuoSwap(
    val fromUid: String,
    val toUid: String,
    val forDate: LocalDate,
    val grantedAt: Instant,
) {
    val monthKey: String get() = monthKeyOf(forDate)

    companion object {
        fun monthKeyOf(date: LocalDate): String = "${date.year}-%02d".format(date.monthValue)
    }
}

/**
 * Tunable rules. Everything that used to be a magic number lives here so the mechanics are
 * explicit and configurable rather than scattered across UI conditionals.
 */
data class StreakConfig(
    /** Max shields a user may hold at once, whether earned or purchased. */
    val shieldCap: Int = 5,

    /** Earn one shield each time a run crosses a multiple of this. */
    val shieldEarnIntervalDays: Int = 7,

    /**
     * How long after a missed day the user may still repair it with a shield. Shields are
     * offered on next open rather than auto-applied, so the run is held in a repairable
     * state for this long instead of breaking immediately at midnight.
     */
    val repairWindow: Duration = Duration.ofHours(48),

    /** Lead time before the day closes at which a run starts reading as at-risk. */
    val atRiskLeadTime: Duration = Duration.ofHours(4),

    /** Lead time at which the at-risk reminder escalates from soft to urgent. */
    val urgentLeadTime: Duration = Duration.ofHours(1),
) {
    init {
        require(shieldCap >= 0) { "shieldCap must not be negative" }
        require(shieldEarnIntervalDays > 0) { "shieldEarnIntervalDays must be positive" }
        require(!repairWindow.isNegative) { "repairWindow must not be negative" }
        require(urgentLeadTime <= atRiskLeadTime) { "urgent lead time must be within at-risk lead time" }
    }

    companion object {
        val DEFAULT = StreakConfig()
    }
}

/**
 * When a habit is expected to be done, and the daily window it must land inside.
 *
 * Day-of-week matching delegates to the existing [HabitScheduling] helper so scheduling
 * behaviour stays consistent with the home screen and the reminder scheduler, which
 * already use it.
 */
data class Schedule(
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val customDaysMask: String? = null,
    val windowStart: LocalTime? = null,
    val windowEnd: LocalTime? = null,
) {
    fun isExpectedOn(date: LocalDate): Boolean =
        HabitScheduling.isDueOn(frequency, customDaysMask, date)

    /**
     * The instant [date] stops accepting completions. Honours an explicit completion
     * window when the habit has one, otherwise midnight at the end of the day.
     */
    fun closesAt(date: LocalDate, zone: ZoneId): Instant {
        val end = windowEnd
        return if (end != null) {
            date.atTime(end).atZone(zone).toInstant()
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant()
        }
    }

    /** Walks backwards from [from] inclusive, yielding only expected days. */
    fun expectedDaysBackFrom(from: LocalDate, limit: Int): List<LocalDate> {
        if (limit <= 0) return emptyList()
        val out = ArrayList<LocalDate>(limit)
        var cursor = from
        var scanned = 0
        // A weekly-custom habit can have six off-days between expected days; the ceiling
        // keeps a pathological/empty mask from spinning forever.
        val scanCeiling = limit * 8 + 32
        while (out.size < limit && scanned < scanCeiling) {
            if (isExpectedOn(cursor)) out += cursor
            cursor = cursor.minusDays(1)
            scanned++
        }
        return out
    }

    /** The most recent expected day at or before [date], if one exists nearby. */
    fun expectedOnOrBefore(date: LocalDate): LocalDate? {
        var cursor = date
        repeat(14) {
            if (isExpectedOn(cursor)) return cursor
            cursor = cursor.minusDays(1)
        }
        return null
    }

    /** The next expected day strictly after [date]. */
    fun nextExpectedAfter(date: LocalDate): LocalDate {
        var cursor = date.plusDays(1)
        repeat(14) {
            if (isExpectedOn(cursor)) return cursor
            cursor = cursor.plusDays(1)
        }
        return date.plusDays(1)
    }

    companion object {
        val DAILY = Schedule()
    }
}

/**
 * The lifecycle of a streak. Exactly one of these is true at any moment, and every screen
 * reads it rather than inventing its own conditional.
 */
enum class StreakStatus {
    /** No completions on record. */
    NOT_STARTED,

    /** Run is alive and today is not yet urgent. */
    ACTIVE,

    /** Today is expected, still open, and the close deadline is approaching. */
    AT_RISK,

    /** A missed day was bridged by a committed shield. Run preserved, not incremented. */
    FROZEN,

    /**
     * A day was missed and the repair window has not closed. The run is being held, not
     * destroyed — the user can still spend a shield to keep it.
     */
    REPAIRABLE,

    /** A day was missed and the repair window closed. The run is gone. */
    BROKEN,
}

/** A shield that has been committed against a specific missed day. */
data class AppliedShield(
    val habitId: String,
    val coveredDate: LocalDate,
    val committedAt: Instant,
)

/** An offer to repair a still-recoverable miss. Surfaced on next app open. */
data class RepairOffer(
    val habitId: String,
    val missedDate: LocalDate,
    val streakAtStake: Int,
    val expiresAt: Instant,
    val shieldsAvailable: Int,
) {
    /** True when the user already holds a shield and needs only to confirm. */
    val canRepairNow: Boolean get() = shieldsAvailable > 0
}

/**
 * A streak that ended. Persisted rather than derived, for two reasons: the loss moment must
 * be shown exactly once even if the process is killed before the user sees it, and
 * [shieldsAvailableAtExpiry] is a fact about the past that cannot be reconstructed later
 * once the user's shield balance changes.
 *
 * This is the highest-intent moment in the whole feature — the user just concretely lost
 * something — so it carries the data needed to make a specific rather than generic offer.
 */
data class StreakLossRecord(
    val habitId: String,
    /** The run length that was lost. */
    val finalStreak: Int,
    /** The expected day that went unsatisfied. */
    val missedDate: LocalDate,
    /** The last day the run was intact. */
    val lastQualifyingDate: LocalDate?,
    /** When the repair window closed and the run became unrecoverable. */
    val expiredAt: Instant,
    /**
     * How many shields the user held at the moment the window closed. Zero means a shield
     * would genuinely have saved this run, which is what makes the offer specific and
     * honest rather than a generic upsell.
     */
    val shieldsAvailableAtExpiry: Int,
    /** Set once the user has been shown the loss moment. */
    val acknowledged: Boolean = false,
) {
    val identity: String get() = "$habitId:$missedDate"

    /**
     * True when the user could not have saved this run because they held nothing. Only in
     * this case is "a shield would have saved this streak" a true statement.
     */
    val wasPreventableWithShield: Boolean get() = shieldsAvailableAtExpiry == 0

    /** Losses below this aren't worth interrupting the user for. */
    fun isWorthSurfacing(minimum: Int = MIN_SURFACEABLE_STREAK): Boolean = finalStreak >= minimum

    companion object {
        /** A one or two day run ending is noise, not a moment. */
        const val MIN_SURFACEABLE_STREAK: Int = 3
    }
}

/** Everything the engine needs. No clock, no I/O, no Android. */
data class StreakInput(
    val habitId: String,
    val completions: List<CompletionRecord>,
    val schedule: Schedule = Schedule.DAILY,
    val today: LocalDate,
    val now: Instant,
    val zone: ZoneId,
    val shields: ShieldLedger = ShieldLedger(),
    val config: StreakConfig = StreakConfig.DEFAULT,
    /** Days gifted to this user by a duo partner. Count as genuine completions. */
    val swaps: Set<LocalDate> = emptySet(),
    /**
     * Losses already recorded and persisted. Prevents the engine from re-emitting a loss
     * the repository has already committed.
     */
    val recordedLosses: Set<String> = emptySet(),
)

/**
 * The one answer. Every surface — habit card, profile, stats, duo screen, widget,
 * notification scheduler — reads these fields instead of recomputing.
 */
data class StreakResult(
    val habitId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val status: StreakStatus,
    val isCompleteToday: Boolean,
    val isExpectedToday: Boolean,
    val lastQualifyingDate: LocalDate? = null,
    val nextDueDate: LocalDate? = null,
    val appliedShields: List<AppliedShield> = emptyList(),
    val repairOffer: RepairOffer? = null,
    /** When today's window closes. Drives at-risk scheduling. */
    val closesAt: Instant? = null,
    /** Shields this run has newly earned, for the repository to credit. */
    val shieldsEarned: Int = 0,
    /**
     * A loss the engine has just detected and the repository has not yet committed. Non-null
     * for exactly one evaluation per lost run, after which it appears in
     * [StreakInput.recordedLosses] and stops being emitted.
     */
    val pendingLoss: StreakLossRecord? = null,
) {
    val isAtRisk: Boolean get() = status == StreakStatus.AT_RISK
    val isRepairable: Boolean get() = status == StreakStatus.REPAIRABLE
    val wasShielded: Boolean get() = appliedShields.isNotEmpty()

    /** True when the run is intact in any form, including held or frozen. */
    val isAlive: Boolean
        get() = status == StreakStatus.ACTIVE ||
            status == StreakStatus.AT_RISK ||
            status == StreakStatus.FROZEN ||
            status == StreakStatus.REPAIRABLE

    companion object {
        fun empty(habitId: String) = StreakResult(
            habitId = habitId,
            currentStreak = 0,
            longestStreak = 0,
            status = StreakStatus.NOT_STARTED,
            isCompleteToday = false,
            isExpectedToday = false,
        )
    }
}
