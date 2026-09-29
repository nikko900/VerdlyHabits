package com.saintnico.verdlyhabits.engine.grove

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Friends Grove extras beyond the spec:
 * - Rival pin: one friend at a time, Home shows planted vs still growing.
 * - Grove Lead frame (`frame_grove_lead`) is earned by weekly #1 among friends — never sold.
 */
object GroveCalendar {
    fun todayKey(today: LocalDate = LocalDate.now()): String = today.toString()

    /** Monday-start ISO date of the current grove week. */
    fun weekKey(today: LocalDate = LocalDate.now()): String =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    fun plantedToday(lastPlantedDay: String?, today: LocalDate = LocalDate.now()): Boolean =
        !lastPlantedDay.isNullOrBlank() && lastPlantedDay == todayKey(today)

    data class PresenceNext(
        val lastPlantedAt: Long,
        val lastPlantedDay: String,
        val plantedCountToday: Int,
        val weeklyPlantDays: Int,
        val weeklyPlantWeekKey: String,
        val currentStreakPublic: Int,
    )

    fun nextPresence(
        previousDay: String?,
        previousCountToday: Int,
        previousWeeklyDays: Int,
        previousWeekKey: String?,
        currentStreakPublic: Int,
        nowMillis: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
    ): PresenceNext {
        val day = todayKey(today)
        val week = weekKey(today)
        val sameDay = previousDay == day
        val sameWeek = previousWeekKey == week
        val plantedCount = if (sameDay) (previousCountToday + 1).coerceAtLeast(1) else 1
        val weeklyDays = when {
            sameDay && sameWeek -> previousWeeklyDays.coerceAtLeast(1)
            sameWeek -> (previousWeeklyDays + 1).coerceAtLeast(1)
            else -> 1
        }
        return PresenceNext(
            lastPlantedAt = nowMillis,
            lastPlantedDay = day,
            plantedCountToday = plantedCount,
            weeklyPlantDays = weeklyDays,
            weeklyPlantWeekKey = week,
            currentStreakPublic = currentStreakPublic.coerceAtLeast(0),
        )
    }

    fun compareShowUp(weeklyA: Int, streakA: Int, weeklyB: Int, streakB: Int): Int {
        val week = weeklyA.compareTo(weeklyB)
        if (week != 0) return week
        return streakA.compareTo(streakB)
    }

    fun passed(previousMineWeekly: Int, previousMineStreak: Int, mineWeekly: Int, mineStreak: Int, theirsWeekly: Int, theirsStreak: Int): Boolean {
        val wasBehindOrTied = compareShowUp(previousMineWeekly, previousMineStreak, theirsWeekly, theirsStreak) <= 0
        val nowAhead = compareShowUp(mineWeekly, mineStreak, theirsWeekly, theirsStreak) > 0
        return wasBehindOrTied && nowAhead && (theirsWeekly > 0 || theirsStreak > 0)
    }
}
