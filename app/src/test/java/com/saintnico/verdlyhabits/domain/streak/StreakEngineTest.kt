package com.saintnico.verdlyhabits.domain.streak

import com.saintnico.verdlyhabits.domain.HabitFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The engine takes its clock as an input, so every case here is a plain deterministic
 * assertion with no clock mocking, no Robolectric, and no flakiness.
 */
class StreakEngineTest {

    private val habitId = "habit-1"
    private val nairobi: ZoneId = ZoneId.of("Africa/Nairobi")

    // A Tuesday, so weekday/weekend cases are unambiguous.
    private val today: LocalDate = LocalDate.of(2026, 3, 10)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0, zone: ZoneId = nairobi): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    private fun completion(
        date: LocalDate,
        source: CompletionSource = CompletionSource.MANUAL,
        zone: ZoneId = nairobi,
    ) = CompletionRecord(
        habitId = habitId,
        localDate = date,
        completedAt = at(date, 9, zone = zone),
        zoneId = zone.id,
        offsetSeconds = zone.rules.getOffset(at(date, 9, zone = zone)).totalSeconds,
        source = source,
    )

    private fun input(
        dates: List<LocalDate>,
        today: LocalDate = this.today,
        now: Instant = at(this.today, 10),
        schedule: Schedule = Schedule.DAILY,
        shields: ShieldLedger = ShieldLedger(),
        swaps: Set<LocalDate> = emptySet(),
        recordedLosses: Set<String> = emptySet(),
        config: StreakConfig = StreakConfig.DEFAULT,
        zone: ZoneId = nairobi,
    ) = StreakInput(
        habitId = habitId,
        completions = dates.map { completion(it, zone = zone) },
        schedule = schedule,
        today = today,
        now = now,
        zone = zone,
        shields = shields,
        config = config,
        swaps = swaps,
        recordedLosses = recordedLosses,
    )

    private fun days(vararg offsetsBack: Int): List<LocalDate> =
        offsetsBack.map { today.minusDays(it.toLong()) }

    // ── Normal consecutive completion ─────────────────────────────────────────

    @Test
    fun `consecutive days including today produce an active run`() {
        val result = StreakEngine.evaluate(input(days(0, 1, 2, 3, 4)))

        assertEquals(5, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertTrue(result.isCompleteToday)
        assertTrue(result.isAlive)
        assertEquals(today, result.lastQualifyingDate)
        assertNull(result.pendingLoss)
    }

    @Test
    fun `run stays alive before today is completed`() {
        // Yesterday done, today still open and not yet urgent.
        val result = StreakEngine.evaluate(input(days(1, 2, 3), now = at(today, 9)))

        assertEquals(3, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertFalse(result.isCompleteToday)
        assertEquals(today, result.nextDueDate)
    }

    @Test
    fun `first ever completion is not treated as a missed history`() {
        // A brand new habit completed once today must read as a 1-day run, not a break.
        val result = StreakEngine.evaluate(input(days(0)))

        assertEquals(1, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertNull(result.pendingLoss)
    }

    @Test
    fun `no completions reads as not started`() {
        val result = StreakEngine.evaluate(input(emptyList()))

        assertEquals(0, result.currentStreak)
        assertEquals(StreakStatus.NOT_STARTED, result.status)
    }

    @Test
    fun `longest run is remembered after a later break`() {
        // A 10-day run long ago, then a fresh 2-day run including today.
        val old = (20..29).map { today.minusDays(it.toLong()) }
        val recent = days(0, 1)
        val result = StreakEngine.evaluate(input(old + recent))

        assertEquals(2, result.currentStreak)
        assertEquals(10, result.longestStreak)
    }

    // ── Missed day without a shield breaks ────────────────────────────────────

    @Test
    fun `missed day with no shield and expired window breaks the run`() {
        // Ran 3 days, then missed 4 days ago onwards; well past the 48h repair window.
        val result = StreakEngine.evaluate(input(days(4, 5, 6)))

        assertEquals(StreakStatus.BROKEN, result.status)
        assertEquals(0, result.currentStreak)
        assertFalse(result.isAlive)
    }

    @Test
    fun `missed day inside the repair window is repairable not broken`() {
        // Missed yesterday, so the streak is held rather than destroyed.
        val result = StreakEngine.evaluate(input(days(1 + 1, 3, 4), now = at(today, 10)))

        // Completions on days -2,-3,-4; yesterday (-1) missed.
        assertEquals(StreakStatus.REPAIRABLE, result.status)
        assertNotNull(result.repairOffer)
        assertEquals(today.minusDays(1), result.repairOffer!!.missedDate)
        assertEquals(3, result.repairOffer!!.streakAtStake)
        assertEquals(3, result.currentStreak)
        assertTrue(result.isAlive)
    }

    @Test
    fun `repair offer knows whether the user can act on it`() {
        val withNone = StreakEngine.evaluate(input(days(2, 3, 4)))
        assertFalse(withNone.repairOffer!!.canRepairNow)

        val withOne = StreakEngine.evaluate(
            input(days(2, 3, 4), shields = ShieldLedger(available = 1)),
        )
        assertTrue(withOne.repairOffer!!.canRepairNow)
    }

    // ── Missed day with a shield available preserves the run ──────────────────

    @Test
    fun `committed shield bridges a missed day and preserves the run`() {
        val missed = today.minusDays(1)
        val result = StreakEngine.evaluate(
            input(
                dates = days(2, 3, 4),
                shields = ShieldLedger(available = 1, spentOn = setOf("$habitId:$missed")),
            ),
        )

        assertEquals(StreakStatus.FROZEN, result.status)
        assertTrue(result.wasShielded)
        assertTrue(result.isAlive)
        assertNull(result.repairOffer)
        assertNull(result.pendingLoss)
    }

    @Test
    fun `shield preserves without incrementing the run`() {
        val missed = today.minusDays(1)
        val shielded = ShieldLedger(available = 0, spentOn = setOf("$habitId:$missed"))

        // Three real completions before the gap, plus today completed.
        val result = StreakEngine.evaluate(
            input(dates = days(0, 2, 3, 4), shields = shielded),
        )

        // 3 before the gap + today = 4. The bridged day itself contributes nothing.
        assertEquals(4, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
    }

    @Test
    fun `shields are earned per configured interval and never double granted`() {
        val config = StreakConfig.DEFAULT
        assertEquals(0, StreakEngine.shieldsEarnedBy(6, config))
        assertEquals(1, StreakEngine.shieldsEarnedBy(7, config))
        assertEquals(1, StreakEngine.shieldsEarnedBy(13, config))
        assertEquals(2, StreakEngine.shieldsEarnedBy(14, config))
        // Reported as a cumulative total so callers diff rather than re-award.
        assertEquals(5, StreakEngine.shieldsEarnedBy(35, config))
    }

    @Test
    fun `shield ledger reports cap state`() {
        val config = StreakConfig.DEFAULT
        assertTrue(ShieldLedger(available = 5).atCap(config))
        assertFalse(ShieldLedger(available = 4).atCap(config))
        assertEquals(2, ShieldLedger(available = 3).roomToHold(config))
        assertEquals(0, ShieldLedger(available = 9).roomToHold(config))
    }

    // ── Streak loss moment ────────────────────────────────────────────────────

    @Test
    fun `loss is emitted once when the repair window expires`() {
        val result = StreakEngine.evaluate(input(days(4, 5, 6)))

        val loss = result.pendingLoss
        assertNotNull(loss)
        assertEquals(3, loss!!.finalStreak)
        assertEquals(today.minusDays(3), loss.missedDate)
        assertFalse(loss.acknowledged)
    }

    @Test
    fun `loss is not re-emitted once recorded`() {
        val first = StreakEngine.evaluate(input(days(4, 5, 6)))
        val identity = first.pendingLoss!!.identity

        val second = StreakEngine.evaluate(
            input(days(4, 5, 6), recordedLosses = setOf(identity)),
        )

        assertNull(second.pendingLoss)
        assertEquals(StreakStatus.BROKEN, second.status)
    }

    @Test
    fun `loss captures that no shield was held so the offer can be specific`() {
        val result = StreakEngine.evaluate(input(days(4, 5, 6), shields = ShieldLedger(available = 0)))

        val loss = result.pendingLoss!!
        assertEquals(0, loss.shieldsAvailableAtExpiry)
        assertTrue(loss.wasPreventableWithShield)
    }

    @Test
    fun `loss records when a shield was held and simply not used`() {
        val result = StreakEngine.evaluate(input(days(4, 5, 6), shields = ShieldLedger(available = 2)))

        val loss = result.pendingLoss!!
        assertEquals(2, loss.shieldsAvailableAtExpiry)
        // The user could have saved it, so claiming a shield would have helped is untrue.
        assertFalse(loss.wasPreventableWithShield)
    }

    @Test
    fun `long absence still attributes the loss to the day the run actually died`() {
        // A 40-day run that ended 30 days ago, with the app never opened since.
        val start = today.minusDays(70)
        val dates = (0 until 40).map { start.plusDays(it.toLong()) }
        val diedOn = dates.last().plusDays(1)

        val result = StreakEngine.evaluate(input(dates))

        val loss = result.pendingLoss
        assertNotNull(loss)
        assertEquals(40, loss!!.finalStreak)
        assertEquals(diedOn, loss.missedDate)
        assertEquals(StreakStatus.BROKEN, result.status)
        assertEquals(40, result.longestStreak)
    }

    @Test
    fun `trivial runs are not worth surfacing as a loss moment`() {
        val big = StreakLossRecord(habitId, 40, today, null, at(today, 1), 0)
        val tiny = StreakLossRecord(habitId, 2, today, null, at(today, 1), 0)

        assertTrue(big.isWorthSurfacing())
        assertFalse(tiny.isWorthSurfacing())
    }

    @Test
    fun `completing after an unrecoverable break starts a fresh run`() {
        // Old run died long ago, but the user completed today.
        val old = (10..14).map { today.minusDays(it.toLong()) }
        val result = StreakEngine.evaluate(input(old + days(0)))

        assertEquals(1, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertEquals(5, result.longestStreak)
    }

    // ── At-risk and the escalation threshold ──────────────────────────────────

    @Test
    fun `run reads as at risk inside the lead time`() {
        // Default lead time is 4h before midnight, so 20:30 is inside it.
        val result = StreakEngine.evaluate(input(days(1, 2, 3), now = at(today, 20, 30)))

        assertEquals(StreakStatus.AT_RISK, result.status)
        assertTrue(result.isAtRisk)
        assertTrue(result.isAlive)
    }

    @Test
    fun `run is not at risk well before the lead time`() {
        val result = StreakEngine.evaluate(input(days(1, 2, 3), now = at(today, 12)))

        assertEquals(StreakStatus.ACTIVE, result.status)
        assertFalse(result.isAtRisk)
    }

    @Test
    fun `at risk applies regardless of how long the run is`() {
        // The old implementation only warned when the streak exceeded seven days.
        val result = StreakEngine.evaluate(input(days(1), now = at(today, 21)))

        assertEquals(StreakStatus.AT_RISK, result.status)
    }

    @Test
    fun `escalates to urgent inside the urgent lead time`() {
        val soft = StreakEngine.evaluate(input(days(1, 2), now = at(today, 20)))
        val urgent = StreakEngine.evaluate(input(days(1, 2), now = at(today, 23, 30)))

        assertFalse(StreakEngine.isUrgent(soft, at(today, 20), StreakConfig.DEFAULT))
        assertTrue(StreakEngine.isUrgent(urgent, at(today, 23, 30), StreakConfig.DEFAULT))
    }

    @Test
    fun `at risk honours an explicit completion window`() {
        val schedule = Schedule(windowStart = LocalTime.of(6, 0), windowEnd = LocalTime.of(9, 0))
        // Window closes at 09:00, so 06:00 is already inside the 4h lead time.
        val result = StreakEngine.evaluate(
            input(days(1, 2), now = at(today, 6), schedule = schedule),
        )

        assertEquals(StreakStatus.AT_RISK, result.status)
        assertEquals(at(today, 9), result.closesAt)
    }

    // ── Midnight boundary ─────────────────────────────────────────────────────

    @Test
    fun `completion one minute before midnight counts for that day`() {
        val result = StreakEngine.evaluate(input(days(0, 1), now = at(today, 23, 59)))

        assertTrue(result.isCompleteToday)
        assertEquals(2, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
    }

    @Test
    fun `just after midnight the previous run is intact and today is newly open`() {
        val newDay = today.plusDays(1)
        // Completed through today; it is now 00:01 on the next day.
        val result = StreakEngine.evaluate(
            input(days(0, 1, 2), today = newDay, now = at(newDay, 0, 1)),
        )

        assertFalse(result.isCompleteToday)
        assertEquals(3, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertEquals(newDay, result.nextDueDate)
    }

    @Test
    fun `an open day never breaks the run no matter the hour`() {
        val result = StreakEngine.evaluate(input(days(1, 2, 3), now = at(today, 23, 59)))

        assertTrue(result.isAlive)
        assertEquals(StreakStatus.AT_RISK, result.status)
        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `day closes at midnight of the following day`() {
        val result = StreakEngine.evaluate(input(days(0)))

        assertEquals(today.plusDays(1).atStartOfDay(nairobi).toInstant(), result.closesAt)
    }

    // ── Schedule awareness: the weekend false-break bug ───────────────────────

    @Test
    fun `weekday habit does not break over the weekend`() {
        val schedule = Schedule(frequency = HabitFrequency.WEEKDAYS)
        // Monday 9 Mar 2026; the run covers the preceding two weeks of weekdays only.
        val monday = LocalDate.of(2026, 3, 9)
        val weekdays = generateSequence(monday) { it.minusDays(1) }
            .filter { it.dayOfWeek.value <= 5 }
            .take(10)
            .toList()

        val result = StreakEngine.evaluate(
            input(dates = weekdays, today = monday, now = at(monday, 12), schedule = schedule),
        )

        // Ten weekdays spanning two weekends, none of which may break the run.
        assertEquals(10, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertNull(result.pendingLoss)
    }

    @Test
    fun `weekday habit is not expected on a Saturday`() {
        val schedule = Schedule(frequency = HabitFrequency.WEEKDAYS)
        val saturday = LocalDate.of(2026, 3, 14)
        val friday = saturday.minusDays(1)

        val result = StreakEngine.evaluate(
            input(dates = listOf(friday), today = saturday, now = at(saturday, 22), schedule = schedule),
        )

        assertFalse(result.isExpectedToday)
        // Not expected today, so a late hour cannot make it at-risk.
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertNull(result.closesAt)
    }

    @Test
    fun `custom schedule only requires its masked days`() {
        // Mon, Wed, Fri only.
        val schedule = Schedule(frequency = HabitFrequency.CUSTOM, customDaysMask = "1010100")
        val friday = LocalDate.of(2026, 3, 13)
        val dates = listOf(
            LocalDate.of(2026, 3, 13), // Fri
            LocalDate.of(2026, 3, 11), // Wed
            LocalDate.of(2026, 3, 9), // Mon
            LocalDate.of(2026, 3, 6), // Fri
        )

        val result = StreakEngine.evaluate(
            input(dates = dates, today = friday, now = at(friday, 12), schedule = schedule),
        )

        assertEquals(4, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
    }

    @Test
    fun `completing on an unscheduled day still counts rather than being discarded`() {
        val schedule = Schedule(frequency = HabitFrequency.WEEKDAYS)
        val saturday = LocalDate.of(2026, 3, 14)
        val result = StreakEngine.evaluate(
            input(
                dates = listOf(saturday, saturday.minusDays(1)),
                today = saturday,
                now = at(saturday, 12),
                schedule = schedule,
            ),
        )

        assertTrue(result.isCompleteToday)
        assertEquals(2, result.currentStreak)
    }

    // ── Duo swap-a-day ────────────────────────────────────────────────────────

    @Test
    fun `a swapped day counts as complete without a proof`() {
        val gifted = today.minusDays(1)
        val result = StreakEngine.evaluate(
            input(dates = days(0, 2, 3), swaps = setOf(gifted)),
        )

        // Yesterday was gifted, so the run is unbroken: 3 completions + the gift.
        assertEquals(4, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertNull(result.repairOffer)
    }

    @Test
    fun `a swap covering today marks today complete`() {
        val result = StreakEngine.evaluate(
            input(dates = days(1, 2), swaps = setOf(today), now = at(today, 23)),
        )

        assertTrue(result.isCompleteToday)
        assertEquals(3, result.currentStreak)
        // A gifted today cannot be at risk.
        assertEquals(StreakStatus.ACTIVE, result.status)
    }

    @Test
    fun `swap differs from shield by actually incrementing the run`() {
        val gap = today.minusDays(1)

        val swapped = StreakEngine.evaluate(input(dates = days(2, 3), swaps = setOf(gap)))
        val shielded = StreakEngine.evaluate(
            input(dates = days(2, 3), shields = ShieldLedger(spentOn = setOf("$habitId:$gap"))),
        )

        // Same history, but the swap counts as a day held and the shield only bridges.
        assertEquals(3, swapped.currentStreak)
        assertEquals(2, shielded.currentStreak)
    }

    // ── Offline completion synced late ────────────────────────────────────────

    @Test
    fun `a completion synced late still lands on the day it was earned`() {
        // Recorded while offline on the 8th, uploaded on the 10th. The stored localDate is
        // authoritative, so the run is continuous rather than showing a hole on the 8th.
        val offlineDay = today.minusDays(2)
        val late = CompletionRecord(
            habitId = habitId,
            localDate = offlineDay,
            completedAt = at(offlineDay, 21),
            zoneId = nairobi.id,
            offsetSeconds = 10_800,
            source = CompletionSource.MANUAL,
        )
        val others = days(0, 1).map { completion(it) }

        val result = StreakEngine.evaluate(
            StreakInput(
                habitId = habitId,
                completions = others + late,
                today = today,
                now = at(today, 10),
                zone = nairobi,
            ),
        )

        assertEquals(3, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertNull(result.pendingLoss)
    }

    @Test
    fun `duplicate records for the same day are counted once`() {
        val dupe = completion(today)
        val result = StreakEngine.evaluate(
            StreakInput(
                habitId = habitId,
                // The same day arriving twice from two devices must not inflate the run.
                completions = listOf(dupe, dupe.copy(completedAt = at(today, 18)), completion(today.minusDays(1))),
                today = today,
                now = at(today, 20),
                zone = nairobi,
            ),
        )

        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `records belonging to another habit are ignored`() {
        val foreign = completion(today).copy(habitId = "someone-else")
        val result = StreakEngine.evaluate(
            StreakInput(
                habitId = habitId,
                completions = listOf(foreign),
                today = today,
                now = at(today, 10),
                zone = nairobi,
            ),
        )

        assertEquals(0, result.currentStreak)
        assertEquals(StreakStatus.NOT_STARTED, result.status)
    }

    // ── Timezone travel ──────────────────────────────────────────────────────

    @Test
    fun `travelling east does not retroactively break a run`() {
        // Completed in Nairobi, then the user flies to Tokyo. The stored localDate is locked
        // to the zone at completion time, so the boundary cannot shift underneath them.
        val tokyo = ZoneId.of("Asia/Tokyo")
        val dates = days(0, 1, 2)
        val result = StreakEngine.evaluate(
            StreakInput(
                habitId = habitId,
                completions = dates.map { completion(it, zone = nairobi) },
                today = today,
                now = today.atTime(9, 0).atZone(tokyo).toInstant(),
                zone = tokyo,
            ),
        )

        assertEquals(3, result.currentStreak)
        assertEquals(StreakStatus.ACTIVE, result.status)
        assertNull(result.pendingLoss)
    }

    @Test
    fun `travelling west does not double count a day`() {
        val la = ZoneId.of("America/Los_Angeles")
        val dates = days(0, 1, 2)
        val result = StreakEngine.evaluate(
            StreakInput(
                habitId = habitId,
                completions = dates.map { completion(it, zone = nairobi) },
                today = today,
                now = today.atTime(23, 0).atZone(la).toInstant(),
                zone = la,
            ),
        )

        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `completion record keeps the zone it was earned in`() {
        val record = completion(today, zone = nairobi)

        assertEquals("Africa/Nairobi", record.zoneId)
        assertEquals(10_800, record.offsetSeconds)
        assertEquals("$habitId:$today", record.identity)
    }

    // ── Configurability ──────────────────────────────────────────────────────

    @Test
    fun `repair window length is configurable`() {
        val shortWindow = StreakConfig(repairWindow = Duration.ofHours(1))
        // Missed yesterday; with a 1h window the chance is already gone by 10:00 today.
        val result = StreakEngine.evaluate(
            input(days(2, 3, 4), config = shortWindow, now = at(today, 10)),
        )

        assertEquals(StreakStatus.BROKEN, result.status)
        assertNotNull(result.pendingLoss)
    }

    @Test
    fun `at risk lead time is configurable`() {
        val wide = StreakConfig(atRiskLeadTime = Duration.ofHours(12))
        val result = StreakEngine.evaluate(input(days(1, 2), config = wide, now = at(today, 13)))

        assertEquals(StreakStatus.AT_RISK, result.status)
    }

    @Test
    fun `config rejects contradictory lead times`() {
        val error = runCatching {
            StreakConfig(atRiskLeadTime = Duration.ofHours(1), urgentLeadTime = Duration.ofHours(5))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
