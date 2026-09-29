package com.saintnico.verdlyhabits.domain.streak

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A day in a duo's shared history, from one partner's perspective.
 *
 * Note there is no `streakDays` counter anywhere in this file. The old duo implementation
 * stored a counter and mutated it with a non-transactional read-modify-write, which meant
 * two partners completing at the same moment could lose or double an increment. Deriving
 * the run from an immutable day ledger removes the race by removing the counter.
 */
data class DuoDay(
    val date: LocalDate,
    val mineSatisfied: Boolean,
    val buddySatisfied: Boolean,
) {
    val bothSatisfied: Boolean get() = mineSatisfied && buddySatisfied
}

data class DuoStreakInput(
    val pairId: String,
    /** Days the server has confirmed closed, keyed by date. */
    val days: Map<LocalDate, DuoDay>,
    /** Dates I gifted to my partner, satisfying their side without proof. */
    val swapsIGave: Set<LocalDate> = emptySet(),
    /** Dates my partner gifted to me, satisfying my side without proof. */
    val swapsIReceived: Set<LocalDate> = emptySet(),
    val today: LocalDate,
    val now: Instant,
    val zone: ZoneId,
    val config: StreakConfig = StreakConfig.DEFAULT,
    /** Set when a grace/shield has been committed against a missed shared day. */
    val shieldedDates: Set<LocalDate> = emptySet(),
)

data class DuoStreakResult(
    val pairId: String,
    val streakDays: Int,
    val longestStreak: Int,
    val status: StreakStatus,
    val mineDoneToday: Boolean,
    val buddyDoneToday: Boolean,
    val bothDoneToday: Boolean,
    val closesAt: Instant?,
    val missedDate: LocalDate? = null,
    val repairExpiresAt: Instant? = null,
    /** Swap counts for the current calendar month, for display and analytics only. */
    val swapsGivenThisMonth: Int = 0,
    val swapsReceivedThisMonth: Int = 0,
) {
    val isAtRisk: Boolean get() = status == StreakStatus.AT_RISK
    val isAlive: Boolean
        get() = status == StreakStatus.ACTIVE ||
            status == StreakStatus.AT_RISK ||
            status == StreakStatus.FROZEN ||
            status == StreakStatus.REPAIRABLE
}

/**
 * Derives a duo streak from the pair's shared day ledger using the same rules as
 * [StreakEngine].
 *
 * A shared day counts when *both* partners are satisfied, where a partner is satisfied by
 * their own completion or by a swap day gifted to them. Swaps count as genuine completions;
 * shields bridge without incrementing. Duo streaks are daily by definition, so there is no
 * schedule to consult.
 */
object DuoStreakResolver {

    private const val MAX_LOOKBACK_DAYS = 3_650

    fun evaluate(input: DuoStreakInput): DuoStreakResult {
        val satisfied = sharedSatisfiedDays(input)
        val today = input.today
        val todayDay = dayFor(input, today)
        val mineToday = todayDay.mineSatisfied
        val buddyToday = todayDay.buddySatisfied
        val bothToday = today in satisfied
        val closesAt = today.plusDays(1).atStartOfDay(input.zone).toInstant()

        var run = if (bothToday) 1 else 0
        var breakDate: LocalDate? = null
        var usedShield = false

        var cursor = today.minusDays(1)
        var walked = 0
        while (walked < MAX_LOOKBACK_DAYS) {
            when {
                cursor in satisfied -> run++
                cursor in input.shieldedDates -> usedShield = true
                else -> {
                    breakDate = cursor
                    break
                }
            }
            cursor = cursor.minusDays(1)
            walked++
        }

        val status = resolveStatus(
            input = input,
            run = run,
            breakDate = breakDate,
            bothToday = bothToday,
            usedShield = usedShield,
            closesAt = closesAt,
        )

        val monthKey = DuoSwap.monthKeyOf(today)
        return DuoStreakResult(
            pairId = input.pairId,
            streakDays = if (status == StreakStatus.BROKEN) 0 else run,
            longestStreak = maxOf(longestSharedRun(satisfied, input.shieldedDates), run),
            status = status,
            mineDoneToday = mineToday,
            buddyDoneToday = buddyToday,
            bothDoneToday = bothToday,
            closesAt = closesAt,
            missedDate = breakDate,
            repairExpiresAt = breakDate?.let { repairDeadline(input, it) },
            swapsGivenThisMonth = input.swapsIGave.count { DuoSwap.monthKeyOf(it) == monthKey },
            swapsReceivedThisMonth = input.swapsIReceived.count { DuoSwap.monthKeyOf(it) == monthKey },
        )
    }

    private fun dayFor(input: DuoStreakInput, date: LocalDate): DuoDay {
        val base = input.days[date]
        val mine = base?.mineSatisfied == true || date in input.swapsIReceived
        val buddy = base?.buddySatisfied == true || date in input.swapsIGave
        return DuoDay(date, mine, buddy)
    }

    private fun sharedSatisfiedDays(input: DuoStreakInput): Set<LocalDate> {
        val candidates = HashSet<LocalDate>(input.days.keys)
        candidates += input.swapsIGave
        candidates += input.swapsIReceived
        return candidates.filterTo(HashSet()) { dayFor(input, it).bothSatisfied }
    }

    private fun resolveStatus(
        input: DuoStreakInput,
        run: Int,
        breakDate: LocalDate?,
        bothToday: Boolean,
        usedShield: Boolean,
        closesAt: Instant,
    ): StreakStatus {
        if (breakDate != null && run == 0) return StreakStatus.BROKEN
        if (breakDate != null) {
            return if (repairDeadline(input, breakDate).isAfter(input.now)) {
                StreakStatus.REPAIRABLE
            } else {
                StreakStatus.BROKEN
            }
        }
        if (run == 0) return StreakStatus.NOT_STARTED
        if (usedShield && !bothToday) return StreakStatus.FROZEN
        if (!bothToday && !input.now.isBefore(closesAt.minus(input.config.atRiskLeadTime))) {
            return StreakStatus.AT_RISK
        }
        return StreakStatus.ACTIVE
    }

    private fun repairDeadline(input: DuoStreakInput, missedDate: LocalDate): Instant =
        missedDate.plusDays(1).atStartOfDay(input.zone).toInstant().plus(input.config.repairWindow)

    private fun longestSharedRun(satisfied: Set<LocalDate>, shielded: Set<LocalDate>): Int {
        if (satisfied.isEmpty()) return 0
        val sorted = satisfied.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val day = sorted[i]
            current = if (bridged(prev, day, shielded)) current + 1 else 1
            if (current > best) best = current
        }
        return best
    }

    /** True when every day strictly between the two is covered by a shield. */
    private fun bridged(from: LocalDate, to: LocalDate, shielded: Set<LocalDate>): Boolean {
        var cursor = from.plusDays(1)
        var guard = 0
        while (cursor.isBefore(to) && guard < 400) {
            if (cursor !in shielded) return false
            cursor = cursor.plusDays(1)
            guard++
        }
        return cursor == to
    }
}
