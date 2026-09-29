package com.saintnico.verdlyhabits.engine

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.pow

/**
 * Loop Habit Tracker–style score: an exponential weighted moving average that
 * grows on completion and decays on missed days.
 *
 *   score_t = score_{t-1} * decay + completed_t * (1 - decay)
 *
 * decay is tuned so a fully consistent habit converges to ~1.0 within ~60 days
 * and a streak break causes a visible but not catastrophic drop.
 *
 * Reference: iSoron/uhabits — `Score.java::compute()`.
 */
object HabitScoreEngine {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** How many days back to evaluate when computing the current score. */
    const val LOOKBACK_DAYS: Int = 90

    /**
     * Compute the score in [0f, 1f] for a habit, given its completion history.
     * Uses [LOOKBACK_DAYS] of history.
     */
    fun score(
        completedDates: Set<String>,
        today: LocalDate = LocalDate.now(),
        plantedAtMillis: Long = 0L
    ): Float {
        if (completedDates.isEmpty()) return 0f

        // Strength constant — chosen so a 30-day streak ≈ 0.85, a 60-day ≈ 0.95
        val freq = 1.0  // daily habits — could become a parameter for weekly habits
        val multiplier = 0.5.pow(1.0 / (13.0 / freq)) // ~13-day half life

        var score = 0.0
        // Start from the planted date or the lookback window, whichever is later
        val plantedDate = if (plantedAtMillis > 0L) {
            java.time.Instant.ofEpochMilli(plantedAtMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else today.minusDays(LOOKBACK_DAYS.toLong())

        val start = maxOf(plantedDate, today.minusDays(LOOKBACK_DAYS.toLong()))
        var cursor = start
        while (!cursor.isAfter(today)) {
            val completed = completedDates.contains(cursor.format(fmt))
            score = if (completed) {
                score + (1.0 - score) * (1.0 - multiplier)
            } else {
                score * multiplier
            }
            cursor = cursor.plusDays(1)
        }
        return score.toFloat().coerceIn(0f, 1f)
    }

    /** Returns score as a percent string, e.g. "82%". */
    fun scoreLabel(score: Float): String = "${(score * 100).toInt()}%"

    /**
     * Tier label for a score — helps the UI surface "Building", "Strong", etc.
     * without raw numbers.
     */
    fun scoreTier(score: Float): String = when {
        score < 0.20f -> "Forming"
        score < 0.45f -> "Building"
        score < 0.70f -> "Strong"
        score < 0.90f -> "Anchored"
        else          -> "Mastered"
    }
}
