package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.data.streak.StreakRepository
import com.saintnico.verdlyhabits.data.engagement.EngagementKind
import com.saintnico.verdlyhabits.data.engagement.SessionEngagementTracker
import com.saintnico.verdlyhabits.domain.CompletionWindow
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.engine.TrophyBus
import com.saintnico.verdlyhabits.engine.TrophyEvent
import com.saintnico.verdlyhabits.domain.streak.CompletionSource
import com.saintnico.verdlyhabits.notifications.ReminderScheduler
import com.saintnico.verdlyhabits.preferences.dataStore
import com.saintnico.verdlyhabits.streak.DayChangeCoordinator
import com.saintnico.verdlyhabits.streak.StreakNotifier
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val lifeNote: String? = null,
    val endedAt: Long? = null,
    val completionProofs: Map<String, String> = emptyMap(),
    val photoProofEnabled: Boolean = false,
    val difficulty: String? = null,
    val category: String? = null,
    val reminderWindow: String? = null,
    val linkedHabitId: String? = null,
    val isFavoriteFocus: Boolean = false,
    val frequency: String? = null,
    val customDaysMask: String? = null,
    val completionWindowStart: String? = null,
    val completionWindowEnd: String? = null,
)

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    val habits = mutableStateListOf<HabitItem>()
    /** False until the first DataStore/Firestore hydrate finishes — avoids empty-state flicker. */
    var habitsHydrated by mutableStateOf(false)
        private set
    private val gson = Gson()
    private val HABITS_KEY = stringPreferencesKey("habits_list")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val userRepository = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()
    private val streakRepo = StreakRepository.get(application)

    init {
        viewModelScope.launch {
            runCatching { streakRepo.migrateLegacyIfNeeded() }
            loadHabits()
            runCatching { DayChangeCoordinator.start(getApplication()) }
            runCatching { StreakNotifier.rescheduleAll(getApplication()) }
        }
        viewModelScope.launch {
            streakRepo.snapshot.collect { snap ->
                runCatching {
                    applyStreakSnapshot(snap.results.mapValues { it.value.currentStreak to it.value.isCompleteToday })
                }
            }
        }
    }

    /**
     * Overlays engine-owned streak/completion onto the in-memory habit list. Habit
     * definitions stay in DataStore; streak maths never do.
     */
    private fun applyStreakSnapshot(byId: Map<String, Pair<Int, Boolean>>) {
        if (habits.isEmpty() || byId.isEmpty()) return
        for (i in habits.indices) {
            val item = habits[i]
            val overlay = byId[item.id] ?: continue
            val (streak, completeToday) = overlay
            if (item.streak != streak || item.isCompleted != completeToday) {
                habits[i] = item.copy(streak = streak, isCompleted = completeToday)
            }
        }
    }

    private suspend fun loadHabits() {
        val prefs = getApplication<Application>().dataStore.data.first()
        val json = prefs[HABITS_KEY]
        if (!json.isNullOrEmpty()) {
            val loaded = applyHabitJson(json)
            if (!loaded) {
                restoreFromFirestoreNow()
            }
        } else {
            // Fresh install / wiped cache — habits live in Firestore, not on device.
            restoreFromFirestoreNow()
        }
        streakRepo.invalidateSchedules()
        streakRepo.refreshDay()
        streakRepo.reconcile()
        refreshHomeScreenWidgets()
        habitsHydrated = true
    }

    private suspend fun applyHabitJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<HabitEntity>>() {}.type
            val entities: List<HabitEntity> = gson.fromJson(json, type) ?: emptyList()
            val results = streakRepo.evaluateAll()

            val loadedHabits = entities.map { entity ->
                val icon = HabitIconRegistry.iconFor(entity.iconName)
                val completedDates = entity.completedDates ?: emptySet()
                val engine = results[entity.id]

                HabitItem(
                    id = entity.id,
                    title = entity.title,
                    icon = icon,
                    streak = engine?.currentStreak ?: 0,
                    isCompleted = engine?.isCompleteToday == true,
                    reminderEnabled = entity.reminderEnabled,
                    reminderTime = entity.reminderTime,
                    reminderTime2 = entity.reminderTime2,
                    completedDates = completedDates,
                    color = if (entity.color != 0L) entity.color else 0xFF4CAF50L,
                    plantedAt = if (entity.plantedAt > 0L) entity.plantedAt else System.currentTimeMillis(),
                    notes = entity.notes,
                    isPaused = entity.isPaused,
                    isArchived = entity.isArchived,
                    lifeNote = entity.lifeNote,
                    endedAt = entity.endedAt,
                    completionProofs = entity.completionProofs ?: emptyMap(),
                    photoProofEnabled = entity.photoProofEnabled,
                    difficulty = Difficulty.fromName(entity.difficulty),
                    category = HabitCategory.fromName(entity.category),
                    reminderWindow = ReminderWindow.fromName(entity.reminderWindow),
                    linkedHabitId = entity.linkedHabitId,
                    isFavoriteFocus = entity.isFavoriteFocus,
                    frequency = HabitFrequency.fromStored(entity.frequency),
                    customDaysMask = entity.customDaysMask,
                    completionWindowStart = entity.completionWindowStart,
                    completionWindowEnd = entity.completionWindowEnd,
                )
            }
            habits.clear()
            habits.addAll(loadedHabits)
            ReminderScheduler.scheduleAll(getApplication())
            val needsIconMigration = entities.any { e ->
                HabitIconRegistry.premiumIconForId(e.iconName) == null &&
                    e.iconName != HabitIconRegistry.stableId(HabitIconRegistry.iconFor(e.iconName))
            }
            if (needsIconMigration) saveHabits()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun saveHabits() {
        viewModelScope.launch {
            val entities = habits.map {
                HabitEntity(
                    id = it.id,
                    title = it.title,
                    iconName = HabitIconRegistry.stableId(it.icon),
                    // Derived from the engine at load; persisted only for legacy readers.
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
                    lifeNote = it.lifeNote,
                    endedAt = it.endedAt,
                    completionProofs = it.completionProofs,
                    photoProofEnabled = it.photoProofEnabled,
                    difficulty = it.difficulty.name,
                    category = it.category.name,
                    reminderWindow = it.reminderWindow.name,
                    linkedHabitId = it.linkedHabitId,
                    isFavoriteFocus = it.isFavoriteFocus,
                    frequency = it.frequency.name,
                    customDaysMask = it.customDaysMask,
                    completionWindowStart = it.completionWindowStart,
                    completionWindowEnd = it.completionWindowEnd,
                )
            }
            val json = gson.toJson(entities)
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[HABITS_KEY] = json
            }

            try {
                userRepository.saveHabits(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            streakRepo.invalidateSchedules()
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
        viewModelScope.launch { restoreFromFirestoreNow() }
    }

    /** Pull habitsJson from Firestore and rebuild local + streak Room state. */
    suspend fun restoreFromFirestoreNow() {
        try {
            val data = userRepository.fetchUserData()
            val json = data?.get("habitsJson") as? String
            if (!json.isNullOrEmpty() && json.trim() != "[]") {
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[HABITS_KEY] = json
                }
                applyHabitJson(json)
                streakRepo.migrateLegacyIfNeeded()
                streakRepo.invalidateSchedules()
                streakRepo.refreshDay()
                streakRepo.reconcile()
                refreshHomeScreenWidgets()
            }
        } catch (_: Exception) {
            // Network/rules may fail — keep local cache rather than crash.
        }
    }

    fun onAccountSessionChanged() {
        viewModelScope.launch {
            habitsHydrated = false
            habits.forEach { ReminderScheduler.cancel(getApplication(), it.id) }
            habits.clear()
            loadHabits()
        }
    }

    fun addHabit(habit: HabitItem) {
        habits.add(0, habit)
        if (habit.reminderEnabled && (habit.reminderTime != null || habit.reminderTime2 != null)) {
            ReminderScheduler.schedule(getApplication(), habit)
        }
        saveHabits()
        SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.HABIT_CREATED)
        TrophyBus.emit(TrophyEvent.HabitsBound(habits.toList()))
        TrophyBus.emit(TrophyEvent.HabitCreated)
    }

    fun setReminderTimeForEnabledHabits(time: String) {
        val next = time.trim()
        if (next.isEmpty()) return
        habits.forEachIndexed { index, habit ->
            if (habit.reminderEnabled && !habit.isArchived) {
                val updated = habit.copy(reminderTime = next)
                habits[index] = updated
                ReminderScheduler.schedule(getApplication(), updated)
            }
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
        if (index == -1) return
        val item = habits[index]
        val todayStr = LocalDate.now().format(dateFormatter)

        // Already complete today — no undo. Streak result is the authority, not the stale flag.
        if (item.isCompleted || item.completedDates.contains(todayStr)) return

        val isFirstCompletion = item.completedDates.isEmpty()
        val previousStreak = item.streak
        viewModelScope.launch {
            val result = streakRepo.complete(id)
            val newDates = item.completedDates + todayStr
            val streak = result?.currentStreak ?: (item.streak + 1)
            val idx = habits.indexOfFirst { it.id == id }
            if (idx != -1) {
                habits[idx] = habits[idx].copy(
                    isCompleted = true,
                    completedDates = newDates,
                    streak = streak,
                )
            }
            saveHabits()
            StreakNotifier.rescheduleAll(getApplication())
            SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.HABIT_COMPLETED)
            emitHabitCompleted(
                habit = item,
                streak = streak,
                previousStreak = previousStreak,
                isFirstCompletion = isFirstCompletion,
                hasPhotoProof = false,
            )
            syncGrovePresence()
        }
    }

    /** Soft-end: habit stays on the shelf and can be restored. Never hard-deletes. */
    fun deleteHabit(id: String) = endHabit(id, note = null)

    fun endHabit(id: String, note: String? = null) {
        val index = habits.indexOfFirst { it.id == id }
        if (index == -1) return
        val item = habits[index]
        habits[index] = item.copy(
            isArchived = true,
            isPaused = false,
            endedAt = item.endedAt ?: System.currentTimeMillis(),
            lifeNote = note?.trim()?.ifBlank { null } ?: item.lifeNote,
        )
        ReminderScheduler.cancel(getApplication(), id)
        saveHabits()
        SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.HABIT_ARCHIVED)
    }

    fun archiveHabit(id: String, note: String? = null) {
        val index = habits.indexOfFirst { it.id == id }
        if (index == -1) return
        val item = habits[index]
        habits[index] = item.copy(
            isArchived = true,
            isPaused = false,
            lifeNote = note?.trim()?.ifBlank { null } ?: item.lifeNote,
        )
        ReminderScheduler.cancel(getApplication(), id)
        saveHabits()
        SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.HABIT_ARCHIVED)
    }

    fun restoreHabit(id: String) {
        val index = habits.indexOfFirst { it.id == id }
        if (index == -1) return
        val item = habits[index]
        val restored = item.copy(isArchived = false, isPaused = false, endedAt = null)
        habits[index] = restored
        if (restored.reminderEnabled && (restored.reminderTime != null || restored.reminderTime2 != null)) {
            ReminderScheduler.schedule(getApplication(), restored)
        }
        saveHabits()
    }

    fun pauseHabit(id: String, note: String? = null) {
        val index = habits.indexOfFirst { it.id == id }
        if (index == -1) return
        val item = habits[index]
        if (item.isPaused) {
            resumeHabit(id)
            return
        }
        habits[index] = item.copy(
            isPaused = true,
            lifeNote = note?.trim()?.ifBlank { null } ?: item.lifeNote,
        )
        ReminderScheduler.cancel(getApplication(), id)
        SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.HABIT_PAUSED)
        saveHabits()
    }

    fun resumeHabit(id: String) {
        val index = habits.indexOfFirst { it.id == id }
        if (index == -1) return
        val item = habits[index]
        val next = item.copy(isPaused = false, isArchived = false)
        habits[index] = next
        if (next.reminderEnabled && (next.reminderTime != null || next.reminderTime2 != null)) {
            ReminderScheduler.schedule(getApplication(), next)
        }
        saveHabits()
    }

    fun clearAllHabits() {
        habits.forEach { habit ->
            ReminderScheduler.cancel(getApplication(), habit.id)
        }
        habits.clear()
        saveHabits()
    }

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
        if (index == -1) return
        val item = habits[index]
        val todayStr = LocalDate.now().format(dateFormatter)

        if (item.isCompleted || item.completedDates.contains(todayStr)) return
        val isFirstCompletion = item.completedDates.isEmpty()
        val previousStreak = item.streak
        viewModelScope.launch {
            val result = streakRepo.complete(
                habitId = habit.id,
                proofUrl = photoUrl,
                source = CompletionSource.PROOF,
            )
            val newDates = item.completedDates + todayStr
            val newProofs = item.completionProofs + (todayStr to photoUrl)
            val streak = result?.currentStreak ?: (item.streak + 1)
            val idx = habits.indexOfFirst { it.id == habit.id }
            if (idx != -1) {
                habits[idx] = habits[idx].copy(
                    isCompleted = true,
                    completedDates = newDates,
                    streak = streak,
                    completionProofs = newProofs,
                )
            }
            saveHabits()
            StreakNotifier.rescheduleAll(getApplication())
            SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.HABIT_COMPLETED)
            emitHabitCompleted(
                habit = item,
                streak = streak,
                previousStreak = previousStreak,
                isFirstCompletion = isFirstCompletion,
                hasPhotoProof = true,
            )
            syncGrovePresence()
        }
    }

    private fun emitHabitCompleted(
        habit: HabitItem,
        streak: Int,
        previousStreak: Int,
        isFirstCompletion: Boolean,
        hasPhotoProof: Boolean,
    ) {
        val hasWindow = !habit.completionWindowStart.isNullOrBlank() &&
            !habit.completionWindowEnd.isNullOrBlank()
        val withinWindow = !hasWindow || CompletionWindow.isActiveNow(
            habit.completionWindowStart,
            habit.completionWindowEnd,
        )
        TrophyBus.emit(
            TrophyEvent.HabitCompleted(
                habitId = habit.id,
                streak = streak,
                previousStreak = previousStreak,
                isFirstCompletion = isFirstCompletion,
                difficultyName = habit.difficulty.name,
                withinCommitmentWindow = hasWindow && withinWindow,
                hasPhotoProof = hasPhotoProof,
                reminderEnabled = habit.reminderEnabled,
                reminderTime = habit.reminderTime,
                plantedAt = habit.plantedAt,
                habits = habits.toList(),
            )
        )
    }

    private fun syncGrovePresence() {
        viewModelScope.launch(Dispatchers.IO) {
            val streak = habits.maxOfOrNull { it.streak } ?: 0
            runCatching { userRepository.syncGrovePresence(streak) }
        }
    }

    /** Spends a shield to repair a missed day. */
    fun repairWithShield(habitId: String, missedDate: java.time.LocalDate, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { streakRepo.repairWithShield(habitId, missedDate) }.getOrDefault(false)
            if (ok) runCatching { StreakNotifier.rescheduleAll(getApplication()) }
            onResult(ok)
        }
    }
}
