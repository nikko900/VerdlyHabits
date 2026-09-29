package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deep premium analytics — Whoop / Strava / Apple Fitness style stories
 * derived only from habit history. No extra libraries.
 */
object StatsInsightEngine {

    data class Report(
        val score: ScoreBreakdown,
        val momentum: Momentum,
        val week: WeekDeepDive,
        val rhythm: RhythmDeepDive,
        val habits: List<HabitDeepDive>,
        val signals: List<Signal>,
        val metrics: Map<MetricKey, MetricStory>,
    )

    enum class MetricKey { Completions, PerfectDays, Focus, Streak, MemberDays, MonthRate }

    enum class SignalKind { WIN, LEAK, COACH, MILESTONE, MOMENTUM }

    data class WindowRate(val label: String, val days: Int, val rate: Float)

    data class ScoreBreakdown(
        val score: Int,
        val tier: String,
        val windows: List<WindowRate>,
        val ratePoints: Int,
        val streakPoints: Int,
        val perfectPoints: Int,
        val headline: String,
        val story: String,
        val congratulations: String?,
        val nextMove: String,
    )

    data class Momentum(
        val label: String,
        val deltaPct: Int,
        val rising: Boolean,
        val spark28: List<Float>,
        val headline: String,
        val story: String,
        val nextMove: String,
    )

    data class DayDetail(
        val date: LocalDate,
        val label: String,
        val rate: Float,
        val done: Int,
        val due: Int,
        val completed: List<String>,
        val missed: List<String>,
        val isToday: Boolean,
    )

    data class WeekDeepDive(
        val days: List<DayDetail>,
        val done: Int,
        val due: Int,
        val rate: Float,
        val perfectDays: Int,
        val bestLabel: String?,
        val leakLabel: String?,
        val headline: String,
        val story: String,
        val nextMove: String,
    )

    data class RhythmDeepDive(
        val strongest: String?,
        val weakest: String?,
        val weekdayRate: Float,
        val weekendRate: Float,
        val headline: String,
        val story: String,
        val nextMove: String,
    )

    data class HabitDeepDive(
        val habit: HabitItem,
        val rank: Int,
        val scorePct: Int,
        val scoreTier: String,
        val consistency30: Int,
        val consistency90: Int,
        val streak: Int,
        val bestStreak: Int,
        val total: Int,
        val missStreak: Int,
        val bestWeekday: String?,
        val worstWeekday: String?,
        val spark30: List<Float>,
        val verdict: String,
        val headline: String,
        val story: String,
        val congratulations: String?,
        val leak: String?,
        val nextMove: String,
    )

    data class Signal(
        val kind: SignalKind,
        val title: String,
        val body: String,
        val action: String?,
        val habitId: String? = null,
        val highlight: Boolean = false,
    )

    data class MetricStory(
        val title: String,
        val value: String,
        val story: String,
        val facts: List<Pair<String, String>>,
        val congratulations: String?,
        val nextMove: String,
    )

    fun analyze(
        habits: List<HabitItem>,
        metrics: StatsEngine.DashboardMetrics,
        longestStreakEver: Int,
        totalCompletions: Int,
        totalXp: Int,
        totalFocusMinutes: Int,
        today: LocalDate = LocalDate.now(),
    ): Report {
        val active = habits.filter { !it.isArchived && !it.isPaused }
        val score = scoreBreakdown(metrics)
        val momentum = momentum(metrics, active, today)
        val week = weekDive(active, metrics, today)
        val rhythm = rhythmDive(metrics)
        val habitDives = active.map { habit ->
            habitDive(habit, metrics.habitRankings, today)
        }.sortedBy { it.rank }
        val signals = signals(
            active = active,
            metrics = metrics,
            score = score,
            momentum = momentum,
            week = week,
            habits = habitDives,
            longestStreakEver = longestStreakEver,
            totalCompletions = totalCompletions,
            totalXp = totalXp,
        )
        val metricStories = metricStories(
            metrics = metrics,
            week = week,
            longestStreakEver = longestStreakEver,
            totalCompletions = totalCompletions,
            totalFocusMinutes = totalFocusMinutes,
        )
        return Report(
            score = score,
            momentum = momentum,
            week = week,
            rhythm = rhythm,
            habits = habitDives,
            signals = signals,
            metrics = metricStories,
        )
    }

    private fun scoreBreakdown(metrics: StatsEngine.DashboardMetrics): ScoreBreakdown {
        val longest = metrics.longestStreak.coerceAtMost(30)
        val ratePoints = (metrics.rate30 * 60f).roundToInt()
        val streakPoints = (longest / 30f * 25f).roundToInt()
        val perfectPoints = (metrics.perfectDays30 / 30f * 15f).roundToInt()
        val windows = listOf(
            WindowRate("7 days", 7, metrics.rate7),
            WindowRate("14 days", 14, metrics.rate14),
            WindowRate("30 days", 30, metrics.rate30),
            WindowRate("90 days", 90, metrics.rate90),
        )
        val congratulations = when {
            metrics.consistencyScore >= 90 ->
                "Elite range. You are living the standard, not chasing it."
            metrics.consistencyScore >= 75 && metrics.weekTrendUp ->
                "Strong score and rising. This is what compounding looks like."
            metrics.consistencyScore >= 75 ->
                "You are already in Strong. Hold the rhythm. That is the whole game."
            metrics.consistencyScore >= 55 && metrics.weekTrendUp ->
                "Building, and this week proved it. Keep stacking quiet wins."
            else -> null
        }
        val nextMove = when {
            metrics.rate7 < metrics.rate30 && metrics.rate30 > 0f ->
                "This week is softer than your 30-day average. Close one open habit before tonight."
            metrics.perfectDays30 == 0 && metrics.habitRankings.isNotEmpty() ->
                "Chase one perfect day this week. Every due habit, same date."
            metrics.longestStreak < 7 ->
                "Protect a 7-day streak on your easiest habit. Identity follows the chain."
            metrics.consistencyScore >= 80 ->
                "Do not add more yet. Defend the loop you already trust."
            else ->
                "Pick the weakest habit and plant it first tomorrow. One clean mark beats a heroic catch-up."
        }
        val headline = when {
            metrics.consistencyScore >= 90 -> "Elite consistency"
            metrics.consistencyScore >= 75 -> "You are in Strong territory"
            metrics.consistencyScore >= 55 -> "The score is building"
            metrics.consistencyScore >= 35 -> "Warming up. The engine is on"
            else -> "The first votes matter most"
        }
        val story = buildString {
            append("Score ${metrics.consistencyScore} is ${metrics.consistencyTier.lowercase()}: ")
            append("${ratePoints} from your 30-day show-up rate, ")
            append("${streakPoints} from streak strength, ")
            append("${perfectPoints} from perfect days.")
            if (metrics.rate7 > 0f) {
                append(" Last 7 days you planted ${(metrics.rate7 * 100).roundToInt()}% of what was due.")
            }
        }
        return ScoreBreakdown(
            score = metrics.consistencyScore,
            tier = metrics.consistencyTier,
            windows = windows,
            ratePoints = ratePoints,
            streakPoints = streakPoints,
            perfectPoints = perfectPoints,
            headline = headline,
            story = story,
            congratulations = congratulations,
            nextMove = nextMove,
        )
    }

    private fun momentum(
        metrics: StatsEngine.DashboardMetrics,
        habits: List<HabitItem>,
        today: LocalDate,
    ): Momentum {
        val spark = (27 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val due = StatsEngine.dueOn(habits, date)
            if (due.isEmpty()) 0f else due.count { StatsEngine.wasDone(it, date) }.toFloat() / due.size
        }
        val recent = spark.takeLast(7).average().toFloat()
        val earlier = spark.take(7).average().toFloat()
        val rising = metrics.weekDeltaPct >= 0
        val label = when {
            metrics.weekDeltaPct >= 20 && recent > earlier -> "Accelerating"
            metrics.weekDeltaPct <= -20 -> "Cooling"
            recent + 0.08f < earlier -> "Rebuilding"
            abs(metrics.weekDeltaPct) <= 6 -> "Steady"
            rising -> "Lifting"
            else -> "Softening"
        }
        val headline = when (label) {
            "Accelerating" -> "Momentum is on your side"
            "Lifting" -> "The week is tilting up"
            "Steady" -> "You are holding the line"
            "Rebuilding" -> "The dip is already being repaired"
            "Softening" -> "Heat is fading. Still recoverable"
            else -> "The fire needs a log"
        }
        val story = if (metrics.lastWeekRate == 0f && metrics.thisWeekRate == 0f) {
            "Not enough weeks yet to call a trend. Every mark this week becomes the baseline."
        } else {
            val dir = if (rising) "ahead of" else "behind"
            "This week you are ${abs(metrics.weekDeltaPct)}% $dir last week " +
                "(${(metrics.thisWeekRate * 100).roundToInt()}% vs ${(metrics.lastWeekRate * 100).roundToInt()}%). " +
                "Your best recent week hit ${(metrics.bestWeekRate * 100).roundToInt()}%."
        }
        val nextMove = when (label) {
            "Accelerating", "Lifting" -> "Do not break the rise. Finish today’s due list before you add anything new."
            "Steady" -> "Steady is premium. Keep the same hour tomorrow. Rhythm beats a surge."
            "Rebuilding" -> "You already started the climb. One more clean day locks the recovery."
            else -> "Treat today as day one of the rebound. Smallest habit first, then the rest."
        }
        return Momentum(
            label = label,
            deltaPct = metrics.weekDeltaPct,
            rising = rising,
            spark28 = spark,
            headline = headline,
            story = story,
            nextMove = nextMove,
        )
    }

    private fun weekDive(
        habits: List<HabitItem>,
        metrics: StatsEngine.DashboardMetrics,
        today: LocalDate,
    ): WeekDeepDive {
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val due = StatsEngine.dueOn(habits, date)
            val completed = due.filter { StatsEngine.wasDone(it, date) }
            val missed = due.filter { !StatsEngine.wasDone(it, date) }
            DayDetail(
                date = date,
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                rate = metrics.dailyPulse.firstOrNull { it.date == date }?.rate
                    ?: if (due.isEmpty()) 0f else completed.size.toFloat() / due.size,
                done = completed.size,
                due = due.size,
                completed = completed.map { it.title },
                missed = missed.map { it.title },
                isToday = offset == 0,
            )
        }
        val done = days.sumOf { it.done }
        val due = days.sumOf { it.due }
        val rate = if (due == 0) 0f else done.toFloat() / due
        val perfect = days.count { it.due > 0 && it.missed.isEmpty() }
        val best = days.filter { it.due > 0 }.maxByOrNull { it.rate }
        val leak = days.filter { it.due > 0 && it.missed.isNotEmpty() }.minByOrNull { it.rate }
        val headline = when {
            due == 0 -> "No habits were due this week"
            rate >= 0.9f -> "This week is almost clean"
            rate >= 0.7f -> "A solid week. A few leaks"
            rate >= 0.4f -> "Half-built week. Recoverable."
            else -> "The week went quiet"
        }
        val story = buildString {
            append("$done of $due due marks planted · $perfect perfect day${if (perfect == 1) "" else "s"}.")
            if (best != null) append(" ${best.label} was your strongest.")
            if (leak != null && leak.missed.isNotEmpty()) {
                append(" ${leak.label} leaked ${leak.missed.take(2).joinToString(" and ")}.")
            }
        }
        val nextMove = when {
            days.lastOrNull()?.missed?.isNotEmpty() == true ->
                "Still open today: ${days.last().missed.take(3).joinToString(", ")}. Close them before midnight."
            leak != null && leak.missed.isNotEmpty() ->
                "Watch ${leak.label}s. Pre-decide the first habit for that day."
            rate >= 0.85f ->
                "Protect the close. Same order tomorrow. Do not get clever."
            else ->
                "Tomorrow, plant the easiest due habit within 10 minutes of waking."
        }
        return WeekDeepDive(
            days = days,
            done = done,
            due = due,
            rate = rate,
            perfectDays = perfect,
            bestLabel = best?.label,
            leakLabel = leak?.label,
            headline = headline,
            story = story,
            nextMove = nextMove,
        )
    }

    private fun rhythmDive(metrics: StatsEngine.DashboardMetrics): RhythmDeepDive {
        val points = metrics.weekdayRhythm
        val weekday = points.filter { it.day.value <= 5 }
        val weekend = points.filter { it.day.value >= 6 }
        val weekdayRate = if (weekday.isEmpty()) 0f else weekday.map { it.rate }.average().toFloat()
        val weekendRate = if (weekend.isEmpty()) 0f else weekend.map { it.rate }.average().toFloat()
        val strong = metrics.bestWeekday
        val weak = metrics.weakestWeekday
        val headline = when {
            strong != null && weak != null && strong.rate - weak.rate >= 0.25f ->
                "${strong.label} carries you · ${weak.label} is the leak"
            strong != null -> "${strong.label} is your home day"
            else -> "Your week still looking for a pulse"
        }
        val story = buildString {
            if (strong != null) {
                append("Across 8 weeks, ${strong.label} is your most reliable day ")
                append("(${(strong.rate * 100).roundToInt()}% of due habits).")
            }
            if (weak != null && (strong == null || weak.day != strong.day)) {
                append(" ${weak.label} drops to ${(weak.rate * 100).roundToInt()}%.")
            }
            append(
                if (weekendRate + 0.08f < weekdayRate) {
                    " Weekends run cooler than weekdays. Classic fade."
                } else if (weekendRate > weekdayRate + 0.08f) {
                    " Weekends are actually stronger. Use that energy."
                } else {
                    " Weekdays and weekends are close. The system is even."
                },
            )
        }
        val nextMove = when {
            weak != null && weak.rate < 0.5f ->
                "On ${weak.label}, shrink the first habit to two minutes. Make the leak day unmissable."
            weekendRate + 0.1f < weekdayRate ->
                "Set a Saturday opener before noon. Weekend drift is where streaks die."
            else ->
                "Keep the same first habit on your strongest day. Clone that hour onto a weaker day."
        }
        return RhythmDeepDive(
            strongest = strong?.label,
            weakest = weak?.label,
            weekdayRate = weekdayRate,
            weekendRate = weekendRate,
            headline = headline,
            story = story,
            nextMove = nextMove,
        )
    }

    private fun habitDive(
        habit: HabitItem,
        rankings: List<StatsEngine.HabitRanking>,
        today: LocalDate,
    ): HabitDeepDive {
        val ranking = rankings.firstOrNull { it.habit.id == habit.id }
        val score = HabitScoreEngine.score(habit.completedDates, today, habit.plantedAt)
        val due90 = (0 until 90).count { StatsEngine.isDue(habit, today.minusDays(it.toLong())) }
        val done90 = (0 until 90).count { d ->
            val date = today.minusDays(d.toLong())
            StatsEngine.isDue(habit, date) && StatsEngine.wasDone(habit, date)
        }
        val spark30 = (29 downTo 0).map { d ->
            if (StatsEngine.wasDone(habit, today.minusDays(d.toLong()))) 1f else 0f
        }
        val byDow = DayOfWeek.entries.associateWith { dow ->
            var due = 0
            var done = 0
            for (w in 0 until 8) {
                val date = today.minusWeeks(w.toLong())
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(dow))
                if (date <= today && StatsEngine.isDue(habit, date)) {
                    due++
                    if (StatsEngine.wasDone(habit, date)) done++
                }
            }
            if (due == 0) 0f else done.toFloat() / due
        }
        val bestDow = byDow.maxByOrNull { it.value }?.takeIf { it.value > 0f }
        val worstDow = byDow.minByOrNull { it.value }
        val bestStreak = bestStreakFromHistory(habit)
        val misses = ranking?.missStreak ?: StatsEngine.missStreak(habit, today)
        val verdict = ranking?.verdict ?: StatsEngine.habitVerdict(score, habit.streak, misses)
        val congratulations = when {
            habit.streak >= 21 -> "Twenty-one days on ${habit.title}. This is identity, not a mood."
            habit.streak >= 7 && score >= 0.7f -> "A week-plus chain and a strong score. ${habit.title} is locking in."
            ranking?.rank == 1 && ranking.consistency30 >= 70 ->
                "${habit.title} is your strongest habit right now. That is the one to protect."
            else -> null
        }
        val leak = when {
            misses >= 3 -> "Open miss streak: $misses due days. Close it today or the score will keep falling."
            worstDow != null && bestDow != null && bestDow.value - worstDow.value >= 0.35f ->
                "${habit.title} fades on ${dowName(worstDow.key)}."
            score < 0.35f && habit.completedDates.isNotEmpty() ->
                "The loop is still fragile. Keep the version that takes under two minutes."
            else -> null
        }
        val nextMove = when {
            misses > 0 -> "Plant ${habit.title} before anything else today. End the miss streak."
            habit.streak in 5..6 -> "One more day and ${habit.title} is a week. Do not get cute."
            worstDow != null && (worstDow.value) < 0.45f ->
                "On ${dowName(worstDow.key)}, attach ${habit.title} to a fixed cue you already keep."
            else -> "Keep the same time. Do not upgrade the habit until the chain is bored."
        }
        val headline = when (verdict) {
            "Mastered rhythm" -> "${habit.title} is in rhythm"
            "Strong" -> "${habit.title} is holding"
            "At risk" -> "${habit.title} needs a rescue"
            "Slipping" -> "${habit.title} is slipping"
            "Building" -> "${habit.title} is building"
            else -> "${habit.title} is still forming"
        }
        val story = buildString {
            append("${ranking?.consistency30 ?: 0}% of due days in 30 · ")
            append("score ${HabitScoreEngine.scoreLabel(score)} (${HabitScoreEngine.scoreTier(score)}) · ")
            append("streak ${habit.streak} (best $bestStreak).")
            if (bestDow != null) append(" Best on ${dowName(bestDow.key)}.")
        }
        return HabitDeepDive(
            habit = habit,
            rank = ranking?.rank ?: 99,
            scorePct = (score * 100).roundToInt(),
            scoreTier = HabitScoreEngine.scoreTier(score),
            consistency30 = ranking?.consistency30 ?: 0,
            consistency90 = if (due90 == 0) 0 else done90 * 100 / due90,
            streak = habit.streak,
            bestStreak = bestStreak,
            total = habit.completedDates.size,
            missStreak = misses,
            bestWeekday = bestDow?.let { dowName(it.key) },
            worstWeekday = worstDow?.let { dowName(it.key) },
            spark30 = spark30,
            verdict = verdict,
            headline = headline,
            story = story,
            congratulations = congratulations,
            leak = leak,
            nextMove = nextMove,
        )
    }

    private fun signals(
        active: List<HabitItem>,
        metrics: StatsEngine.DashboardMetrics,
        score: ScoreBreakdown,
        momentum: Momentum,
        week: WeekDeepDive,
        habits: List<HabitDeepDive>,
        longestStreakEver: Int,
        totalCompletions: Int,
        totalXp: Int,
    ): List<Signal> {
        val out = mutableListOf<Signal>()
        if (momentum.rising && metrics.weekDeltaPct >= 8) {
            out += Signal(
                kind = SignalKind.MOMENTUM,
                title = "Momentum up",
                body = momentum.story,
                action = momentum.nextMove,
                highlight = true,
            )
        } else if (!momentum.rising && metrics.weekDeltaPct <= -8) {
            out += Signal(
                kind = SignalKind.LEAK,
                title = "Momentum cooling",
                body = momentum.story,
                action = momentum.nextMove,
            )
        }

        habits.filter { it.congratulations != null }.take(2).forEach {
            out += Signal(
                kind = SignalKind.WIN,
                title = "Doing well · ${it.habit.title}",
                body = it.congratulations!!,
                action = it.nextMove,
                habitId = it.habit.id,
                highlight = true,
            )
        }
        habits.filter { it.leak != null }.sortedByDescending { it.missStreak }.take(3).forEach {
            out += Signal(
                kind = SignalKind.LEAK,
                title = "Needs you · ${it.habit.title}",
                body = it.leak!!,
                action = it.nextMove,
                habitId = it.habit.id,
            )
        }

        val todayMiss = week.days.lastOrNull()?.missed.orEmpty()
        if (todayMiss.isNotEmpty()) {
            out += Signal(
                kind = SignalKind.COACH,
                title = "Still open today",
                body = todayMiss.joinToString(", "),
                action = "Close ${todayMiss.first()} first. The rest get easier.",
            )
        }

        if (week.perfectDays >= 2) {
            out += Signal(
                kind = SignalKind.WIN,
                title = "${week.perfectDays} perfect days this week",
                body = "You cleared every due habit on those dates. That is rare air.",
                action = "Stack one more perfect day before Sunday.",
                highlight = true,
            )
        }

        val best = habits.minByOrNull { it.rank }
        if (best != null && best.consistency30 >= 70) {
            out += Signal(
                kind = SignalKind.WIN,
                title = "${best.habit.title} is carrying the board",
                body = "Highest 30-day consistency at ${best.consistency30}%.",
                action = "Do not touch the recipe. Same time, same cue.",
                habitId = best.habit.id,
            )
        }

        val currentBest = active.maxOfOrNull { it.streak } ?: 0
        if (currentBest > 0 && currentBest >= longestStreakEver && currentBest >= 5) {
            out += Signal(
                kind = SignalKind.MILESTONE,
                title = "Longest streak of your life",
                body = "$currentBest days. The chain has never been this long.",
                action = "Tonight, decide tomorrow’s first habit before you sleep.",
                highlight = true,
            )
        }

        val xpToNext = GamificationEngine.xpToNextLevel(totalXp)
        val level = GamificationEngine.calculateLevel(totalXp)
        if (xpToNext in 1..180 && level > 0) {
            out += Signal(
                kind = SignalKind.MILESTONE,
                title = "Close to ${GamificationEngine.levelTitle(level + 1)}",
                body = "$xpToNext XP left. A focused evening can finish this.",
                action = "One more honest completion today.",
            )
        }

        val nextCount = listOf(50, 100, 250, 500, 1000, 2500).firstOrNull { it > totalCompletions }
        if (nextCount != null && nextCount - totalCompletions <= 12) {
            out += Signal(
                kind = SignalKind.MILESTONE,
                title = "${nextCount - totalCompletions} to $nextCount completions",
                body = "You are on the doorstep of a number you will remember.",
                action = "Do not waste a due day this week.",
            )
        }

        if (score.congratulations != null) {
            out += Signal(
                kind = SignalKind.WIN,
                title = score.headline,
                body = score.congratulations,
                action = score.nextMove,
                highlight = metrics.consistencyScore >= 80,
            )
        }

        if (metrics.bestWeekday != null) {
            out += Signal(
                kind = SignalKind.COACH,
                title = "${metrics.bestWeekday.label} is your proof day",
                body = "You show up most when the week hits ${metrics.bestWeekday.label}.",
                action = "Clone that day’s first cue onto your weakest day.",
            )
        }

        if (out.isEmpty()) {
            out += Signal(
                kind = SignalKind.COACH,
                title = "The board is new",
                body = "Plant three honest days and the dashboard starts talking back.",
                action = "Pick one habit. Mark it today.",
            )
        }
        return out.distinctBy { it.title + it.body }.take(12)
    }

    private fun metricStories(
        metrics: StatsEngine.DashboardMetrics,
        week: WeekDeepDive,
        longestStreakEver: Int,
        totalCompletions: Int,
        totalFocusMinutes: Int,
    ): Map<MetricKey, MetricStory> {
        val nextMilestone = listOf(50, 100, 250, 500, 1000, 2500, 5000)
            .firstOrNull { it > totalCompletions }
        return mapOf(
            MetricKey.Completions to MetricStory(
                title = "Total planted",
                value = "$totalCompletions",
                story = "Every mark is a vote. You have cast $totalCompletions so far. " +
                    "${metrics.allTimeCompletions} of them sit on the habits still on the board.",
                facts = listOf(
                    "This week" to "${week.done} marks",
                    "This month" to "${(metrics.thisMonthRate * 100).roundToInt()}% of due",
                    "Next milestone" to (nextMilestone?.let { "$it" } ?: "You passed the chart"),
                ),
                congratulations = if (totalCompletions >= 50)
                    "Fifty-plus completions. You are no longer dabbling."
                else null,
                nextMove = nextMilestone?.let { "You are ${it - totalCompletions} away from $it. Keep the daily minimum." }
                    ?: "Keep the same daily minimum. Legacy is a volume game.",
            ),
            MetricKey.PerfectDays to MetricStory(
                title = "Perfect days",
                value = "${metrics.perfectDays30}",
                story = "A perfect day means every due habit got a mark. " +
                    "You have ${metrics.perfectDays30} in the last 30 days, ${metrics.perfectDaysAll} in the last year.",
                facts = listOf(
                    "This week" to "${week.perfectDays}",
                    "Last 30 days" to "${metrics.perfectDays30}",
                    "Last 365 days" to "${metrics.perfectDaysAll}",
                ),
                congratulations = if (metrics.perfectDays30 >= 5)
                    "Five or more clean days this month. That is a standard, not a spike."
                else null,
                nextMove = "Aim for one perfect day in the next 48 hours. Write the order tonight.",
            ),
            MetricKey.Focus to MetricStory(
                title = "Focus time",
                value = if (metrics.focusHours >= 1f) "%.1fh".format(metrics.focusHours) else "${totalFocusMinutes}m",
                story = "Deep work hours recorded in Focus. This is the quiet half of the system. " +
                    "The minutes nobody else sees.",
                facts = listOf(
                    "Total minutes" to "$totalFocusMinutes",
                    "Hours" to "%.1f".format(metrics.focusHours),
                ),
                congratulations = if (totalFocusMinutes >= 60)
                    "You have already sat still for a full hour of focus. That is rare."
                else null,
                nextMove = "Stack a 25-minute focus session onto your strongest habit tomorrow.",
            ),
            MetricKey.Streak to MetricStory(
                title = "Best streak",
                value = "${metrics.longestStreak}d",
                story = "Current best live streak is ${metrics.longestStreak} days. " +
                    "Your all-time record is $longestStreakEver.",
                facts = listOf(
                    "Live best" to "${metrics.longestStreak} days",
                    "All-time" to "$longestStreakEver days",
                    "To a week" to "${(7 - metrics.longestStreak).coerceAtLeast(0)} days",
                ),
                congratulations = if (metrics.longestStreak >= 7)
                    "A week-long chain is already teaching your brain who you are."
                else null,
                nextMove = if (metrics.longestStreak < 7)
                    "Get any one habit to 7. The rest of the board will feel lighter."
                else
                    "Do not celebrate by adding five habits. Defend the chain.",
            ),
            MetricKey.MemberDays to MetricStory(
                title = "Days with Verdly",
                value = "${metrics.memberDays}",
                story = "You have been planting here for ${metrics.memberDays} day${if (metrics.memberDays == 1) "" else "s"}. " +
                    "The forest is a function of ordinary dates, not launches.",
                facts = listOf(
                    "Member days" to "${metrics.memberDays}",
                    "Marks / day" to if (metrics.memberDays == 0) "0"
                    else "%.1f".format(metrics.allTimeCompletions.toFloat() / metrics.memberDays),
                ),
                congratulations = if (metrics.memberDays >= 14)
                    "Two weeks in. Most people never make it past the novelty week."
                else null,
                nextMove = "Treat tomorrow like day one of the next ${metrics.memberDays} days.",
            ),
            MetricKey.MonthRate to MetricStory(
                title = "This month",
                value = "${(metrics.thisMonthRate * 100).roundToInt()}%",
                story = "Of every habit that was due this calendar month, you planted " +
                    "${(metrics.thisMonthRate * 100).roundToInt()}%. " +
                    "90-day rate sits at ${(metrics.rate90 * 100).roundToInt()}%.",
                facts = listOf(
                    "This month" to "${(metrics.thisMonthRate * 100).roundToInt()}%",
                    "Last 30 days" to "${(metrics.rate30 * 100).roundToInt()}%",
                    "Last 90 days" to "${(metrics.rate90 * 100).roundToInt()}%",
                ),
                congratulations = if (metrics.thisMonthRate >= 0.75f)
                    "Three-quarters of the month, planted. That is a professional month."
                else null,
                nextMove = if (metrics.thisMonthRate < metrics.rate90)
                    "This month is trailing your longer average. Win the next three due days in a row."
                else
                    "You are beating your longer baseline. Keep the same size. Do not swell the list.",
            ),
        )
    }

    private fun bestStreakFromHistory(habit: HabitItem): Int {
        val sorted = habit.completedDates.mapNotNull {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }.sorted()
        if (sorted.isEmpty()) return 0
        var best = 1
        var cur = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                cur++
                best = maxOf(best, cur)
            } else {
                cur = 1
            }
        }
        return maxOf(best, habit.streak)
    }

    private fun dowName(day: DayOfWeek): String =
        day.getDisplayName(TextStyle.FULL, Locale.getDefault())
}
