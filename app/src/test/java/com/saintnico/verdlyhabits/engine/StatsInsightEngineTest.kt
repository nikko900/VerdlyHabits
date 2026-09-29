package com.saintnico.verdlyhabits.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsInsightEngineTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 13)

    private fun habit(
        id: String,
        title: String,
        done: List<LocalDate>,
        streak: Int = done.size,
        frequency: HabitFrequency = HabitFrequency.DAILY,
    ) = HabitItem(
        id = id,
        title = title,
        icon = Icons.Filled.Star,
        streak = streak,
        completedDates = done.map { it.toString() }.toSet(),
        frequency = frequency,
    )

    @Test
    fun dueAwareScoreRewardsScheduledDaysOnly() {
        val weekendOnly = habit(
            id = "w",
            title = "Long run",
            done = listOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 6)),
            streak = 1,
            frequency = HabitFrequency.WEEKENDS,
        )
        val metrics = StatsEngine.computeDashboard(listOf(weekendOnly), today)
        assertTrue(metrics.habitRankings.first().consistency30 >= 20)
        assertTrue(metrics.consistencyScore >= 0)
    }

    @Test
    fun strongWeekProducesWinSignalsAndRisingMomentum() {
        val dates = (0 until 30).map { today.minusDays(it.toLong()) }
        val water = habit("water", "Water", dates, streak = 30)
        val metrics = StatsEngine.computeDashboard(listOf(water), today)
        val report = StatsInsightEngine.analyze(
            habits = listOf(water),
            metrics = metrics,
            longestStreakEver = 30,
            totalCompletions = 30,
            totalXp = 400,
            totalFocusMinutes = 40,
            today = today,
        )
        assertEquals("Elite", metrics.consistencyTier)
        assertTrue(report.signals.any { it.kind == StatsInsightEngine.SignalKind.WIN })
        assertTrue(report.score.score >= 80)
        assertTrue(report.week.rate >= 0.9f)
        assertTrue(report.habits.first().congratulations != null)
    }

    @Test
    fun missedDaysSurfaceLeakAndCoach() {
        val done = listOf(today.minusDays(5), today.minusDays(6))
        val journal = habit("j", "Journal", done, streak = 0)
        val metrics = StatsEngine.computeDashboard(listOf(journal), today)
        val report = StatsInsightEngine.analyze(
            habits = listOf(journal),
            metrics = metrics,
            longestStreakEver = 6,
            totalCompletions = 2,
            totalXp = 20,
            totalFocusMinutes = 0,
            today = today,
        )
        assertTrue(report.habits.first().missStreak >= 3)
        assertTrue(report.signals.any { it.kind == StatsInsightEngine.SignalKind.LEAK || it.kind == StatsInsightEngine.SignalKind.COACH })
        assertTrue(report.week.days.last().missed.contains("Journal"))
    }

    @Test
    fun metricStoriesCoverTheBoard() {
        val habit = habit("a", "Read", listOf(today), streak = 1)
        val metrics = StatsEngine.computeDashboard(listOf(habit), today, totalFocusMinutes = 90)
        val report = StatsInsightEngine.analyze(
            habits = listOf(habit),
            metrics = metrics,
            longestStreakEver = 1,
            totalCompletions = 1,
            totalXp = 10,
            totalFocusMinutes = 90,
            today = today,
        )
        assertEquals(StatsInsightEngine.MetricKey.entries.size, report.metrics.size)
        assertTrue(report.metrics.getValue(StatsInsightEngine.MetricKey.Focus).value.contains("h") ||
            report.metrics.getValue(StatsInsightEngine.MetricKey.Focus).value.contains("m"))
    }
}
