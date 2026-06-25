package com.saintnico.verdlyhabits.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ChallengeStreakCalculator {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun isDayComplete(dayData: Any?): Boolean = when (dayData) {
        is Boolean -> dayData
        is Map<*, *> -> dayData["completed"] == true
        else -> false
    }

    /**
     * Consecutive completed days ending at [anchorDate] (walks back through gaps).
     * Matches [com.saintnico.verdlyhabits.data.remote.firestore.ChallengeRepository.calculateCurrentStreak].
     */
    fun currentStreak(
        userCompletions: Map<String, Any>,
        anchorDate: String,
        challengeStartMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val startDay = if (challengeStartMillis > 0) {
            Instant.ofEpochMilli(challengeStartMillis).atZone(zone).toLocalDate()
        } else {
            LocalDate.parse(anchorDate, formatter).minusYears(10)
        }

        fun isComplete(day: LocalDate): Boolean {
            val key = day.format(formatter)
            return isDayComplete(userCompletions[key])
        }

        var anchor = LocalDate.parse(anchorDate, formatter)
        while (!isComplete(anchor) && anchor.isAfter(startDay)) {
            anchor = anchor.minusDays(1)
        }
        if (!isComplete(anchor)) return 0

        var streak = 0
        var d = anchor
        while (!d.isBefore(startDay) && isComplete(d)) {
            streak++
            d = d.minusDays(1)
        }
        return streak
    }

    fun anchorDateFor(userCompletions: Map<String, Any>): String {
        val today = LocalDate.now().format(formatter)
        return if (isDayComplete(userCompletions[today])) today else {
            LocalDate.now().minusDays(1).format(formatter)
        }
    }
}
