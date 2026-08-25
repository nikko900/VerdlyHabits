package com.saintnico.verdlyhabits.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/**
 * Duo streak rules: cooperative daily completion, milestones, grace windows.
 */
object DuoStreakEngine {

    val CELEBRATION_MILESTONES = listOf(3, 7, 14, 21, 30)

    fun milestoneXp(streakDays: Int): Int = when (streakDays) {
        3 -> 25
        7 -> 100
        14 -> 150
        21 -> 200
        30 -> 500
        else -> 0
    }

    fun milestoneTitle(streakDays: Int): String = when (streakDays) {
        3 -> "3-Day Spark"
        7 -> "Week Warriors"
        14 -> "Fortnight Force"
        21 -> "Habit Architects"
        30 -> "Monthly Legends"
        else -> "$streakDays-Day Duo"
    }

    fun milestoneSubtitle(streakDays: Int, buddyName: String): String = when (streakDays) {
        3 -> "You and $buddyName showed up three days straight."
        7 -> "A full week of showing up together."
        14 -> "Two weeks. Most pairs quit by now — not you."
        21 -> "21 days — habits are forming for real."
        30 -> "A month of mutual accountability. Elite."
        else -> "Keep building with $buddyName."
    }

    fun isoWeekKey(date: LocalDate = LocalDate.now()): String =
        "${date.year}-W${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    fun todayKey(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun yesterdayKey(): String = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** True when the pair missed completing together yesterday (streak at risk or broken). */
    fun missedYesterday(lastBothCompleteDate: String?): Boolean {
        if (lastBothCompleteDate.isNullOrBlank()) return false
        val last = runCatching { LocalDate.parse(lastBothCompleteDate) }.getOrNull() ?: return false
        val yesterday = LocalDate.now().minusDays(1)
        return last.isBefore(yesterday)
    }

    /** Streak should reset unless grace forgave yesterday. */
    fun shouldResetStreak(
        lastBothCompleteDate: String?,
        graceForgivenDate: String?,
        streakDays: Int,
    ): Boolean {
        if (streakDays <= 0) return false
        if (!missedYesterday(lastBothCompleteDate)) return false
        return graceForgivenDate != yesterdayKey()
    }

    fun graceAvailableThisWeek(
        graceWeekKey: String?,
        isPremium: Boolean,
    ): Boolean = isPremium && graceWeekKey != isoWeekKey()

    fun timeUntilMidnight(): Pair<Int, Int> {
        val now = LocalDateTime.now()
        val midnight = LocalDate.now().plusDays(1).atStartOfDay()
        val mins = Duration.between(now, midnight).toMinutes().coerceAtLeast(0)
        return (mins / 60).toInt() to (mins % 60).toInt()
    }

    fun countdownLabel(hours: Int, minutes: Int): String = when {
        hours <= 0 && minutes <= 0 -> "Midnight — lock in now"
        hours == 0 -> "${minutes}m left to save the streak"
        hours < 5 -> "${hours}h ${minutes}m left"
        else -> "${hours}h left today"
    }

    fun tierLabel(streakDays: Int): String = when {
        streakDays >= 30 -> "Legendary Duo"
        streakDays >= 14 -> "Unstoppable"
        streakDays >= 7 -> "On Fire"
        streakDays >= 3 -> "Heating Up"
        streakDays >= 1 -> "Spark Started"
        else -> "New Pair"
    }

    fun vibeLine(
        streakDays: Int,
        buddyName: String,
        bothDone: Boolean,
        atRisk: Boolean,
        buddyDone: Boolean,
        myDone: Boolean,
    ): String = when {
        atRisk -> "Midnight is creeping in — tag-team this with $buddyName!"
        bothDone -> "Both locked in. Your flame grows at midnight."
        buddyDone && !myDone -> "$buddyName finished — your move!"
        myDone && !buddyDone -> "You're done. Waiting on $buddyName…"
        streakDays >= 14 -> "Two weeks strong. Most pairs quit — not you two."
        streakDays >= 7 -> "A full week together. Keep the flame alive."
        streakDays >= 3 -> "Three days in. The habit is forming."
        else -> "Show up together. Grow the streak."
    }
}
