package com.saintnico.verdlyhabits.ui.screens.goals.detail

import java.util.concurrent.TimeUnit

object GoalNarrativeEngine {
    fun generateNarrative(
        startDate: Long,
        targetDate: Long,
        currentValue: Float,
        targetValue: Float?,
        energyLastUpdated: Long
    ): String {
        val now = System.currentTimeMillis()
        val daysSinceActivity = TimeUnit.MILLISECONDS.toDays(now - energyLastUpdated)

        if (daysSinceActivity >= 7) {
            return "This goal needs you back."
        }

        val totalDuration = targetDate - startDate
        val elapsed = now - startDate
        val timeProgress = if (totalDuration > 0) (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 1f

        val valueProgress = if (targetValue != null && targetValue > 0) {
            (currentValue / targetValue).coerceIn(0f, 1f)
        } else {
            // For non-REACH goals, we might use time or linked habit completion as progress
            timeProgress
        }

        if (elapsed < TimeUnit.DAYS.toMillis(2)) {
            return "Day 1. Every legend starts here."
        }

        if (valueProgress >= 0.8f) {
            return "You're in the home stretch."
        }

        val progressDiff = valueProgress - timeProgress

        return when {
            progressDiff > 0.1f -> "You're ahead of pace. Keep this up and you'll hit your goal early."
            progressDiff < -0.1f -> "You're a bit behind schedule. Adjusting your timeline takes just a tap."
            else -> "You're right on track. Stay consistent."
        }
    }
}
