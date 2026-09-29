package com.saintnico.verdlyhabits.ui.utils

import com.saintnico.verdlyhabits.domain.streak.CompletionRecord
import com.saintnico.verdlyhabits.domain.streak.CompletionSource
import com.saintnico.verdlyhabits.domain.streak.Schedule
import com.saintnico.verdlyhabits.domain.streak.StreakEngine
import com.saintnico.verdlyhabits.domain.streak.StreakInput
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Thin adapter for any remaining call sites that still pass a legacy date-string set.
 * Prefer [StreakRepository] / [StreakEngine] directly — this only exists so a missed
 * cutover cannot silently invent its own consecutive-day walk again.
 */
fun calculateStreak(completedDates: Set<String>): Int {
    if (completedDates.isEmpty()) return 0
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val now = Instant.now()
    val records = completedDates.mapNotNull { raw ->
        val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return@mapNotNull null
        CompletionRecord(
            habitId = "_",
            localDate = date,
            completedAt = date.atStartOfDay(zone).toInstant(),
            zoneId = zone.id,
            offsetSeconds = zone.rules.getOffset(now).totalSeconds,
            source = CompletionSource.MIGRATED,
        )
    }
    return StreakEngine.evaluate(
        StreakInput(
            habitId = "_",
            completions = records,
            schedule = Schedule.DAILY,
            today = today,
            now = now,
            zone = zone,
        ),
    ).currentStreak
}

fun getPlantedDateText(plantedAt: Long): String {
    val instant = Instant.ofEpochMilli(plantedAt)
    val date = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return date.format(formatter)
}
