package com.saintnico.verdlyhabits.domain.streak

import java.time.Instant
import java.time.LocalDate

/**
 * The single source of truth for streak state.
 *
 * Given a habit's completion history and an injected clock, [evaluate] deterministically
 * produces the current run, the longest run, and exactly one [StreakStatus]. Nothing else
 * in the app is permitted to count consecutive days.
 *
 * Design rules that make this reliable:
 *
 *  - **No clock access.** `today` and `now` are inputs. The same input always produces the
 *    same output, which is what makes every edge case unit-testable without a clock shim.
 *  - **Schedule-aware.** The walk steps through *expected* days from [Schedule], not raw
 *    calendar days, so a weekdays-only habit does not break every Saturday.
 *  - **Today is never a break.** An unsatisfied day only breaks the run once its window has
 *    actually closed. An open day reads as [StreakStatus.ACTIVE] or [StreakStatus.AT_RISK].
 *  - **History has a beginning.** Days before a habit's first completion are not misses.
 *  - **A loss is attributed to the day the run actually died**, not to the most recent day.
 *    If the app is not opened for a week, the loss still reports the right date and the right
 *    final count instead of silently evaporating.
 *  - **Shields are read, never spent.** The engine reports what a shield *would* cover and
 *    what has *already* been committed. Consuming a shield is a separate, idempotent write,
 *    so re-evaluating can never double-spend.
 */
object StreakEngine {

    /** How far back to walk. Generous enough for any real run, bounded for safety. */
    private const val MAX_LOOKBACK_EXPECTED_DAYS = 3_650

    fun evaluate(input: StreakInput): StreakResult {
        val satisfied = satisfiedDays(input)
        val expectedToday = input.schedule.isExpectedOn(input.today)
        val closesAt = if (expectedToday) input.schedule.closesAt(input.today, input.zone) else null

        if (satisfied.isEmpty()) {
            return StreakResult.empty(input.habitId).copy(
                isExpectedToday = expectedToday,
                nextDueDate = nextDue(input, complete = false),
                closesAt = closesAt,
            )
        }

        val today = input.today
        val completeToday = today in satisfied
        val firstSatisfied = satisfied.min()
        val todayContribution = if (completeToday) 1 else 0

        // The most recent day that could already have broken the run. Today is excluded
        // because its window may still be open.
        val lastClosedDay = input.schedule.expectedOnOrBefore(
            if (expectedToday) today.minusDays(1) else today,
        )

        // No closed expected day within reach, or the habit did not exist yet: nothing can
        // have been missed, so the run is simply whatever today contributes.
        if (lastClosedDay == null || lastClosedDay.isBefore(firstSatisfied)) {
            val run = todayContribution
            return finish(
                input = input,
                run = run,
                satisfied = satisfied,
                status = when {
                    run == 0 -> StreakStatus.NOT_STARTED
                    else -> liveStatus(input, expectedToday, completeToday, shieldHoldsTail = false)
                },
                completeToday = completeToday,
                expectedToday = expectedToday,
                lastQualifying = if (completeToday) today else null,
                closesAt = closesAt,
                appliedShields = emptyList(),
            )
        }

        val tail = mostRecentHeldDay(input, satisfied, lastClosedDay, firstSatisfied)

        if (tail != null && tail == lastClosedDay) {
            // The run is intact: the most recent closed day is satisfied or shielded.
            val counted = countRunEndingAt(input, satisfied, lastClosedDay, firstSatisfied)
            val run = counted.length + todayContribution
            val shieldHoldsTail = !satisfied.contains(lastClosedDay)
            return finish(
                input = input,
                run = run,
                satisfied = satisfied,
                status = liveStatus(input, expectedToday, completeToday, shieldHoldsTail),
                completeToday = completeToday,
                expectedToday = expectedToday,
                lastQualifying = if (completeToday) today else counted.lastQualifying,
                closesAt = closesAt,
                appliedShields = counted.shields,
            )
        }

        // The run died. Attribute the loss to the first expected day the user actually
        // missed, which is the day after the last day they held.
        val missedDate = if (tail == null) {
            firstExpectedOnOrAfter(input, firstSatisfied) ?: lastClosedDay
        } else {
            input.schedule.nextExpectedAfter(tail)
        }
        val atStake = if (tail == null) {
            0
        } else {
            countRunEndingAt(input, satisfied, tail, firstSatisfied).length
        }

        val deadline = repairDeadline(input, missedDate)
        val stillRepairable = atStake > 0 && deadline.isAfter(input.now)

        if (stillRepairable) {
            return finish(
                input = input,
                run = atStake,
                satisfied = satisfied,
                status = StreakStatus.REPAIRABLE,
                completeToday = completeToday,
                expectedToday = expectedToday,
                lastQualifying = tail,
                closesAt = closesAt,
                appliedShields = emptyList(),
                repairOffer = RepairOffer(
                    habitId = input.habitId,
                    missedDate = missedDate,
                    streakAtStake = atStake,
                    expiresAt = deadline,
                    shieldsAvailable = input.shields.available,
                ),
            )
        }

        // Unrecoverable. Any completion today starts a fresh run.
        val identity = "${input.habitId}:$missedDate"
        val loss = if (atStake > 0 && identity !in input.recordedLosses) {
            StreakLossRecord(
                habitId = input.habitId,
                finalStreak = atStake,
                missedDate = missedDate,
                lastQualifyingDate = tail,
                expiredAt = deadline,
                shieldsAvailableAtExpiry = input.shields.available,
            )
        } else {
            null
        }

        return finish(
            input = input,
            run = todayContribution,
            satisfied = satisfied,
            status = if (todayContribution > 0) {
                liveStatus(input, expectedToday, completeToday, shieldHoldsTail = false)
            } else {
                StreakStatus.BROKEN
            },
            completeToday = completeToday,
            expectedToday = expectedToday,
            lastQualifying = if (completeToday) today else null,
            closesAt = closesAt,
            appliedShields = emptyList(),
            pendingLoss = loss,
        )
    }

    private fun finish(
        input: StreakInput,
        run: Int,
        satisfied: Set<LocalDate>,
        status: StreakStatus,
        completeToday: Boolean,
        expectedToday: Boolean,
        lastQualifying: LocalDate?,
        closesAt: Instant?,
        appliedShields: List<AppliedShield>,
        repairOffer: RepairOffer? = null,
        pendingLoss: StreakLossRecord? = null,
    ): StreakResult = StreakResult(
        habitId = input.habitId,
        currentStreak = run,
        longestStreak = maxOf(longestRun(input, satisfied), run),
        status = status,
        isCompleteToday = completeToday,
        isExpectedToday = expectedToday,
        lastQualifyingDate = lastQualifying,
        nextDueDate = nextDue(input, completeToday),
        appliedShields = appliedShields,
        repairOffer = repairOffer,
        closesAt = closesAt,
        shieldsEarned = shieldsEarnedBy(run, input.config),
        pendingLoss = pendingLoss,
    )

    /** Status for a run that is currently alive. */
    private fun liveStatus(
        input: StreakInput,
        expectedToday: Boolean,
        completeToday: Boolean,
        shieldHoldsTail: Boolean,
    ): StreakStatus {
        if (shieldHoldsTail && !completeToday) return StreakStatus.FROZEN
        if (expectedToday && !completeToday) {
            val closes = input.schedule.closesAt(input.today, input.zone)
            if (!input.now.isBefore(closes.minus(input.config.atRiskLeadTime))) {
                return StreakStatus.AT_RISK
            }
        }
        return StreakStatus.ACTIVE
    }

    /**
     * Days the habit is genuinely held: real completions plus partner-gifted swap days.
     * Shields are excluded — they bridge without counting, and are applied during the walk.
     */
    private fun satisfiedDays(input: StreakInput): Set<LocalDate> {
        if (input.completions.isEmpty() && input.swaps.isEmpty()) return emptySet()
        val out = HashSet<LocalDate>(input.completions.size + input.swaps.size)
        input.completions.forEach { if (it.habitId == input.habitId) out += it.localDate }
        out += input.swaps
        return out
    }

    /**
     * The most recent expected day at or before [from] that the user holds, either by
     * completing it or by a committed shield. Null when nothing in range is held.
     */
    private fun mostRecentHeldDay(
        input: StreakInput,
        satisfied: Set<LocalDate>,
        from: LocalDate,
        floor: LocalDate,
    ): LocalDate? {
        val days = input.schedule.expectedDaysBackFrom(from, MAX_LOOKBACK_EXPECTED_DAYS)
        for (day in days) {
            if (day.isBefore(floor)) return null
            if (day in satisfied || input.shields.isShielded(input.habitId, day)) return day
        }
        return null
    }

    private data class Counted(
        val length: Int,
        val lastQualifying: LocalDate?,
        val shields: List<AppliedShield>,
    )

    /**
     * Length of the run ending at [end], walking backwards over expected days. Satisfied days
     * increment; shielded days bridge without incrementing; anything else ends the run, as
     * does reaching back past [floor], the habit's first completion.
     */
    private fun countRunEndingAt(
        input: StreakInput,
        satisfied: Set<LocalDate>,
        end: LocalDate,
        floor: LocalDate,
    ): Counted {
        var length = 0
        var lastQualifying: LocalDate? = null
        val shields = mutableListOf<AppliedShield>()

        val days = input.schedule.expectedDaysBackFrom(end, MAX_LOOKBACK_EXPECTED_DAYS)
        for (day in days) {
            if (day.isBefore(floor)) break
            when {
                day in satisfied -> {
                    length++
                    if (lastQualifying == null) lastQualifying = day
                }

                input.shields.isShielded(input.habitId, day) -> {
                    shields += AppliedShield(
                        habitId = input.habitId,
                        coveredDate = day,
                        committedAt = input.now,
                    )
                }

                else -> break
            }
        }
        return Counted(length, lastQualifying, shields)
    }

    private fun firstExpectedOnOrAfter(input: StreakInput, from: LocalDate): LocalDate? {
        var cursor = from
        repeat(14) {
            if (input.schedule.isExpectedOn(cursor)) return cursor
            cursor = cursor.plusDays(1)
        }
        return null
    }

    /** The instant a missed day stops being repairable. */
    private fun repairDeadline(input: StreakInput, missedDate: LocalDate): Instant =
        input.schedule.closesAt(missedDate, input.zone).plus(input.config.repairWindow)

    /**
     * The longest run ever recorded, independent of whether it ends today. Replaces the two
     * duplicate "best streak" implementations that previously lived in
     * `WrappedInsightsEngine` and `MotivationalEngine`.
     */
    private fun longestRun(input: StreakInput, satisfied: Set<LocalDate>): Int {
        if (satisfied.isEmpty()) return 0
        val sorted = satisfied.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val day = sorted[i]
            // Consecutive means "no expected day was missed in between", not "yesterday",
            // so scheduled off-days do not end a run.
            current = if (missedExpectedDayBetween(input, prev, day)) 1 else current + 1
            if (current > best) best = current
        }
        return best
    }

    private fun missedExpectedDayBetween(
        input: StreakInput,
        exclusiveStart: LocalDate,
        exclusiveEnd: LocalDate,
    ): Boolean {
        var cursor = exclusiveStart.plusDays(1)
        var guard = 0
        while (cursor.isBefore(exclusiveEnd) && guard < 400) {
            if (input.schedule.isExpectedOn(cursor) &&
                !input.shields.isShielded(input.habitId, cursor)
            ) {
                return true
            }
            cursor = cursor.plusDays(1)
            guard++
        }
        return false
    }

    private fun nextDue(input: StreakInput, complete: Boolean): LocalDate {
        val today = input.today
        val expectedToday = input.schedule.isExpectedOn(today)
        return if (expectedToday && !complete) today else input.schedule.nextExpectedAfter(today)
    }

    /**
     * Shields a run of [runLength] has earned in total. Callers diff this against what has
     * already been credited, so re-evaluating the same run never grants extra shields.
     */
    fun shieldsEarnedBy(runLength: Int, config: StreakConfig): Int {
        if (runLength <= 0) return 0
        return runLength / config.shieldEarnIntervalDays
    }

    /**
     * Whether the at-risk reminder should escalate from soft to urgent. Kept here so the
     * notification scheduler reads the same thresholds the UI does.
     */
    fun isUrgent(result: StreakResult, now: Instant, config: StreakConfig): Boolean {
        val closes = result.closesAt ?: return false
        return !now.isBefore(closes.minus(config.urgentLeadTime))
    }
}
