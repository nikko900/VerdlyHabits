package com.saintnico.verdlyhabits.domain

/**
 * Smart reminder time window. EXACT means use the habit's `reminderTime` field directly;
 * the windows pick a randomized but stable in-window time per habit so multiple habits
 * in the same window don't all fire at once.
 */
enum class ReminderWindow(
    val displayName: String,
    val startHour: Int,
    val endHour: Int
) {
    MORNING  ("Morning",   6,  9),
    AFTERNOON("Afternoon", 12, 15),
    EVENING  ("Evening",   18, 21),
    EXACT    ("Exact",     -1, -1);

    companion object {
        fun fromName(name: String?): ReminderWindow =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: EXACT
    }
}
