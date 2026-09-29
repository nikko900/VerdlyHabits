package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * One-day snapshot for an active goal — the number the user sees on the goals list,
 * reframed as something shareable for today only.
 */
object GoalDailyRecapEngine {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val isoDay = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayDay = DateTimeFormatter.ofPattern("EEEE, MMM d")

    data class DailyRecap(
        val dateLabel: String,
        /** Large hero stat shown on card and in the sheet preview. */
        val heroNumber: String,
        val cheerLine: String,
        val statLine: String,
        /** 0–100 for progress ring on share card. */
        val todayProgressPercent: Int,
    )

    fun build(
        goal: GoalEntity,
        habits: List<HabitItem>,
        metrics: GoalProgressEngine.GoalMetrics,
    ): DailyRecap {
        val today = LocalDate.now(zone)
        val todayStr = today.format(isoDay)
        val linked = GoalProgressEngine.linkedHabits(goal, habits)
        val completedToday = linked.count { it.completedDates.contains(todayStr) }
        val linkedCount = linked.size.coerceAtLeast(1)
        val todayFraction = (completedToday.toFloat() / linkedCount).coerceIn(0f, 1f)

        return when (goal.goalType) {
            GoalType.REACH -> reachRecap(goal, linked, metrics, today, completedToday, todayFraction)
            GoalType.BUILD -> buildRecap(metrics, completedToday, linkedCount, today, todayFraction)
            GoalType.QUIT -> quitRecap(metrics, today)
            GoalType.MAINTAIN -> maintainRecap(metrics, completedToday, linkedCount, goal, today, todayFraction)
        }
    }

    private fun reachRecap(
        goal: GoalEntity,
        linked: List<HabitItem>,
        metrics: GoalProgressEngine.GoalMetrics,
        today: LocalDate,
        completedToday: Int,
        todayFraction: Float,
    ): DailyRecap {
        val target = goal.targetValue?.takeIf { it > 0f }
        val current = if (linked.isNotEmpty()) {
            linked.sumOf { it.completedDates.size }.toFloat()
        } else {
            goal.currentValue
        }
        val unit = goal.unit.trim().let { if (it.isNotEmpty()) " $it" else "" }
        val hero = if (target != null) {
            "${formatNum(current)} of ${formatNum(target)}$unit"
        } else {
            metrics.heroPrimary
        }
        val todayLine = when {
            completedToday > 0 -> "+$completedToday rep${if (completedToday == 1) "" else "s"} today"
            else -> "No reps logged today yet"
        }
        val cheer = when {
            completedToday > 0 -> "Another step toward \"${goal.title}\"."
            metrics.progressFraction >= 0.75f -> "Close enough to taste it. One rep today counts."
            else -> "The number moves when you move."
        }
        return DailyRecap(
            dateLabel = today.format(displayDay),
            heroNumber = hero,
            cheerLine = cheer,
            statLine = todayLine,
            todayProgressPercent = (todayFraction * 100f).roundToInt(),
        )
    }

    private fun buildRecap(
        metrics: GoalProgressEngine.GoalMetrics,
        completedToday: Int,
        linkedCount: Int,
        today: LocalDate,
        todayFraction: Float,
    ): DailyRecap {
        val hero = if (linkedCount <= 1) {
            if (completedToday > 0) "Done today" else "Not yet today"
        } else {
            "$completedToday/$linkedCount today"
        }
        val cheer = when {
            completedToday == linkedCount && linkedCount > 0 -> "Full rhythm today."
            completedToday > 0 -> "Building block by block."
            else -> "Room to show up. Today is still open."
        }
        return DailyRecap(
            dateLabel = today.format(displayDay),
            heroNumber = hero,
            cheerLine = cheer,
            statLine = metrics.heroPrimary,
            todayProgressPercent = (todayFraction * 100f).roundToInt(),
        )
    }

    private fun quitRecap(
        metrics: GoalProgressEngine.GoalMetrics,
        today: LocalDate,
    ): DailyRecap {
        val days = metrics.cleanStreakDays.coerceAtLeast(0)
        val hero = if (days == 1) "1 day clear" else "$days days clear"
        val cheer = when {
            days >= 30 -> "A month of freedom. Today included."
            days >= 7 -> "Another clear day in the streak."
            days >= 1 -> "Today counts."
            else -> "Today can be day one."
        }
        return DailyRecap(
            dateLabel = today.format(displayDay),
            heroNumber = hero,
            cheerLine = cheer,
            statLine = "Clear through today",
            todayProgressPercent = if (days > 0) 100 else 0,
        )
    }

    private fun maintainRecap(
        metrics: GoalProgressEngine.GoalMetrics,
        completedToday: Int,
        linkedCount: Int,
        goal: GoalEntity,
        today: LocalDate,
        todayFraction: Float,
    ): DailyRecap {
        val floor = goal.floorCount ?: linkedCount
        val hero = when {
            metrics.floorMetThisWeek && completedToday > 0 -> "Floor met"
            completedToday >= floor -> "Standard held"
            else -> "$completedToday/$floor today"
        }
        val cheer = when {
            metrics.floorMetThisWeek -> "You held the line this week."
            completedToday > 0 -> "Protecting the standard, one day at a time."
            else -> "The floor is still reachable today."
        }
        return DailyRecap(
            dateLabel = today.format(displayDay),
            heroNumber = hero,
            cheerLine = cheer,
            statLine = metrics.heroSecondary ?: metrics.heroPrimary,
            todayProgressPercent = (todayFraction * 100f).roundToInt().coerceAtLeast(
                if (metrics.floorMetThisWeek) 100 else 0,
            ),
        )
    }

    private fun formatNum(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString() else String.format("%.1f", v)
}
