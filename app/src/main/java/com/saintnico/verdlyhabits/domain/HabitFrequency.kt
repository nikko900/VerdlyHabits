package com.saintnico.verdlyhabits.domain

/**
 * How often a habit is expected to be done. Persisted by enum [name] (e.g. DAILY).
 */
enum class HabitFrequency(val label: String) {
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    CUSTOM("Custom"),
    ;

    companion object {
        fun fromStored(value: String?): HabitFrequency {
            if (value.isNullOrBlank()) return DAILY
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.label.equals(value, ignoreCase = true) }
                ?: DAILY
        }
    }
}
