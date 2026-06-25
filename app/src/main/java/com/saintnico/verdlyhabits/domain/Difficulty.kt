package com.saintnico.verdlyhabits.domain

import androidx.compose.ui.graphics.Color

/**
 * Habit difficulty tier — affects XP earned per completion and the card accent color.
 * Stored on each HabitItem; defaults to EASY for backward-compat with older saves.
 */
enum class Difficulty(
    val displayName: String,
    val xp: Int,
    val accent: Long
) {
    EASY  ("Easy",   xp = 10,  accent = 0xFF66BB6A),   // soft green
    MEDIUM("Medium", xp = 25,  accent = 0xFF42A5F5),   // calm blue
    HARD  ("Hard",   xp = 50,  accent = 0xFFFF9800),   // amber
    EPIC  ("Epic",   xp = 100, accent = 0xFF7C3AED);   // royal purple

    val color: Color get() = Color(accent)

    companion object {
        fun fromName(name: String?): Difficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EASY
    }
}
