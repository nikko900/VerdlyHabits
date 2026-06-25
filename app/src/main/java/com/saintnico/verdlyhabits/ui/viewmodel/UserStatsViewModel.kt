package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.AchievementEngine
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.engine.StreakShieldEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserStatsUiState(
    val totalXp: Int = 0,
    val totalCompletions: Int = 0,
    val longestStreakEver: Int = 0,
    val totalFocusMinutes: Int = 0,
    val memberSince: Long = System.currentTimeMillis(),
    val achievements: List<Achievement> = AchievementEngine.allAchievements,
    val level: Int = 0,
    val levelTitle: String = "Seed",
    val xpToNextLevel: Int = 100,
    val progressToNextLevel: Float = 0f,
    val newlyUnlocked: Achievement? = null,
    val streakShields: Int = 0,
    val xpEarnedToday: Int = 0,
    val isProDebugEnabled: Boolean = false
)

class UserStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppDataStore(application)
    private val gson = Gson()
    private val userRepository = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()

    private val _state = MutableStateFlow(UserStatsUiState())
    val state: StateFlow<UserStatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { store.setMemberSince(System.currentTimeMillis()) }

        viewModelScope.launch {
            combine(
                store.totalXp,
                store.totalCompletions,
                store.longestStreakEver,
                store.totalFocusMinutes,
                store.memberSince,
            ) { xp, completions, streak, focus, since ->
                val level = GamificationEngine.calculateLevel(xp)
                UserStatsUiState(
                    totalXp = xp,
                    totalCompletions = completions,
                    longestStreakEver = streak,
                    totalFocusMinutes = focus,
                    memberSince = since,
                    level = level,
                    levelTitle = GamificationEngine.levelTitle(level),
                    xpToNextLevel = GamificationEngine.xpToNextLevel(xp),
                    progressToNextLevel = GamificationEngine.progressToNextLevel(xp)
                )
            }.collect { base ->
                // Merge achievements + shields from their own flows
                val achJson = store.achievementsJson.first()
                val achievements = parseAchievements(achJson)
                val shields = store.streakShields.first()
                _state.value = base.copy(
                    achievements = achievements,
                    streakShields = shields,
                    xpEarnedToday = _state.value.xpEarnedToday
                )
            }
        }

        // Keep shields and debug mode in sync independently
        viewModelScope.launch {
            store.streakShields.collect { shields ->
                _state.value = _state.value.copy(streakShields = shields)
            }
        }
        viewModelScope.launch {
            store.proDebugEnabled.collect { enabled ->
                _state.value = _state.value.copy(isProDebugEnabled = enabled)
            }
        }
    }

    fun onHabitCompleted(
        streak: Int,
        isPerfectDay: Boolean,
        isFirstCompletion: Boolean,
        habits: List<com.saintnico.verdlyhabits.ui.screens.home.HabitItem>,
        difficulty: Difficulty = Difficulty.EASY
    ) {
        viewModelScope.launch {
            val xp = GamificationEngine.xpForCompletion(streak, isPerfectDay, isFirstCompletion, difficulty)
            val burst = GamificationEngine.streakBurstXp(streak)
            val total = xp + burst
            store.addXp(total)
            store.incrementCompletions()
            store.updateLongestStreak(streak)
            // XP earned today is in-memory only — reset by app process restart, which is fine.
            _state.value = _state.value.copy(xpEarnedToday = _state.value.xpEarnedToday + total)

            // Award shields for hitting weekly milestones
            val shields = StreakShieldEngine.shieldsEarnedFor(streak)
            if (shields > 0) store.earnShields(shields)

            // Re-evaluate achievements
            val current = parseAchievements(store.achievementsJson.first())
            val langChanged = store.languageChanged.first()
            val updated = AchievementEngine.evaluate(
                habits = habits,
                totalCompletions = store.totalCompletions.first(),
                currentLevel = GamificationEngine.calculateLevel(store.totalXp.first()),
                languageChanged = langChanged,
                existing = current
            )
            store.saveAchievements(gson.toJson(updated.map { it.toStorable() }))

            val newlyUnlocked = updated.firstOrNull { new ->
                new.isUnlocked && current.find { it.id == new.id }?.isUnlocked == false
            }
            if (newlyUnlocked != null) {
                _state.value = _state.value.copy(newlyUnlocked = newlyUnlocked)
            }
            syncToCloud()
        }
    }

    fun onFocusSessionComplete(minutes: Int, xp: Int) {
        viewModelScope.launch {
            store.addXp(xp)
            store.addFocusMinutes(minutes)
            syncToCloud()
        }
    }

    private suspend fun syncToCloud() {
        try {
            userRepository.saveStats(
                xp = store.totalXp.first(),
                completions = store.totalCompletions.first(),
                longestStreak = store.longestStreakEver.first(),
                focusMinutes = store.totalFocusMinutes.first(),
                achievementsJson = store.achievementsJson.first()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreFromFirestore() {
        viewModelScope.launch {
            try {
                val data = userRepository.fetchUserData()
                if (data != null) {
                    fun num(key: String): Int = (data[key] as? Number)?.toInt() ?: 0
                    store.restoreStats(
                        xp = num("xp"),
                        completions = num("totalCompletions"),
                        streak = num("longestStreakEver"),
                        focus = num("totalFocusMinutes"),
                        achievements = data["achievementsJson"] as? String ?: "[]",
                    )
                }
            } catch (_: Exception) {
                // Keep local stats if cloud pull fails.
            }
        }
    }

    fun hardReset() {
        viewModelScope.launch {
            store.clearAllData()
            userRepository.resetUserAccount()
            _state.value = UserStatsUiState() // Reset local state
        }
    }

    fun markLanguageChanged() {
        viewModelScope.launch { store.markLanguageChanged() }
    }

    fun clearNewlyUnlocked() {
        _state.value = _state.value.copy(newlyUnlocked = null)
    }

    /** Returns true if a shield was actually spent. */
    suspend fun spendShield(): Boolean = store.spendShield()

    fun spendShieldFireAndForget() {
        viewModelScope.launch { store.spendShield() }
    }

    fun setProDebugEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setProDebugEnabled(enabled) }
    }

    private fun parseAchievements(json: String): List<Achievement> {
        return try {
            val type = object : TypeToken<List<StorableAchievement>>() {}.type
            val storable: List<StorableAchievement> = gson.fromJson(json, type) ?: return AchievementEngine.allAchievements
            AchievementEngine.allAchievements.map { template ->
                val stored = storable.find { it.id == template.id }
                if (stored != null) {
                    template.copy(
                        isUnlocked = stored.isUnlocked,
                        unlockedAt = stored.unlockedAt,
                        progressCurrent = stored.progressCurrent
                    )
                } else template
            }
        } catch (_: Exception) {
            AchievementEngine.allAchievements
        }
    }

    private data class StorableAchievement(
        val id: String,
        val isUnlocked: Boolean,
        val unlockedAt: Long?,
        val progressCurrent: Int
    )

    private fun Achievement.toStorable() = StorableAchievement(id, isUnlocked, unlockedAt, progressCurrent)
}
