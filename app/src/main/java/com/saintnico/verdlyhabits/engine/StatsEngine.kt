package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.domain.HabitScheduling
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
        val rate7: Float = 0f,
        val rate14: Float = 0f,
        val rate30: Float = 0f,
        val rate90: Float = 0f,
        val bestWeekday: WeekdayPoint? = null,
        val weakestWeekday: WeekdayPoint? = null,
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
        val scorePct: Int = 0,
        val missStreak: Int = 0,
        val verdict: String = "",
    )

    fun computeDashboard(
        habits: List<HabitItem>,
        today: LocalDate = LocalDate.now(),
        totalFocusMinutes: Int = 0,
        memberSinceMillis: Long = System.currentTimeMillis(),
    ): DashboardMetrics {
        val active = habits.filter { !it.isArchived && !it.isPaused }
        val dailyRates = buildDailyRates(active, today, 365)

        val thisWeekRate = weekRate(dailyRates, today, 0)
        val lastWeekRate = weekRate(dailyRates, today, 1)
        val weekDeltaPct = if (lastWeekRate > 0f) {
            ((thisWeekRate - lastWeekRate) / lastWeekRate * 100).roundToInt()
        } else if (thisWeekRate > 0f) 100 else 0

        val longestStreak = active.maxOfOrNull { it.streak } ?: 0
        val rate7 = averageRate(dailyRates, today, 7)
        val rate14 = averageRate(dailyRates, today, 14)
        val rate30 = averageRate(dailyRates, today, 30)
        val rate90 = averageRate(dailyRates, today, 90)
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
                    val due = active.filter { isDue(it, date) }
                    total += due.size
                    done += due.count { wasDone(it, date) }
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
                val due30 = (0 until 30).count { d -> isDue(habit, today.minusDays(d.toLong())) }
                val last30 = (0 until 30).count { d ->
                    val date = today.minusDays(d.toLong())
                    isDue(habit, date) && wasDone(habit, date)
                }
                val spark7 = (6 downTo 0).map { d ->
                    if (wasDone(habit, today.minusDays(d.toLong()))) 1f else 0f
                }
                val score = HabitScoreEngine.score(habit.completedDates, today, habit.plantedAt)
                HabitRanking(
                    habit = habit,
                    consistency30 = if (due30 == 0) 0 else (last30 * 100 / due30),
                    streak = habit.streak,
                    total = habit.completedDates.size,
                    spark7 = spark7,
                    rank = 0,
                    scorePct = (score * 100).roundToInt(),
                    missStreak = missStreak(habit, today),
                    verdict = habitVerdict(score, habit.streak, missStreak(habit, today)),
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
            rate7 = rate7,
            rate14 = rate14,
            rate30 = rate30,
            rate90 = rate90,
            bestWeekday = weekdayRhythm.maxByOrNull { it.rate }?.takeIf { it.rate > 0f },
            weakestWeekday = weekdayRhythm.minByOrNull { it.rate },
        )
    }

    val disciplineQuotes: List<String> = listOf(
            "Small daily wins compound into a life you’re proud of.",
            "Discipline is choosing what you want most over what you want now.",
            "You don’t rise to your goals. You fall to your systems.",
            "Every completion is a vote for the person you’re becoming.",
            "Consistency beats intensity. Show up again today.",
            "The gap between who you are and who you want to be is called action.",
            "Your future self is watching. Make them proud.",
            "Progress isn’t linear. But showing up is.",
            "Excellence is not an act, but a habit.",
            "One percent better every day is thirty-seven times better in a year.",
            "The best time to plant a tree was twenty years ago. The second best is now.",
            "Motivation gets you started. Habit keeps you going.",
            "You are one habit away from a completely different life.",
            "Don’t break the chain. Momentum is everything.",
            "What you do today defines who you become tomorrow.",
            "A habit is a vote. Cast another one before the day ends.",
            "Miss once if you must. Never miss twice.",
            "The work is quiet. The results are loud.",
            "Show up when it is boring. That is the whole secret.",
            "Your streak is not a trophy. It is a reminder you can be trusted.",
            "Protect the morning and the evening will follow.",
            "Soft days still count if you plant something.",
            "Discipline is self-respect with a calendar.",
            "You do not need more motivation. You need the next small done.",
            "The person you want is built in ordinary Tuesdays.",
            "Keep the promise when nobody is watching.",
            "Rhythm beats willpower. Design the day, then walk it.",
            "A closed loop today is a quieter mind tonight.",
            "Start smaller than your pride. Finish anyway.",
            "The forest grows from days that did not feel special.",
            "Hold the line for twenty minutes. Identity does the rest.",
            "You are not behind. You are one honest session away.",
            "Make it so easy to start that quitting feels heavier.",
            "Consistency is a kindness you give your future self.",
            "Heat fades. Systems stay. Trust the system today.",
            "If it is on the list, it gets a mark or a reason. Not a shrug.",
            "Win the day in three moves, not thirty.",
            "The chain is the coach. Do not argue with it.",
            "Plant. Water. Repeat. Pride comes later.",
            "Today’s standard becomes tomorrow’s baseline.",
        )

    fun dailyQuote(dayOfYear: Int = LocalDate.now().dayOfYear): String {
        return disciplineQuotes[dayOfYear % disciplineQuotes.size]
    }

    fun rotatingQuote(tick: Int, dayOfYear: Int = LocalDate.now().dayOfYear): String {
        val i = (dayOfYear + tick).mod(disciplineQuotes.size)
        return disciplineQuotes[i]
    }

    fun isDue(habit: HabitItem, date: LocalDate): Boolean {
        if (habit.isArchived || habit.isPaused) return false
        return HabitScheduling.isDueOn(habit.frequency, habit.customDaysMask, date)
    }

    fun wasDone(habit: HabitItem, date: LocalDate): Boolean =
        habit.completedDates.contains(date.format(fmt))

    fun dueOn(habits: List<HabitItem>, date: LocalDate): List<HabitItem> =
        habits.filter { isDue(it, date) }

    fun missStreak(habit: HabitItem, today: LocalDate, lookback: Int = 60): Int {
        var streak = 0
        for (d in 0 until lookback) {
            val date = today.minusDays(d.toLong())
            if (!isDue(habit, date)) continue
            if (wasDone(habit, date)) break
            streak++
        }
        return streak
    }

    fun habitVerdict(score: Float, streak: Int, misses: Int): String = when {
        misses >= 4 -> "At risk"
        score >= 0.85f && streak >= 7 -> "Mastered rhythm"
        score >= 0.70f -> "Strong"
        score >= 0.45f && streak >= 3 -> "Building"
        misses >= 2 -> "Slipping"
        else -> "Forming"
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
            for (i in 0 until days) {
                val date = today.minusDays(i.toLong())
                val due = dueOn(habits, date)
                put(date, if (due.isEmpty()) 0f else due.count { wasDone(it, date) }.toFloat() / due.size)
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
            val date = today.minusDays(d.toLong())
            val due = dueOn(habits, date)
            if (due.isNotEmpty() && due.all { wasDone(it, date) }) count++
        }
        return count
    }

    private fun monthRate(habits: List<HabitItem>, today: LocalDate): Float {
        if (habits.isEmpty()) return 0f
        var done = 0
        var total = 0
        for (d in 0 until today.dayOfMonth) {
            val date = today.minusDays(d.toLong())
            val due = dueOn(habits, date)
            total += due.size
            done += due.count { wasDone(it, date) }
        }
        return if (total == 0) 0f else done.toFloat() / total
    }
}
