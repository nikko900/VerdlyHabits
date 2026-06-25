package com.saintnico.verdlyhabits.domain

import androidx.compose.ui.graphics.Color

/**
 * Coarse category tag for a habit. Used for filtering, theming the chip accent,
 * and dashboard analytics. Mirrors Fabulous-style life areas.
 */
enum class HabitCategory(
    val displayName: String,
    val accent: Long
) {
    HEALTH   ("Health",   0xFFEF5350),
    MIND     ("Mind",     0xFF7C3AED),
    FINANCE  ("Finance",  0xFFFFB300),
    SOCIAL   ("Social",   0xFF26C6DA),
    CREATIVE ("Creative", 0xFFEC407A),
    OTHER    ("Other",    0xFF78909C);

    val color: Color get() = Color(accent)

    companion object {
        fun fromName(name: String?): HabitCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
    }
}
