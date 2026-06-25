package com.saintnico.verdlyhabits.domain

import java.time.LocalDate

object HabitScheduling {

    /**
     * [customDaysMask]: seven chars Mon..Sun, `'1'` = scheduled, anything else = off.
     * If null/short for CUSTOM, treated as "every day" so older saves still behave.
     */
    fun isDueOn(frequency: HabitFrequency, customDaysMask: String?, date: LocalDate): Boolean {
        return when (frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKDAYS -> date.dayOfWeek.value <= 5 // Mon–Fri
            HabitFrequency.WEEKENDS -> date.dayOfWeek.value >= 6 // Sat–Sun
            HabitFrequency.CUSTOM -> customMatches(customDaysMask, date)
        }
    }

    fun maskFromBooleans(daysMonFirst: BooleanArray): String {
        return daysMonFirst.take(7).joinToString("") { if (it) "1" else "0" }.padEnd(7, '0')
    }

    fun booleansFromMask(mask: String?): BooleanArray {
        val out = BooleanArray(7) { false }
        if (mask.isNullOrBlank()) return out
        for (i in 0 until 7) {
            out[i] = mask.getOrNull(i) == '1'
        }
        return out
    }

    private fun customMatches(mask: String?, date: LocalDate): Boolean {
        if (mask.isNullOrBlank() || mask.length < 7) return true
        val idx = date.dayOfWeek.value - 1 // Monday = 0
        return mask.getOrNull(idx) == '1'
    }
}
