package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.notifications.ReminderScheduler
import com.saintnico.verdlyhabits.preferences.dataStore
import com.saintnico.verdlyhabits.ui.models.premiumHabitIcons
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.ui.utils.calculateStreak

data class HabitEntity(
    val id: String,
    val title: String,
    val iconName: String,
    var isCompleted: Boolean,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val reminderTime2: String? = null,
    val completedDates: Set<String>,
    val color: Long = 0xFF4CAF50,
    val plantedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val completionProofs: Map<String, String> = emptyMap(),
    // Phase 1 overhaul fields — nullable in persistence so old saves still deserialize.
    val difficulty: String? = null,
    val category: String? = null,
    val reminderWindow: String? = null,
    val linkedHabitId: String? = null,
    val isFavoriteFocus: Boolean = false,
    val frequency: String? = null,
    val customDaysMask: String? = null,
)

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    val habits = mutableStateListOf<HabitItem>()
    private val gson = Gson()
    private val HABITS_KEY = stringPreferencesKey("habits_list")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val userRepository = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()

    init {
        viewModelScope.launch {
            loadHabits()
        }
    }

    private suspend fun loadHabits() {
        val prefs = getApplication<Application>().dataStore.data.first()
        val json = prefs[HABITS_KEY]
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<HabitEntity>>() {}.type
            val entities: List<HabitEntity> = gson.fromJson(json, type)
            val today = LocalDate.now().format(dateFormatter)

            val loadedHabits = entities.map { entity ->
                val icon = premiumHabitIcons.find { it.vector.name == entity.iconName }?.vector
                    ?: premiumHabitIcons[0].vector
                
                // Reset isCompleted if the latest completed date isn't today
                val isCompletedToday = entity.completedDates.contains(today)
                
                HabitItem(
                    id = entity.id,
                    title = entity.title,
                    icon = icon,
                    streak = calculateStreak(entity.completedDates),
                    isCompleted = isCompletedToday,
                    reminderEnabled = entity.reminderEnabled,
                    reminderTime = entity.reminderTime,
                    reminderTime2 = entity.reminderTime2,
                    completedDates = entity.completedDates ?: emptySet(),
                    color = if (entity.color != 0L) entity.color else 0xFF4CAF50L,
                    plantedAt = if (entity.plantedAt > 0L) entity.plantedAt else System.currentTimeMillis(),
                    notes = entity.notes,
                    isPaused = entity.isPaused,
                    isArchived = entity.isArchived,
                    completionProofs = entity.completionProofs ?: emptyMap(),
                    difficulty = Difficulty.fromName(entity.difficulty),
                    category = HabitCategory.fromName(entity.category),
                    reminderWindow = ReminderWindow.fromName(entity.reminderWindow),
                    linkedHabitId = entity.linkedHabitId,
                    isFavoriteFocus = entity.isFavoriteFocus,
                    frequency = HabitFrequency.fromStored(entity.frequency),
                    customDaysMask = entity.customDaysMask,
                )
            }
            habits.clear()
            habits.addAll(loadedHabits)
            ReminderScheduler.scheduleAll(getApplication())
        }
        refreshHomeScreenWidgets()
    }

    private fun saveHabits() {
        viewModelScope.launch {
            val entities = habits.map {
                HabitEntity(
                    id = it.id,
                    title = it.title,
                    iconName = it.icon.name,
                    isCompleted = it.isCompleted,
                    reminderEnabled = it.reminderEnabled,
                    reminderTime = it.reminderTime,
                    reminderTime2 = it.reminderTime2,
                    completedDates = it.completedDates,
                    color = it.color,
                    plantedAt = it.plantedAt,
                    notes = it.notes,
                    isPaused = it.isPaused,
                    isArchived = it.isArchived,
                    completionProofs = it.completionProofs,
                    difficulty = it.difficulty.name,
                    category = it.category.name,
                    reminderWindow = it.reminderWindow.name,
                    linkedHabitId = it.linkedHabitId,
                    isFavoriteFocus = it.isFavoriteFocus,
                    frequency = it.frequency.name,
                    customDaysMask = it.customDaysMask,
                )
            }
            val json = gson.toJson(entities)
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[HABITS_KEY] = json
            }
            
            // Sync to Cloud
            try {
                userRepository.saveHabits(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            refreshHomeScreenWidgets()
        }
    }

    private fun refreshHomeScreenWidgets() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            com.saintnico.verdlyhabits.widget.WidgetSnapshotWriter.refreshHabitsFromLocalStore(app)
            com.saintnico.verdlyhabits.widget.WidgetRefresh.updateHabitsOnly(app)
        }
    }

    fun restoreFromFirestore() {
        viewModelScope.launch {
            try {
                val data = userRepository.fetchUserData()
                val json = data?.get("habitsJson") as? String
                if (!json.isNullOrEmpty()) {
                    getApplication<Application>().dataStore.edit { prefs ->
                        prefs[HABITS_KEY] = json
                    }
                    loadHabits()
                }
            } catch (_: Exception) {
                // Network/rules may fail — keep local cache rather than crash.
            }
        }
    }

    /** Reload in-memory habits after a local wipe (account switch / sign-out). */
    fun onAccountSessionChanged() {
        viewModelScope.launch {
            habits.forEach { ReminderScheduler.cancel(getApplication(), it.id) }
            habits.clear()
            loadHabits()
        }
    }

    fun addHabit(habit: HabitItem) {
        habits.add(0, habit) // Add to the top
        if (habit.reminderEnabled && (habit.reminderTime != null || habit.reminderTime2 != null)) {
            ReminderScheduler.schedule(getApplication(), habit)
        }
        saveHabits()
    }

    fun updateHabit(updated: HabitItem) {
        val index = habits.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            habits[index] = updated
            if (updated.reminderEnabled && (updated.reminderTime != null || updated.reminderTime2 != null)) {
                ReminderScheduler.schedule(getApplication(), updated)
            } else {
                ReminderScheduler.cancel(getApplication(), updated.id)
            }
            saveHabits()
        }
    }

    fun toggleHabitCompletion(id: String) {
        val index = habits.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = habits[index]
            val todayStr = LocalDate.now().format(dateFormatter)
            val newCompletedDates = item.completedDates.toMutableSet()
            
            val newIsCompleted = !item.isCompleted
            
            // STRICT: If already completed today, don't allow un-completing
            if (item.isCompleted && item.completedDates.contains(todayStr)) {
                return // Do nothing, stay completed
            }

            if (newIsCompleted) {
                newCompletedDates.add(todayStr)
            } else {
                newCompletedDates.remove(todayStr)
            }
            
            val updatedItem = item.copy(
                isCompleted = newIsCompleted,
                completedDates = newCompletedDates,
                streak = calculateStreak(newCompletedDates)
            )
            habits[index] = updatedItem
            saveHabits()
        }
    }

    fun deleteHabit(id: String) {
        val index = habits.indexOfFirst { it.id == id }
        if (index != -1) {
            habits.removeAt(index)
            ReminderScheduler.cancel(getApplication(), id)
            saveHabits()
        }
    }

    fun archiveHabit(id: String) {
        val index = habits.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = habits[index]
            habits[index] = item.copy(isArchived = true)
            ReminderScheduler.cancel(getApplication(), id)
            saveHabits()
        }
    }

    fun pauseHabit(id: String) {
        val index = habits.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = habits[index]
            val newPausedState = !item.isPaused
            habits[index] = item.copy(isPaused = newPausedState)
            if (newPausedState) {
                ReminderScheduler.cancel(getApplication(), id)
            } else if (item.reminderEnabled && (item.reminderTime != null || item.reminderTime2 != null)) {
                ReminderScheduler.schedule(getApplication(), item)
            }
            saveHabits()
        }
    }

    fun clearAllHabits() {
        habits.forEach { habit ->
            ReminderScheduler.cancel(getApplication(), habit.id)
        }
        habits.clear()
        saveHabits()
    }

    /**
     * Mark exactly one habit as the user's "Today's Focus" pinned card. Clears any
     * previous pin. Pass null to unpin.
     */
    fun setTodayFocus(habitId: String?) {
        var changed = false
        val ids = habits.map { it.id }
        for (i in ids.indices) {
            val item = habits[i]
            val shouldBePinned = item.id == habitId
            if (item.isFavoriteFocus != shouldBePinned) {
                habits[i] = item.copy(isFavoriteFocus = shouldBePinned)
                changed = true
            }
        }
        if (changed) saveHabits()
    }

    /** Returns the chain of habits stacked after [parentId] in order. */
    fun stackedAfter(parentId: String): List<HabitItem> {
        val result = mutableListOf<HabitItem>()
        val seen = mutableSetOf(parentId)
        var cursor = parentId
        while (true) {
            val next = habits.firstOrNull { it.linkedHabitId == cursor && it.id !in seen } ?: break
            result += next
            seen += next.id
            cursor = next.id
        }
        return result
    }

    fun completeHabitWithProof(habit: HabitItem, photoUrl: String) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index != -1) {
            val item = habits[index]
            val todayStr = LocalDate.now().format(dateFormatter)
            val newCompletedDates = item.completedDates.toMutableSet()
            newCompletedDates.add(todayStr)
            
            val newProofs = item.completionProofs.toMutableMap()
            newProofs[todayStr] = photoUrl
            
            val updatedItem = item.copy(
                isCompleted = true,
                completedDates = newCompletedDates,
                streak = calculateStreak(newCompletedDates),
                completionProofs = newProofs
            )
            habits[index] = updatedItem
            saveHabits()
        }
    }
}
