package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Persisted counters for trophies that cannot be derived from habits alone.
 * Stored as JSON in DataStore and merged to Firestore with stats.
 *
 * Maps are stored as CSV so Gson restore stays Int-safe.
 */
data class TrophyFacts(
    val statsVisitCount: Int = 0,
    val lastStatsIso: String = "",
    val consecutiveStatsDays: Int = 0,
    val lastOpenIso: String = "",
    val consecutiveOpenDays: Int = 0,
    val earlyMorningCount: Int = 0,
    val beforeSevenCount: Int = 0,
    val consecutiveEarlyBirdDays: Int = 0,
    val lastEarlyBirdIso: String = "",
    val lateNightCount: Int = 0,
    val afterElevenCount: Int = 0,
    val veryEarlyCount: Int = 0,
    val perfectDayCount: Int = 0,
    val consecutivePerfectDays: Int = 0,
    val lastPerfectIso: String = "",
    val recoveryCount: Int = 0,
    val surpassedCount: Int = 0,
    val peakStreakCsv: String = "",
    val firstHit14Csv: String = "",
    val monthsCsv: String = "",
    val noExcuses: Boolean = false,
    val firstHabitId: String = "",
    val firstHabitPlantedAt: Long = 0L,
    val originDone: Boolean = false,
    val launchDayPerfect: Boolean = false,
    val monthKey: String = "",
    val monthCompletions: Int = 0,
    val prevMonthCompletions: Int = 0,
    val velocityStreak: Int = 0,
    val goalsCreated: Int = 0,
    val goalsCompleted: Int = 0,
    val focusSessions: Int = 0,
    val focusMinutes: Int = 0,
    val friendsCount: Int = 0,
    val profilesViewed: Int = 0,
    val profileViewsReceived: Int = 0,
    val photoProofs: Int = 0,
    val shares: Int = 0,
    val duoStarted: Boolean = false,
    val duoMaxStreak: Int = 0,
    val challengesJoined: Int = 0,
    val challengeWins: Int = 0,
    val gardenOpened: Boolean = false,
    val hallOpened: Boolean = false,
    val twoHabitDays: Int = 0,
    val lastTwoHabitIso: String = "",
    val pendingSurpassCsv: String = "",
    val oathsLocked: Int = 0,
    val oathsConverted: Int = 0,
    val oathsForfeited: Int = 0,
    val coinsPlanted: Int = 0,
    val coinsGifted: Int = 0,
    val cosmeticsOwned: Int = 0,
) {
    fun monthsWithActivity(): Set<Int> =
        parseIntSet(monthsCsv).filter { it in 1..12 }.toSet()

    fun peakStreaks(): Map<String, Int> = parseIntMap(peakStreakCsv)

    fun firstHit14Dates(): Map<String, String> = parseStringMap(firstHit14Csv)

    /** Gson can write null into Kotlin non-null strings; collapse those before evaluate. */
    fun sanitized(): TrophyFacts = copy(
        lastStatsIso = lastStatsIso.orEmpty(),
        lastOpenIso = lastOpenIso.orEmpty(),
        lastEarlyBirdIso = lastEarlyBirdIso.orEmpty(),
        lastPerfectIso = lastPerfectIso.orEmpty(),
        peakStreakCsv = peakStreakCsv.orEmpty(),
        firstHit14Csv = firstHit14Csv.orEmpty(),
        monthsCsv = monthsCsv.orEmpty(),
        firstHabitId = firstHabitId.orEmpty(),
        monthKey = monthKey.orEmpty(),
        lastTwoHabitIso = lastTwoHabitIso.orEmpty(),
        pendingSurpassCsv = pendingSurpassCsv.orEmpty(),
    )
}

object TrophyFactsEngine {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun onAppOpen(facts: TrophyFacts, today: LocalDate): TrophyFacts {
        val todayIso = today.format(iso)
        if (facts.lastOpenIso == todayIso) return facts
        val consecutive = if (facts.lastOpenIso == today.minusDays(1).format(iso)) {
            facts.consecutiveOpenDays + 1
        } else {
            1
        }
        return facts.copy(lastOpenIso = todayIso, consecutiveOpenDays = consecutive)
    }

    fun onStatsOpen(facts: TrophyFacts, today: LocalDate): TrophyFacts {
        val todayIso = today.format(iso)
        if (facts.lastStatsIso == todayIso) {
            return facts.copy(statsVisitCount = facts.statsVisitCount + 1)
        }
        val consecutive = if (facts.lastStatsIso == today.minusDays(1).format(iso)) {
            facts.consecutiveStatsDays + 1
        } else {
            1
        }
        return facts.copy(
            statsVisitCount = facts.statsVisitCount + 1,
            lastStatsIso = todayIso,
            consecutiveStatsDays = consecutive,
        )
    }

    fun onHabitComplete(
        facts: TrophyFacts,
        today: LocalDate,
        time: LocalTime,
        habitId: String,
        plantedAt: Long,
        previousStreak: Int,
        newStreak: Int,
        isPerfectDay: Boolean,
        completedTodayCount: Int,
        reminderEnabled: Boolean,
        reminderMinutes: Int?,
        hasPhotoProof: Boolean,
        isInstallDay: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): TrophyFacts {
        val todayIso = today.format(iso)
        val hour = time.hour
        val minuteOfDay = hour * 60 + time.minute
        var next = facts

        if (next.firstHabitId.isBlank() && habitId.isNotBlank()) {
            next = next.copy(
                firstHabitId = habitId,
                firstHabitPlantedAt = if (plantedAt > 0L) plantedAt else System.currentTimeMillis(),
            )
        }

        if (hour < 5) next = next.copy(veryEarlyCount = next.veryEarlyCount + 1)
        if (hour < 6) next = next.copy(earlyMorningCount = next.earlyMorningCount + 1)
        if (hour < 7) {
            val alreadyToday = next.lastEarlyBirdIso == todayIso
            val earlyStreak = when {
                alreadyToday -> next.consecutiveEarlyBirdDays
                next.lastEarlyBirdIso == today.minusDays(1).format(iso) -> next.consecutiveEarlyBirdDays + 1
                else -> 1
            }
            next = next.copy(
                beforeSevenCount = next.beforeSevenCount + 1,
                consecutiveEarlyBirdDays = earlyStreak,
                lastEarlyBirdIso = todayIso,
            )
        }
        if (hour >= 22) next = next.copy(lateNightCount = next.lateNightCount + 1)
        if (hour >= 23) next = next.copy(afterElevenCount = next.afterElevenCount + 1)

        if (isPerfectDay) {
            val alreadyToday = next.lastPerfectIso == todayIso
            val perfectStreak = when {
                alreadyToday -> next.consecutivePerfectDays
                next.lastPerfectIso == today.minusDays(1).format(iso) -> next.consecutivePerfectDays + 1
                else -> 1
            }
            next = next.copy(
                perfectDayCount = if (alreadyToday) next.perfectDayCount else next.perfectDayCount + 1,
                consecutivePerfectDays = perfectStreak,
                lastPerfectIso = todayIso,
                launchDayPerfect = next.launchDayPerfect || isInstallDay,
            )
        }

        if (completedTodayCount >= 2 && next.lastTwoHabitIso != todayIso) {
            next = next.copy(
                twoHabitDays = next.twoHabitDays + 1,
                lastTwoHabitIso = todayIso,
            )
        }

        val peaks = next.peakStreaks().toMutableMap()
        val peak = peaks[habitId] ?: 0
        val pending = next.pendingSurpassCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        if (previousStreak == 0 && newStreak == 1 && peak >= 3 && habitId.isNotBlank()) {
            next = next.copy(recoveryCount = next.recoveryCount + 1)
            pending.add(habitId)
        }
        if (habitId in pending && newStreak > peak && peak >= 3) {
            next = next.copy(surpassedCount = next.surpassedCount + 1)
            pending.remove(habitId)
        }
        next = next.copy(pendingSurpassCsv = pending.joinToString(","))
        if (newStreak > peak && habitId.isNotBlank()) {
            peaks[habitId] = newStreak
            next = next.copy(peakStreakCsv = formatIntMap(peaks))
        }

        if (newStreak >= 14 && habitId.isNotBlank()) {
            val hits = next.firstHit14Dates()
            if (habitId !in hits) {
                next = next.copy(firstHit14Csv = formatStringMap(hits + (habitId to todayIso)))
            }
        }

        val month = today.monthValue
        val months = next.monthsWithActivity() + month
        val key = "${today.year}-${today.monthValue}"
        next = if (next.monthKey == key) {
            next.copy(
                monthCompletions = next.monthCompletions + 1,
                monthsCsv = months.sorted().joinToString(","),
            )
        } else {
            val improved = next.monthKey.isNotBlank() && next.monthCompletions > next.prevMonthCompletions
            next.copy(
                prevMonthCompletions = next.monthCompletions,
                monthKey = key,
                monthCompletions = 1,
                velocityStreak = if (improved) next.velocityStreak + 1 else 0,
                monthsCsv = months.sorted().joinToString(","),
            )
        }

        if (!next.noExcuses && reminderEnabled && reminderMinutes != null) {
            if (minuteOfDay >= reminderMinutes + 60) {
                next = next.copy(noExcuses = true)
            }
        }

        if (hasPhotoProof) next = next.copy(photoProofs = next.photoProofs + 1)

        if (!next.originDone && next.firstHabitId == habitId && next.firstHabitPlantedAt > 0L) {
            val planted = Instant.ofEpochMilli(next.firstHabitPlantedAt).atZone(zone).toLocalDate()
            if (planted.plusYears(1) == today) {
                next = next.copy(originDone = true)
            }
        }

        return next
    }

    fun parseReminderMinutes(time: String?): Int? {
        val raw = time?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parts = raw.split(':')
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].take(2).toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    fun isInstallCalendarDay(installDateMillis: Long, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (installDateMillis <= 0L) return false
        val installed = Instant.ofEpochMilli(installDateMillis).atZone(zone).toLocalDate()
        return installed == today
    }

    fun isPerfectDayAfterCompleting(
        active: List<HabitItem>,
        justCompletedId: String,
        todayIso: String,
    ): Boolean {
        if (active.isEmpty()) return false
        return active.all { habit ->
            habit.id == justCompletedId ||
                habit.isCompleted ||
                habit.completedDates.contains(todayIso)
        }
    }

    fun completedTodayCount(
        active: List<HabitItem>,
        justCompletedId: String,
        todayIso: String,
    ): Int = active.count { habit ->
        habit.id == justCompletedId ||
            habit.isCompleted ||
            habit.completedDates.contains(todayIso)
    }

    /** Two habits first reached a 14-day streak within the same 7-day window. */
    fun chainReactionProgress(facts: TrophyFacts): Int {
        val dates = facts.firstHit14Dates().values.mapNotNull { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
        if (dates.size < 2) return dates.size.coerceAtMost(1)
        for (i in dates.indices) {
            for (j in i + 1 until dates.size) {
                if (kotlin.math.abs(ChronoUnit.DAYS.between(dates[i], dates[j])) <= 6) return 2
            }
        }
        return 1
    }
}

internal fun parseIntSet(csv: String): Set<Int> =
    csv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

internal fun parseIntMap(csv: String): Map<String, Int> {
    if (csv.isBlank()) return emptyMap()
    return csv.split(',').mapNotNull { token ->
        val parts = token.split(':')
        if (parts.size != 2) return@mapNotNull null
        val key = parts[0].trim()
        val value = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
        if (key.isEmpty()) null else key to value
    }.toMap()
}

internal fun parseStringMap(csv: String): Map<String, String> {
    if (csv.isBlank()) return emptyMap()
    return csv.split(',').mapNotNull { token ->
        val parts = token.split(':')
        if (parts.size != 2) return@mapNotNull null
        val key = parts[0].trim()
        val value = parts[1].trim()
        if (key.isEmpty() || value.isEmpty()) null else key to value
    }.toMap()
}

internal fun formatIntMap(map: Map<String, Int>): String =
    map.entries.joinToString(",") { "${it.key}:${it.value}" }

internal fun formatStringMap(map: Map<String, String>): String =
    map.entries.joinToString(",") { "${it.key}:${it.value}" }
