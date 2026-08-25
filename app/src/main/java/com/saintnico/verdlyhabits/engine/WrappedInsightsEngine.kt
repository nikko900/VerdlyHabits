package com.saintnico.verdlyhabits.engine

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeeklyTrend {
    UP,
    DOWN,
    NEUTRAL,
}

data class WrappedSnapshot(
    val totalCompletions: Int,
    val totalActiveDays: Int,
    val thisWeekCompletions: Int,
    val weeklyTrend: WeeklyTrend,
    val trendPercent: Int,
    val bestDayOfWeek: String,
    val bestHabitName: String,
    val bestConsecutiveStreak: Int,
    val personaName: String,
    val personaDescription: String,
)

object WrappedInsightsEngine {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun computeAndPersist(
        habits: List<HabitItem>,
        today: LocalDate = LocalDate.now(),
    ): WrappedSnapshot {
        val snapshot = withContext(Dispatchers.Default) { compute(habits, today) }
        persist(snapshot)
        return snapshot
    }

    fun compute(
        habits: List<HabitItem>,
        today: LocalDate = LocalDate.now(),
    ): WrappedSnapshot {
        val active = habits.filter { !it.isArchived }
        val allDates = active.flatMap { habit ->
            habit.completedDates.mapNotNull(::parseDate)
        }
        val totalCompletions = allDates.size
        val totalActiveDays = allDates.toSet().size

        val thisWeekStart = today.minusDays(6)
        val lastWeekStart = today.minusDays(13)
        val lastWeekEnd = today.minusDays(7)
        val thisWeekCompletions = allDates.count { it in thisWeekStart..today }
        val lastWeekCompletions = allDates.count { it in lastWeekStart..lastWeekEnd }

        val rawTrendPercent = when {
            lastWeekCompletions > 0 ->
                (((thisWeekCompletions - lastWeekCompletions).toFloat() / lastWeekCompletions) * 100f)
                    .roundToInt()
            thisWeekCompletions > 0 -> 100
            else -> 0
        }
        val weeklyTrend = when {
            rawTrendPercent >= 8 -> WeeklyTrend.UP
            rawTrendPercent <= -8 -> WeeklyTrend.DOWN
            else -> WeeklyTrend.NEUTRAL
        }

        val bestDay = allDates
            .groupingBy { it.dayOfWeek }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.getDisplayName(TextStyle.FULL, Locale.getDefault())
            ?: today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

        val ranked = active.maxWithOrNull(
            compareBy<HabitItem> { longestConsecutive(it.completedDates) }
                .thenBy { it.completedDates.size }
                .thenBy { it.streak },
        )
        val bestHabitName = ranked?.title?.takeIf { it.isNotBlank() } ?: "Your first habit"
        val bestConsecutiveStreak = active.maxOfOrNull { longestConsecutive(it.completedDates) }
            ?.coerceAtLeast(ranked?.streak ?: 0)
            ?: 0

        val (personaName, personaDescription) = persona(
            totalCompletions = totalCompletions,
            totalActiveDays = totalActiveDays,
            bestStreak = bestConsecutiveStreak,
            trend = weeklyTrend,
        )

        return WrappedSnapshot(
            totalCompletions = totalCompletions,
            totalActiveDays = totalActiveDays,
            thisWeekCompletions = thisWeekCompletions,
            weeklyTrend = weeklyTrend,
            trendPercent = abs(rawTrendPercent),
            bestDayOfWeek = bestDay,
            bestHabitName = bestHabitName,
            bestConsecutiveStreak = bestConsecutiveStreak,
            personaName = personaName,
            personaDescription = personaDescription,
        )
    }

    private fun persona(
        totalCompletions: Int,
        totalActiveDays: Int,
        bestStreak: Int,
        trend: WeeklyTrend,
    ): Pair<String, String> = when {
        bestStreak >= 30 ->
            "30-Day Legend" to "A month of showing up. That streak is who you are now."
        bestStreak >= 21 ->
            "Iron Will" to "Three weeks of consecutive action. Discipline is compounding."
        totalCompletions >= 100 ->
            "The Architect" to "A hundred votes for the person you are becoming."
        trend == WeeklyTrend.UP ->
            "Momentum Maker" to "This week outpaced the last. Keep the chain alive."
        totalActiveDays >= 14 ->
            "Steady Grower" to "Two weeks of presence. Quiet consistency beats intensity."
        totalCompletions >= 7 ->
            "Week Warrior" to "Seven check-ins in. The habit is taking root."
        totalCompletions > 0 ->
            "The Sprout" to "You planted something real. Water it again tomorrow."
        else ->
            "The Seed" to "Your wrapped story starts with one check-in. Begin today."
    }

    private fun parseDate(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw.take(10), fmt) }.getOrNull()

    private fun longestConsecutive(dates: Set<String>): Int {
        val parsed = dates.mapNotNull(::parseDate).toSortedSet()
        if (parsed.isEmpty()) return 0
        var best = 1
        var current = 1
        var previous: LocalDate? = null
        for (date in parsed) {
            if (previous != null && date == previous.plusDays(1)) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
            previous = date
        }
        return best
    }

    private suspend fun persist(snapshot: WrappedSnapshot) = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "weeklyWrapped" to mapOf(
                            "totalCompletions" to snapshot.totalCompletions,
                            "totalActiveDays" to snapshot.totalActiveDays,
                            "thisWeekCompletions" to snapshot.thisWeekCompletions,
                            "trend" to snapshot.weeklyTrend.name,
                            "trendPercent" to snapshot.trendPercent,
                            "bestDayOfWeek" to snapshot.bestDayOfWeek,
                            "bestHabitName" to snapshot.bestHabitName,
                            "bestConsecutiveStreak" to snapshot.bestConsecutiveStreak,
                            "personaName" to snapshot.personaName,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ),
                    SetOptions.merge(),
                )
                .await()
        }
    }
}
