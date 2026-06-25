package com.saintnico.verdlyhabits.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "verdlyhabits_data")

/**
 * DataStore-backed persistence for all new feature data (mood, focus, goals, user stats).
 * Uses the same Gson serialization pattern as the existing HabitViewModel.
 */
class AppDataStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        val MOODS_JSON = stringPreferencesKey("moods_json")
        val FOCUS_SESSIONS_JSON = stringPreferencesKey("focus_sessions_json")
        val GOALS_JSON = stringPreferencesKey("goals_json")
        val TOTAL_XP = intPreferencesKey("total_xp")
        val TOTAL_COMPLETIONS = intPreferencesKey("total_completions")
        val LONGEST_STREAK_EVER = intPreferencesKey("longest_streak_ever")
        val TOTAL_FOCUS_MINUTES = intPreferencesKey("total_focus_minutes")
        val MEMBER_SINCE = longPreferencesKey("member_since")
        val LANGUAGE_CHANGED = booleanPreferencesKey("language_changed")
        val ACHIEVEMENTS_JSON = stringPreferencesKey("achievements_json")

        // ── New (Phase 1 overhaul) ──────────────────────────────────────────
        val STREAK_SHIELDS = intPreferencesKey("streak_shields")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PINNED_FOCUS_HABIT_ID = stringPreferencesKey("pinned_focus_habit_id")
        val FLOW_STREAK = intPreferencesKey("flow_streak") // consecutive focus sessions today
        val FLOW_STREAK_DAY = stringPreferencesKey("flow_streak_day") // ISO date the counter belongs to
        val AMBIENT_LAST_USED = stringPreferencesKey("ambient_last_used")
        val TRIAL_PILL_DISMISSED_DAY = stringPreferencesKey("trial_pill_dismissed_iso_day")
        val PRO_DEBUG_ENABLED = booleanPreferencesKey("pro_debug_enabled")
    }

    // ── Mood ──────────────────────────────────────────────────────────────
    data class MoodEntry(
        val id: String,
        val timestamp: Long,
        val mood: Int,
        val tags: List<String>,
        val note: String?
    )

    val moods: Flow<List<MoodEntry>> = context.appDataStore.data.map { prefs ->
        val json = prefs[MOODS_JSON] ?: "[]"
        try { gson.fromJson(json, object : TypeToken<List<MoodEntry>>() {}.type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }

    suspend fun saveMood(entry: MoodEntry) {
        context.appDataStore.edit { prefs ->
            val current: MutableList<MoodEntry> = try {
                gson.fromJson(prefs[MOODS_JSON] ?: "[]", object : TypeToken<MutableList<MoodEntry>>() {}.type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }
            current.add(0, entry)
            prefs[MOODS_JSON] = gson.toJson(current)
        }
    }

    // ── Focus Sessions ────────────────────────────────────────────────────
    data class FocusSession(
        val id: String,
        val habitId: String?,
        val startedAt: Long,
        val durationMinutes: Int,
        val completed: Boolean,
        val xpEarned: Int
    )

    val focusSessions: Flow<List<FocusSession>> = context.appDataStore.data.map { prefs ->
        val json = prefs[FOCUS_SESSIONS_JSON] ?: "[]"
        try { gson.fromJson(json, object : TypeToken<List<FocusSession>>() {}.type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }

    suspend fun saveFocusSession(session: FocusSession) {
        context.appDataStore.edit { prefs ->
            val current: MutableList<FocusSession> = try {
                gson.fromJson(prefs[FOCUS_SESSIONS_JSON] ?: "[]", object : TypeToken<MutableList<FocusSession>>() {}.type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }
            current.add(0, session)
            prefs[FOCUS_SESSIONS_JSON] = gson.toJson(current)
        }
    }

    // ── User Stats ────────────────────────────────────────────────────────
    val totalXp: Flow<Int> = context.appDataStore.data.map { it[TOTAL_XP] ?: 0 }
    val totalCompletions: Flow<Int> = context.appDataStore.data.map { it[TOTAL_COMPLETIONS] ?: 0 }
    val longestStreakEver: Flow<Int> = context.appDataStore.data.map { it[LONGEST_STREAK_EVER] ?: 0 }
    val totalFocusMinutes: Flow<Int> = context.appDataStore.data.map { it[TOTAL_FOCUS_MINUTES] ?: 0 }
    val memberSince: Flow<Long> = context.appDataStore.data.map { it[MEMBER_SINCE] ?: System.currentTimeMillis() }
    val languageChanged: Flow<Boolean> = context.appDataStore.data.map { it[LANGUAGE_CHANGED] ?: false }
    val achievementsJson: Flow<String> = context.appDataStore.data.map { it[ACHIEVEMENTS_JSON] ?: "[]" }

    suspend fun addXp(xp: Int) {
        context.appDataStore.edit { prefs ->
            prefs[TOTAL_XP] = (prefs[TOTAL_XP] ?: 0) + xp
        }
    }

    suspend fun incrementCompletions() {
        context.appDataStore.edit { prefs ->
            prefs[TOTAL_COMPLETIONS] = (prefs[TOTAL_COMPLETIONS] ?: 0) + 1
        }
    }

    suspend fun updateLongestStreak(streak: Int) {
        context.appDataStore.edit { prefs ->
            val current = prefs[LONGEST_STREAK_EVER] ?: 0
            if (streak > current) prefs[LONGEST_STREAK_EVER] = streak
        }
    }

    suspend fun addFocusMinutes(minutes: Int) {
        context.appDataStore.edit { prefs ->
            prefs[TOTAL_FOCUS_MINUTES] = (prefs[TOTAL_FOCUS_MINUTES] ?: 0) + minutes
        }
    }

    suspend fun setMemberSince(time: Long) {
        context.appDataStore.edit { prefs ->
            if (prefs[MEMBER_SINCE] == null) prefs[MEMBER_SINCE] = time
        }
    }

    suspend fun markLanguageChanged() {
        context.appDataStore.edit { prefs -> prefs[LANGUAGE_CHANGED] = true }
    }

    suspend fun saveAchievements(json: String) {
        context.appDataStore.edit { prefs -> prefs[ACHIEVEMENTS_JSON] = json }
    }

    // ── Streak shields ────────────────────────────────────────────────────
    val streakShields: Flow<Int> = context.appDataStore.data.map { it[STREAK_SHIELDS] ?: 0 }

    suspend fun earnShields(count: Int) {
        if (count <= 0) return
        context.appDataStore.edit { prefs ->
            val current = prefs[STREAK_SHIELDS] ?: 0
            prefs[STREAK_SHIELDS] = (current + count).coerceAtMost(
                com.saintnico.verdlyhabits.engine.StreakShieldEngine.MAX_SHIELDS
            )
        }
    }

    suspend fun spendShield(): Boolean {
        var spent = false
        context.appDataStore.edit { prefs ->
            val current = prefs[STREAK_SHIELDS] ?: 0
            if (current > 0) {
                prefs[STREAK_SHIELDS] = current - 1
                spent = true
            }
        }
        return spent
    }

    // ── Sound / haptics / premium prefs ───────────────────────────────────
    val soundEnabled: Flow<Boolean> = context.appDataStore.data.map { it[SOUND_ENABLED] ?: true }
    val hapticsEnabled: Flow<Boolean> = context.appDataStore.data.map { it[HAPTICS_ENABLED] ?: true }
    val isPremium: Flow<Boolean> = context.appDataStore.data.map { it[IS_PREMIUM] ?: false }
    val pinnedFocusHabitId: Flow<String?> = context.appDataStore.data.map { it[PINNED_FOCUS_HABIT_ID] }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }

    suspend fun setPremium(enabled: Boolean) {
        context.appDataStore.edit { it[IS_PREMIUM] = enabled }
    }

    val proDebugEnabled: Flow<Boolean> = context.appDataStore.data.map { it[PRO_DEBUG_ENABLED] ?: false }

    suspend fun setProDebugEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[PRO_DEBUG_ENABLED] = enabled }
    }

    suspend fun setPinnedFocusHabit(id: String?) {
        context.appDataStore.edit { prefs ->
            if (id == null) prefs.remove(PINNED_FOCUS_HABIT_ID) else prefs[PINNED_FOCUS_HABIT_ID] = id
        }
    }

    // ── Flow streak (consecutive focus sessions today) ────────────────────
    val flowStreak: Flow<Int> = context.appDataStore.data.map { prefs ->
        val day = prefs[FLOW_STREAK_DAY] ?: ""
        val todayStr = java.time.LocalDate.now().toString()
        if (day == todayStr) prefs[FLOW_STREAK] ?: 0 else 0
    }

    suspend fun incrementFlowStreak() {
        context.appDataStore.edit { prefs ->
            val todayStr = java.time.LocalDate.now().toString()
            val day = prefs[FLOW_STREAK_DAY] ?: ""
            val current = if (day == todayStr) prefs[FLOW_STREAK] ?: 0 else 0
            prefs[FLOW_STREAK] = current + 1
            prefs[FLOW_STREAK_DAY] = todayStr
        }
    }

    suspend fun resetFlowStreak() {
        context.appDataStore.edit { prefs ->
            prefs.remove(FLOW_STREAK)
            prefs.remove(FLOW_STREAK_DAY)
        }
    }

    val ambientLastUsed: Flow<String?> = context.appDataStore.data.map { it[AMBIENT_LAST_USED] }
    suspend fun setAmbientLastUsed(name: String?) {
        context.appDataStore.edit { prefs ->
            if (name == null) prefs.remove(AMBIENT_LAST_USED) else prefs[AMBIENT_LAST_USED] = name
        }
    }

    val trialPillDismissedDay: Flow<String?> =
        context.appDataStore.data.map { it[TRIAL_PILL_DISMISSED_DAY] }

    suspend fun dismissTrialPillForToday() {
        val day = java.time.LocalDate.now().toString()
        context.appDataStore.edit { it[TRIAL_PILL_DISMISSED_DAY] = day }
    }

    suspend fun clearAllData() {
        context.appDataStore.edit { it.clear() }
    }

    suspend fun restoreStats(
        xp: Int, 
        completions: Int, 
        streak: Int, 
        focus: Int, 
        achievements: String
    ) {
        context.appDataStore.edit { prefs ->
            prefs[TOTAL_XP] = xp
            prefs[TOTAL_COMPLETIONS] = completions
            prefs[LONGEST_STREAK_EVER] = streak
            prefs[TOTAL_FOCUS_MINUTES] = focus
            prefs[ACHIEVEMENTS_JSON] = achievements
        }
    }
}
