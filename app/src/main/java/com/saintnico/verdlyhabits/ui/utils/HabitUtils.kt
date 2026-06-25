package com.saintnico.verdlyhabits.ui.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun calculateStreak(completedDates: Set<String>): Int {
    if (completedDates.isEmpty()) return 0
    
    var streak = 0
    var currentDate = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    val todayStr = currentDate.format(dateFormatter)
    if (!completedDates.contains(todayStr)) {
        currentDate = currentDate.minusDays(1)
    }

    while (completedDates.contains(currentDate.format(dateFormatter))) {
        streak++
        currentDate = currentDate.minusDays(1)
    }
    
    return streak
}

fun getPlantedDateText(plantedAt: Long): String {
    val instant = java.time.Instant.ofEpochMilli(plantedAt)
    val date = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return date.format(formatter)
}
