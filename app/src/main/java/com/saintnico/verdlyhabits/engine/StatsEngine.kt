package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure analytics for the premium stats dashboard.
 * Strava / Duolingo / GA-inspired metrics derived from habit history.
 */
object StatsEngine {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    data class DashboardMetrics(
        val consistencyScore: Int,
        val consistencyTier: String,
        val thisWeekRate: Float,
        val lastWeekRate: Float,
        val weekDeltaPct: Int,
        val weekTrendUp: Boolean,
        val weeklyTrend: List<WeekPoint>,
        val dailyPulse: List<DayPulse>,
        val weekdayRhythm: List<WeekdayPoint>,
        val longestStreak: Int,
        val perfectDays30: Int,
        val perfectDaysAll: Int,
        val allTimeCompletions: Int,
        val thisMonthRate: Float,
        val bestWeekRate: Float,
        val habitRankings: List<HabitRanking>,
        val topHabit: HabitRanking?,
        val focusHours: Float,
        val memberDays: Int,
    )

    data class WeekPoint(val label: String, val rate: Float, val isCurrent: Boolean)
    data class DayPulse(val date: LocalDate, val label: String, val rate: Float, val isToday: Boolean)
    data class WeekdayPoint(val day: DayOfWeek, val label: String, val rate: Float)
    data class HabitRanking(
        val habit: HabitItem,
        val consistency30: Int,
        val streak: Int,
        val total: Int,
        val spark7: List<Float>,
        val rank: Int,
    )

    fun computeDashboard(
        habits: List<HabitItem>,
        today: LocalDate = LocalDate.now(),
        totalFocusMinutes: Int = 0,
        memberSinceMillis: Long = System.currentTimeMillis(),
    ): DashboardMetrics {
        val active = habits.filter { !it.isArchived }
        val dailyRates = buildDailyRates(active, today, 365)

        val thisWeekRate = weekRate(dailyRates, today, 0)
        val lastWeekRate = weekRate(dailyRates, today, 1)
        val weekDeltaPct = if (lastWeekRate > 0f) {
            ((thisWeekRate - lastWeekRate) / lastWeekRate * 100).roundToInt()
        } else if (thisWeekRate > 0f) 100 else 0

        val longestStreak = active.maxOfOrNull { it.streak } ?: 0
        val rate30 = averageRate(dailyRates, today, 30)
        val perfect30 = perfectDaysInWindow(active, today, 30)
        val perfectAll = perfectDaysInWindow(active, today, 365)
        val streakFactor = (longestStreak.coerceAtMost(30) / 30f)
        val perfectFactor = if (active.isEmpty()) 0f else perfect30 / 30f
        val rawScore = rate30 * 0.60f + streakFactor * 0.25f + perfectFactor * 0.15f
        val consistencyScore = (rawScore * 100).roundToInt().coerceIn(0, 100)

        val weeklyTrend = (7 downTo 0).map { weeksBack ->
            val label = when (weeksBack) {
                0 -> "Now"
                1 -> "1w"
                else -> "${weeksBack}w"
            }
            WeekPoint(label, weekRate(dailyRates, today, weeksBack), weeksBack == 0)
        }.reversed()

        val dailyPulse = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayPulse(
                date = date,
                label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                rate = dailyRates[date] ?: 0f,
                isToday = offset == 0,
            )
        }

        val weekdayRhythm = DayOfWeek.entries.map { dow ->
            var done = 0
            var total = 0
            for (weekBack in 0 until 8) {
                val date = today.minusWeeks(weekBack.toLong())
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(dow))
                if (date <= today) {
                    total += active.size.coerceAtLeast(1)
                    done += active.count { it.completedDates.contains(date.format(fmt)) }
                }
            }
            WeekdayPoint(
                day = dow,
                label = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                rate = if (total == 0) 0f else done.toFloat() / total,
            )
        }

        val thisMonthRate = monthRate(active, today)
        val bestWeekRate = weeklyTrend.maxOfOrNull { it.rate } ?: 0f
        val habitRankings = active
            .map { habit ->
                val last30 = (0 until 30).count { d ->
                    habit.completedDates.contains(today.minusDays(d.toLong()).format(fmt))
                }
                val spark7 = (6 downTo 0).map { d ->
                    if (habit.completedDates.contains(today.minusDays(d.toLong()).format(fmt))) 1f else 0f
                }
                HabitRanking(
                    habit = habit,
                    consistency30 = (last30 * 100 / 30),
                    streak = habit.streak,
                    total = habit.completedDates.size,
                    spark7 = spark7,
                    rank = 0,
                )
            }
            .sortedByDescending { it.consistency30 }
            .mapIndexed { i, r -> r.copy(rank = i + 1) }

        val memberDays = ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(memberSinceMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            today,
        ).toInt().coerceAtLeast(1)

        return DashboardMetrics(
            consistencyScore = consistencyScore,
            consistencyTier = tierLabel(consistencyScore),
            thisWeekRate = thisWeekRate,
            lastWeekRate = lastWeekRate,
            weekDeltaPct = weekDeltaPct,
            weekTrendUp = weekDeltaPct >= 0,
            weeklyTrend = weeklyTrend,
            dailyPulse = dailyPulse,
            weekdayRhythm = weekdayRhythm,
            longestStreak = longestStreak,
            perfectDays30 = perfect30,
            perfectDaysAll = perfectAll,
            allTimeCompletions = active.sumOf { it.completedDates.size },
            thisMonthRate = thisMonthRate,
            bestWeekRate = bestWeekRate,
            habitRankings = habitRankings,
            topHabit = habitRankings.firstOrNull(),
            focusHours = totalFocusMinutes / 60f,
            memberDays = memberDays,
        )
    }

    fun dailyQuote(dayOfYear: Int = LocalDate.now().dayOfYear): String {
        val quotes = listOf(
            "Small daily wins compound into a life you’re proud of.",
            "Discipline is choosing what you want most over what you want now.",
            "You don’t rise to your goals — you fall to your systems.",
            "Every completion is a vote for the person you’re becoming.",
            "Consistency beats intensity. Show up again today.",
            "The gap between who you are and who you want to be is called action.",
            "Your future self is watching. Make them proud.",
            "Progress isn’t linear — but showing up is.",
            "Excellence is not an act, but a habit.",
            "One percent better every day is thirty-seven times better in a year.",
            "The best time to plant a tree was twenty years ago. The second best is now.",
            "Motivation gets you started. Habit keeps you going.",
            "You are one habit away from a completely different life.",
            "Don’t break the chain. Momentum is everything.",
            "What you do today defines who you become tomorrow.",
        )
        return quotes[dayOfYear % quotes.size]
    }

    private fun tierLabel(score: Int): String = when {
        score >= 90 -> "Elite"
        score >= 75 -> "Strong"
        score >= 55 -> "Building"
        score >= 35 -> "Warming up"
        else -> "Getting started"
    }

    private fun buildDailyRates(habits: List<HabitItem>, today: LocalDate, days: Int): Map<LocalDate, Float> =
        buildMap {
            val total = habits.size.coerceAtLeast(1)
            for (i in 0 until days) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.format(fmt)
                val done = habits.count { it.completedDates.contains(dateStr) }
                put(date, done.toFloat() / total)
            }
        }

    private fun weekRate(rates: Map<LocalDate, Float>, today: LocalDate, weeksBack: Int): Float {
        val weekEnd = today.minusWeeks(weeksBack.toLong())
        var sum = 0f
        for (d in 0 until 7) sum += rates[weekEnd.minusDays(d.toLong())] ?: 0f
        return sum / 7f
    }

    private fun averageRate(rates: Map<LocalDate, Float>, today: LocalDate, days: Int): Float {
        if (days == 0) return 0f
        var sum = 0f
        for (d in 0 until days) sum += rates[today.minusDays(d.toLong())] ?: 0f
        return sum / days
    }

    private fun perfectDaysInWindow(habits: List<HabitItem>, today: LocalDate, days: Int): Int {
        if (habits.isEmpty()) return 0
        var count = 0
        for (d in 0 until days) {
            val dateStr = today.minusDays(d.toLong()).format(fmt)
            if (habits.all { it.completedDates.contains(dateStr) }) count++
        }
        return count
    }

    private fun monthRate(habits: List<HabitItem>, today: LocalDate): Float {
        if (habits.isEmpty()) return 0f
        var done = 0
        var total = 0
        for (d in 0 until today.dayOfMonth) {
            val dateStr = today.minusDays(d.toLong()).format(fmt)
            total += habits.size
            done += habits.count { it.completedDates.contains(dateStr) }
        }
        return if (total == 0) 0f else done.toFloat() / total
    }
}
