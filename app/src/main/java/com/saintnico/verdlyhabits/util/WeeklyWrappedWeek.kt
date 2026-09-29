package com.saintnico.verdlyhabits.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Monday-start week key for home Weekly Wrapped visibility (e.g. `2026-08-25`). */
object WeeklyWrappedWeek {
    fun currentKey(): String =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
}
