package com.saintnico.verdlyhabits.engine.grove

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroveCalendarTest {

    @Test
    fun weekKeyStartsOnMonday() {
        assertEquals("2026-09-07", GroveCalendar.weekKey(LocalDate.of(2026, 9, 9)))
        assertEquals("2026-09-07", GroveCalendar.weekKey(LocalDate.of(2026, 9, 7)))
        assertEquals("2026-09-07", GroveCalendar.weekKey(LocalDate.of(2026, 9, 13)))
        assertEquals("2026-09-14", GroveCalendar.weekKey(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun nextPresenceIncrementsSameWeekNewDay() {
        val next = GroveCalendar.nextPresence(
            previousDay = "2026-09-08",
            previousCountToday = 2,
            previousWeeklyDays = 2,
            previousWeekKey = "2026-09-07",
            currentStreakPublic = 4,
            nowMillis = 1L,
            today = LocalDate.of(2026, 9, 9),
        )
        assertEquals("2026-09-09", next.lastPlantedDay)
        assertEquals(1, next.plantedCountToday)
        assertEquals(3, next.weeklyPlantDays)
        assertEquals("2026-09-07", next.weeklyPlantWeekKey)
    }

    @Test
    fun nextPresenceDoesNotDoubleCountSameDay() {
        val next = GroveCalendar.nextPresence(
            previousDay = "2026-09-09",
            previousCountToday = 1,
            previousWeeklyDays = 3,
            previousWeekKey = "2026-09-07",
            currentStreakPublic = 4,
            nowMillis = 1L,
            today = LocalDate.of(2026, 9, 9),
        )
        assertEquals(2, next.plantedCountToday)
        assertEquals(3, next.weeklyPlantDays)
    }

    @Test
    fun nextPresenceResetsOnNewWeek() {
        val next = GroveCalendar.nextPresence(
            previousDay = "2026-09-13",
            previousCountToday = 1,
            previousWeeklyDays = 6,
            previousWeekKey = "2026-09-07",
            currentStreakPublic = 8,
            nowMillis = 1L,
            today = LocalDate.of(2026, 9, 14),
        )
        assertEquals(1, next.weeklyPlantDays)
        assertEquals("2026-09-14", next.weeklyPlantWeekKey)
    }

    @Test
    fun passedFiresOnlyWhenCrossingAhead() {
        assertTrue(GroveCalendar.passed(2, 3, 4, 3, 3, 10))
        assertFalse(GroveCalendar.passed(4, 3, 5, 3, 3, 10))
        assertFalse(GroveCalendar.passed(2, 3, 2, 3, 3, 10))
        assertFalse(GroveCalendar.passed(0, 0, 1, 1, 0, 0))
    }
}
