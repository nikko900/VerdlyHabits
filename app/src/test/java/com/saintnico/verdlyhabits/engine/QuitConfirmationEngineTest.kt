package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.ConfirmationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class QuitConfirmationEngineTest {

    private val nairobi: ZoneId = ZoneId.of("Africa/Nairobi")
    private val today: LocalDate = LocalDate.of(2026, 9, 7)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0) =
        date.atTime(hour, minute).atZone(nairobi).toInstant()

    private fun snap(
        start: LocalDate = today.minusDays(10),
        confirmed: LocalDate? = today,
        lapse: LocalDate? = null,
        anchor: LocalDate = start,
        archived: Boolean = false,
    ) = QuitConfirmationEngine.Snapshot(
        startDate = at(start, 9).toEpochMilli(),
        lastLapseAt = lapse?.let { at(it, 12).toEpochMilli() },
        lastConfirmedAt = confirmed?.let { at(it, 9).toEpochMilli() },
        streakAnchorAt = at(anchor, 0).toEpochMilli(),
        isArchived = archived,
    )

    @Test
    fun openingTheAppDoesNotAdvanceDays() {
        val start = today.minusDays(4)
        val lastConfirm = today.minusDays(4)
        val frozen = snap(start = start, confirmed = lastConfirm, anchor = start)
        val daysOnOpen = QuitConfirmationEngine.cleanStreakDays(frozen, nairobi)
        val laterSameSilence = QuitConfirmationEngine.cleanStreakDays(frozen, nairobi)
        assertEquals(1, daysOnOpen)
        assertEquals(daysOnOpen, laterSameSilence)
    }

    @Test
    fun confirmedTodayIsStillClearRegardlessOfEvening() {
        val s = snap(confirmed = today)
        val morning = QuitConfirmationEngine.evaluate(s, at(today, 10), nairobi)
        val night = QuitConfirmationEngine.evaluate(s, at(today, 21), nairobi)
        assertEquals(ConfirmationState.STILL_CLEAR, morning.state)
        assertEquals(ConfirmationState.STILL_CLEAR, night.state)
        assertTrue(morning.confirmedToday)
        assertEquals(11, morning.cleanStreakDays)
    }

    @Test
    fun yesterdayConfirmBeforeRiskTimeIsStillClear() {
        val s = snap(confirmed = today.minusDays(1), start = today.minusDays(5), anchor = today.minusDays(5))
        val eval = QuitConfirmationEngine.evaluate(s, at(today, 19, 0), nairobi)
        assertEquals(ConfirmationState.STILL_CLEAR, eval.state)
        assertEquals(1, eval.silentDays)
        assertFalse(eval.confirmedToday)
        assertEquals(5, eval.cleanStreakDays)
    }

    @Test
    fun yesterdayConfirmAfter2030IsAtRisk() {
        val s = snap(confirmed = today.minusDays(1), start = today.minusDays(5), anchor = today.minusDays(5))
        val eval = QuitConfirmationEngine.evaluate(s, at(today, 20, 30), nairobi)
        assertEquals(ConfirmationState.AT_RISK, eval.state)
        assertEquals(5, eval.cleanStreakDays)
    }

    @Test
    fun twoSilentDaysFreezeAsUnconfirmed() {
        val s = snap(confirmed = today.minusDays(4), start = today.minusDays(10), anchor = today.minusDays(10))
        val eval = QuitConfirmationEngine.evaluate(s, at(today, 11), nairobi)
        assertEquals(ConfirmationState.UNCONFIRMED, eval.state)
        assertEquals(4, eval.silentDays)
        assertEquals(7, eval.cleanStreakDays)
    }

    @Test
    fun slipTodayYieldsZeroDays() {
        val s = snap(
            start = today.minusDays(20),
            confirmed = null,
            lapse = today,
            anchor = today.plusDays(1),
        )
        val eval = QuitConfirmationEngine.evaluate(s, at(today, 16), nairobi)
        assertEquals(0, eval.cleanStreakDays)
        assertEquals(ConfirmationState.STILL_CLEAR, eval.state)
        assertEquals(0, eval.silentDays)
    }

    @Test
    fun graceWindowAppliesTapToYesterday() {
        val last = today.minusDays(2)
        val date = QuitConfirmationEngine.resolveConfirmDate(
            today = today,
            localTime = LocalTime.of(1, 15),
            lastConfirmDate = last,
        )
        assertEquals(today.minusDays(1), date)
    }

    @Test
    fun graceWindowDoesNotStealTodayIfYesterdayAlreadyConfirmed() {
        val date = QuitConfirmationEngine.resolveConfirmDate(
            today = today,
            localTime = LocalTime.of(1, 15),
            lastConfirmDate = today.minusDays(1),
        )
        assertEquals(today, date)
    }

    @Test
    fun catchUpCopyScalesWithGap() {
        val few = QuitConfirmationEngine.catchUpCopy("Smoking", 3)
        val week = QuitConfirmationEngine.catchUpCopy("Smoking", 8)
        val long = QuitConfirmationEngine.catchUpCopy("Smoking", 20)
        assertTrue(few.second.contains("few days"))
        assertTrue(week.first.contains("week"))
        assertTrue(long.first.contains("while"))
    }
}
