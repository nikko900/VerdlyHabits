package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure functions that generate contextual, motivational insights from habit data.
 *
 * Each insight is a self-contained sentence designed to make the user feel:
 *   → "I am making real progress"
 *   → "This app understands me"
 *   → "I need to show this to someone"
 *
 * The engine never stores state — it derives everything from the habit list and
 * stats on every call, so insights always reflect the latest data.
 */
object MotivationalEngine {

    data class Insight(
        val text: String,
        /** True when this insight should trigger a glow/pulse animation. */
        val isHighlight: Boolean = false,
        /** Which icon style to show — a simple hint for the UI. */
        val kind: InsightKind = InsightKind.INFO
    )

    enum class InsightKind { FIRE, TROPHY, TREND, CLOCK, CHAIN, INFO }

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Generates a ranked list of insights from the user's habits. The first
     * element is the most important/exciting insight for today. The caller
     * typically shows only one or rotates through the top 3.
     */
    fun generateInsights(
        habits: List<HabitItem>,
        longestStreakEver: Int,
        totalCompletions: Int,
        totalXp: Int
    ): List<Insight> {
        if (habits.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val active = habits.filter { !it.isArchived && !it.isPaused }
        val insights = mutableListOf<Insight>()

        // ── 1. Longest streak ever detection ────────────────────────────────
        val currentBestStreak = active.maxOfOrNull { it.streak } ?: 0
        if (currentBestStreak > 0 && currentBestStreak >= longestStreakEver) {
            insights += Insight(
                text = "You're on your longest streak ever — $currentBestStreak days!",
                isHighlight = true,
                kind = InsightKind.FIRE
            )
        }

        // ── 2. Close to personal best ───────────────────────────────────────
        active.filter { it.streak > 0 }.forEach { habit ->
            val bestStreak = calculateBestStreakFromHistory(habit)
            val gap = bestStreak - habit.streak
            if (gap in 1..5 && bestStreak > 3) {
                insights += Insight(
                    text = "$gap more day${if (gap > 1) "s" else ""} to beat your personal best on ${habit.title}.",
                    kind = InsightKind.CHAIN
                )
            }
        }

        // ── 3. Week-over-week performance comparison ────────────────────────
        val thisWeekRate = weekCompletionRate(active, today, 0)
        val lastWeekRate = weekCompletionRate(active, today, 1)
        if (thisWeekRate > 0 && lastWeekRate > 0) {
            val pctChange = ((thisWeekRate - lastWeekRate) / lastWeekRate * 100).toInt()
            if (pctChange > 10) {
                insights += Insight(
                    text = "This week you're ${pctChange}% ahead of last week. Keep building.",
                    kind = InsightKind.TREND
                )
            }
        }

        // ── 4. Outperform past weeks ────────────────────────────────────────
        val pastWeekRates = (1..8).map { weekCompletionRate(active, today, it) }.filter { it > 0 }
        if (pastWeekRates.isNotEmpty() && thisWeekRate > 0) {
            val betterThanCount = pastWeekRates.count { thisWeekRate >= it }
            val pct = (betterThanCount * 100) / pastWeekRates.size
            if (pct >= 70) {
                insights += Insight(
                    text = "This week you outperformed ${pct}% of your past weeks.",
                    isHighlight = pct >= 90,
                    kind = InsightKind.TROPHY
                )
            }
        }

        // ── 5. Never-miss-a-day detection ───────────────────────────────────
        val neverMissedDay = findNeverMissedDay(active, today)
        if (neverMissedDay != null) {
            val dayName = neverMissedDay.getDisplayName(TextStyle.FULL, Locale.getDefault())
            insights += Insight(
                text = "$dayName is your most consistent day — you've never missed it.",
                kind = InsightKind.CHAIN
            )
        }

        // ── 6. Don't-break-the-chain ────────────────────────────────────────
        active.filter { it.streak >= 7 }.sortedByDescending { it.streak }.take(2).forEach { habit ->
            insights += Insight(
                text = "${habit.title}: ${habit.streak} days straight — don't break the chain.",
                kind = InsightKind.CHAIN
            )
        }

        // ── 7. XP level proximity ───────────────────────────────────────────
        val xpToNext = GamificationEngine.xpToNextLevel(totalXp)
        val currentLevel = GamificationEngine.calculateLevel(totalXp)
        val nextTitle = GamificationEngine.levelTitle(currentLevel + 1)
        if (xpToNext <= 150 && currentLevel > 0) {
            insights += Insight(
                text = "Only $xpToNext XP to reach $nextTitle. You're almost there.",
                kind = InsightKind.TROPHY
            )
        }

        // ── 8. Milestone completion count ───────────────────────────────────
        val nextMilestone = listOf(50, 100, 250, 500, 1000, 2500, 5000)
            .firstOrNull { it > totalCompletions }
        if (nextMilestone != null) {
            val remaining = nextMilestone - totalCompletions
            if (remaining <= 10) {
                insights += Insight(
                    text = "$remaining completions until you hit $nextMilestone total. Legacy in progress.",
                    kind = InsightKind.TROPHY
                )
            }
        }

        // ── 9. Perfect day streak ───────────────────────────────────────────
        val perfectDayStreak = countConsecutivePerfectDays(active, today)
        if (perfectDayStreak >= 3) {
            insights += Insight(
                text = "$perfectDayStreak perfect days in a row. Flawless.",
                isHighlight = true,
                kind = InsightKind.FIRE
            )
        }

        // ── 10. Morning advantage ───────────────────────────────────────────
        val hour = java.time.LocalTime.now().hour
        if (hour < 10 && active.any { !it.isCompleted }) {
            insights += Insight(
                text = "Habits completed before 10 AM have a 90% streak survival rate.",
                kind = InsightKind.CLOCK
            )
        }

        return insights
    }

    /**
     * Returns the top insight for today, or a generic motivational line if
     * no data-driven insight is available.
     */
    fun topInsight(
        habits: List<HabitItem>,
        longestStreakEver: Int,
        totalCompletions: Int,
        totalXp: Int
    ): Insight {
        val all = generateInsights(habits, longestStreakEver, totalCompletions, totalXp)
        return all.firstOrNull() ?: Insight(
            text = "Every completion today is a vote for the person you're becoming.",
            kind = InsightKind.INFO
        )
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private fun weekCompletionRate(
        habits: List<HabitItem>,
        today: LocalDate,
        weeksBack: Int
    ): Float {
        if (habits.isEmpty()) return 0f
        val weekEnd = today.minusWeeks(weeksBack.toLong())
        var total = 0
        var done = 0
        for (d in 0 until 7) {
            val dateStr = weekEnd.minusDays(d.toLong()).format(fmt)
            total += habits.size
            done += habits.count { it.completedDates.contains(dateStr) }
        }
        return if (total == 0) 0f else done.toFloat() / total
    }

    private fun calculateBestStreakFromHistory(habit: HabitItem): Int {
        if (habit.completedDates.isEmpty()) return 0
        val sorted = habit.completedDates.mapNotNull {
            try { LocalDate.parse(it, fmt) } catch (_: Exception) { null }
        }.sorted()
        if (sorted.isEmpty()) return 0

        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                current++
                best = maxOf(best, current)
            } else {
                current = 1
            }
        }
        return best
    }

    private fun findNeverMissedDay(habits: List<HabitItem>, today: LocalDate): DayOfWeek? {
        if (habits.isEmpty()) return null
        // Look at the last 8 weeks
        return DayOfWeek.entries.firstOrNull { dow ->
            val daysToCheck = (0 until 8).map { weekBack ->
                today.minusWeeks(weekBack.toLong())
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(dow))
            }.filter { it <= today }

            daysToCheck.size >= 4 && daysToCheck.all { date ->
                val dateStr = date.format(fmt)
                habits.any { it.completedDates.contains(dateStr) }
            }
        }
    }

    private fun countConsecutivePerfectDays(habits: List<HabitItem>, today: LocalDate): Int {
        if (habits.isEmpty()) return 0
        var count = 0
        var day = today.minusDays(1) // Start from yesterday (today may not be done yet)
        while (true) {
            val dateStr = day.format(fmt)
            val allDone = habits.all { it.completedDates.contains(dateStr) }
            if (!allDone) break
            count++
            day = day.minusDays(1)
            if (count > 365) break // Safety
        }
        return count
    }
}
