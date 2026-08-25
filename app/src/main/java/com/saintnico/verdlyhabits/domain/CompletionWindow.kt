package com.saintnico.verdlyhabits.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Time window within which a habit should be completed.
 * Hard / Epic habits require a window; Medium encourages one; Easy is all-day.
 */
object CompletionWindow {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun requiresWindow(difficulty: Difficulty): Boolean =
        difficulty == Difficulty.HARD || difficulty == Difficulty.EPIC

    fun supportsOptionalWindow(difficulty: Difficulty): Boolean =
        difficulty == Difficulty.MEDIUM

    /** Suggested defaults when the user picks a harder tier. */
    fun suggestedRange(difficulty: Difficulty): Pair<String, String> = when (difficulty) {
        Difficulty.EPIC -> "05:00" to "07:00"
        Difficulty.HARD -> "06:00" to "09:00"
        Difficulty.MEDIUM -> "07:00" to "10:00"
        Difficulty.EASY -> "08:00" to "20:00"
    }

    fun minSpanMinutes(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EPIC -> 60
        Difficulty.HARD -> 90
        Difficulty.MEDIUM -> 45
        Difficulty.EASY -> 30
    }

    fun maxSpanMinutes(difficulty: Difficulty): Int? = when (difficulty) {
        Difficulty.EPIC -> 180
        Difficulty.HARD -> 300
        Difficulty.MEDIUM -> 360
        Difficulty.EASY -> null
    }

    fun parse(time: String?): LocalTime? = runCatching {
        time?.let { LocalTime.parse(it, timeFormatter) }
    }.getOrNull()

    fun spanMinutes(start: String, end: String): Int? {
        val s = parse(start) ?: return null
        val e = parse(end) ?: return null
        if (!e.isAfter(s)) return null
        return ((e.toSecondOfDay() - s.toSecondOfDay()) / 60)
    }

    fun isValidRange(start: String?, end: String?, difficulty: Difficulty): Boolean {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return false
        val minutes = spanMinutes(start, end) ?: return false
        if (minutes < minSpanMinutes(difficulty)) return false
        maxSpanMinutes(difficulty)?.let { max -> if (minutes > max) return false }
        return true
    }

    fun isActiveNow(start: String?, end: String?, now: LocalTime = LocalTime.now()): Boolean {
        val s = parse(start) ?: return true
        val e = parse(end) ?: return true
        return !now.isBefore(s) && !now.isAfter(e)
    }

    fun formatRange(start: String?, end: String?): String {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return "Any time"
        return "$start – $end"
    }

    /** Bonus XP when a Hard/Epic habit is checked off inside its commitment window. */
    fun onTimeBonusXp(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EPIC -> 25
        Difficulty.HARD -> 15
        Difficulty.MEDIUM -> 5
        Difficulty.EASY -> 0
    }

    fun windowHint(difficulty: Difficulty): String = when (difficulty) {
        Difficulty.EPIC ->
            "Epic habits need a tight window (1–3 hours). Complete inside it for bonus XP."
        Difficulty.HARD ->
            "Hard habits need a commitment window (at least 90 min). Finish inside it for bonus XP."
        Difficulty.MEDIUM ->
            "Optional: set a window to earn a small on-time bonus."
        Difficulty.EASY ->
            "Easy habits count any time today — no window required."
    }
}
