package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Derives live goal progress from linked habits and the goal timeline.
 * Used by [com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel] to keep Room in sync.
 */
object GoalProgressEngine {

    private val dayMs = 24 * 60 * 60 * 1000L
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun linkedIds(goal: GoalEntity): List<String> =
        goal.linkedHabitIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun linkedHabits(goal: GoalEntity, habits: List<HabitItem>): List<HabitItem> {
        val ids = linkedIds(goal).toSet()
        return habits.filter { it.id in ids && !it.isArchived }
    }

    /** Progress fraction 0..1 for milestone + UI rings. */
    fun progressFraction(goal: GoalEntity, habits: List<HabitItem>): Float {
        val linked = linkedHabits(goal, habits)
        val totalDays = goalDurationDays(goal).coerceAtLeast(1)

        val habitBased = when (goal.goalType) {
            GoalType.REACH -> {
                val target = goal.targetValue ?: return timeProgress(goal)
                if (target <= 0f || linked.isEmpty()) return timeProgress(goal)
                (linked.sumOf { it.completedDates.size }.toFloat() / target).coerceIn(0f, 1f)
            }
            GoalType.BUILD, GoalType.MAINTAIN -> {
                if (linked.isEmpty()) return timeProgress(goal)
                (countPerfectLinkedDays(goal, linked).toFloat() / totalDays).coerceIn(0f, 1f)
            }
            GoalType.QUIT -> {
                if (linked.isEmpty()) return timeProgress(goal)
                val bestStreak = linked.maxOfOrNull { it.streak } ?: 0
                (bestStreak.toFloat() / totalDays).coerceIn(0f, 1f)
            }
        }
        return habitBased
    }

    /** Numeric value stored on [GoalEntity.currentValue]. */
    fun currentValue(goal: GoalEntity, habits: List<HabitItem>): Float {
        val fraction = progressFraction(goal, habits)
        return when (goal.goalType) {
            GoalType.REACH -> linkedHabits(goal, habits).sumOf { it.completedDates.size }.toFloat()
            else -> fraction * 100f
        }
    }

    fun goalDurationDays(goal: GoalEntity): Int =
        ((goal.targetDate - goal.startDate) / dayMs).toInt().coerceAtLeast(1)

    /** True when the user missed all linked habits yesterday (for why-reminder nudges). */
    fun missedYesterday(goal: GoalEntity, habits: List<HabitItem>): Boolean {
        val linked = linkedHabits(goal, habits)
        if (linked.isEmpty()) return false
        val yesterday = LocalDate.now().minusDays(1).format(dateFmt)
        return linked.none { it.completedDates.contains(yesterday) }
    }

    private fun timeProgress(goal: GoalEntity): Float {
        val span = (goal.targetDate - goal.startDate).coerceAtLeast(1L)
        return ((System.currentTimeMillis() - goal.startDate).toFloat() / span).coerceIn(0f, 1f)
    }

    private fun countPerfectLinkedDays(goal: GoalEntity, linked: List<HabitItem>): Int {
        val start = Instant.ofEpochMilli(goal.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val end = minOf(
            LocalDate.now(),
            Instant.ofEpochMilli(goal.targetDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        var count = 0
        var d = start
        while (!d.isAfter(end)) {
            val key = d.format(dateFmt)
            if (linked.all { it.completedDates.contains(key) }) count++
            d = d.plusDays(1)
        }
        return count
    }
}
