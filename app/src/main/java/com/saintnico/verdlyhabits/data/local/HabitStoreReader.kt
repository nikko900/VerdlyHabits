package com.saintnico.verdlyhabits.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.preferences.dataStore
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.ui.viewmodel.HabitEntity
import kotlinx.coroutines.flow.first
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry

/** Reads persisted habits for background workers (goals sync, nudges). */
object HabitStoreReader {

    private val habitsKey = stringPreferencesKey("habits_list")
    private val gson = Gson()

    suspend fun loadHabits(context: Context): List<HabitItem> =
        loadHabitsForReminders(context)

    suspend fun loadHabitsForReminders(context: Context): List<HabitItem> {
        return try {
            val json = context.dataStore.data.first()[habitsKey] ?: return emptyList()
            val type = object : TypeToken<List<HabitEntity>>() {}.type
            val entities: List<HabitEntity> = gson.fromJson(json, type) ?: return emptyList()
            entities.filter { !it.isArchived }.map { e ->
                HabitItem(
                    id = e.id,
                    title = e.title,
                    icon = HabitIconRegistry.iconFor(e.iconName),
                    color = if (e.color != 0L) e.color else 0xFF52B788L,
                    streak = e.completedDates.size.coerceAtLeast(0),
                    isCompleted = e.isCompleted,
                    reminderEnabled = e.reminderEnabled,
                    reminderTime = e.reminderTime,
                    reminderTime2 = e.reminderTime2,
                    completedDates = e.completedDates,
                    isPaused = e.isPaused,
                    isArchived = e.isArchived,
                    frequency = HabitFrequency.fromStored(e.frequency),
                    customDaysMask = e.customDaysMask,
                    category = HabitCategory.fromName(e.category),
                    difficulty = Difficulty.fromName(e.difficulty),
                    reminderWindow = ReminderWindow.fromName(e.reminderWindow),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
