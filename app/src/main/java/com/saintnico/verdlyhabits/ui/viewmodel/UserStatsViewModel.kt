package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.AchievementEngine
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.engine.StreakShieldEngine
import com.saintnico.verdlyhabits.engine.TrophyBus
import com.saintnico.verdlyhabits.engine.TrophyEvent
import com.saintnico.verdlyhabits.engine.TrophyFacts
import com.saintnico.verdlyhabits.engine.TrophyFactsEngine
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    val unlockQueue: List<Achievement> = emptyList(),
    val streakShields: Int = 0,
    val xpEarnedToday: Int = 0,
    val isProDebugEnabled: Boolean = false,
    val oathsLocked: Int = 0,
    val oathsConverted: Int = 0,
    val coinsPlanted: Int = 0,
    val coinsGifted: Int = 0,
)

class UserStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppDataStore(application)
    private val gson = Gson()
    private val userRepository = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()
    private val evalMutex = Mutex()
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    @Volatile
    private var latestHabits: List<HabitItem> = emptyList()

    @Volatile
    private var habitsHydrated = false

    /** Blocks cloud uploads until the first Firestore restore attempt finishes. */
    @Volatile
    private var cloudHydrated = false

    private val _state = MutableStateFlow(UserStatsUiState())
    val state: StateFlow<UserStatsUiState> = _state.asStateFlow()

    init {
        TrophyBus.bind { event -> handleEvent(event) }

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
                    progressToNextLevel = GamificationEngine.progressToNextLevel(xp),
                )
            }.collect { base ->
                val achJson = store.achievementsJson.first()
                val achievements = parseAchievements(achJson)
                val shields = store.streakShields.first()
                val prev = _state.value
                val facts = loadFacts()
                _state.value = base.copy(
                    achievements = achievements,
                    streakShields = shields,
                    xpEarnedToday = prev.xpEarnedToday,
                    newlyUnlocked = prev.newlyUnlocked,
                    unlockQueue = prev.unlockQueue,
                    oathsLocked = facts.oathsLocked,
                    oathsConverted = facts.oathsConverted,
                    coinsPlanted = facts.coinsPlanted,
                    coinsGifted = facts.coinsGifted,
                )
            }
        }

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
        // Pull cloud progress as soon as this VM exists (cold start while already signed in).
        viewModelScope.launch {
            restoreFromFirestoreNow()
        }
    }

    override fun onCleared() {
        TrophyBus.unbind()
        super.onCleared()
    }

    fun bindHabits(habits: List<HabitItem>) {
        latestHabits = habits
        if (habitsHydrated) return
        habitsHydrated = true
        viewModelScope.launch {
            evaluateAndPersist(suppressCeremony = true)
        }
    }

    fun onHabitCompleted(
        streak: Int,
        isPerfectDay: Boolean,
        isFirstCompletion: Boolean,
        habits: List<HabitItem>,
        difficulty: Difficulty = Difficulty.EASY,
        withinCommitmentWindow: Boolean = false,
        habitId: String = "",
        previousStreak: Int = (streak - 1).coerceAtLeast(0),
        hasPhotoProof: Boolean = false,
        reminderEnabled: Boolean = false,
        reminderTime: String? = null,
        plantedAt: Long = 0L,
    ) {
        viewModelScope.launch {
            applyHabitCompletion(
                streak = streak,
                isPerfectDay = isPerfectDay,
                isFirstCompletion = isFirstCompletion,
                habits = habits,
                difficulty = difficulty,
                withinCommitmentWindow = withinCommitmentWindow,
                habitId = habitId,
                previousStreak = previousStreak,
                hasPhotoProof = hasPhotoProof,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                plantedAt = plantedAt,
            )
        }
    }

    fun onFocusSessionComplete(minutes: Int, xp: Int) {
        viewModelScope.launch {
            store.addXp(xp)
            store.addFocusMinutes(minutes)
            val facts = loadFacts()
            evaluateAndPersist(
                facts.copy(
                    focusSessions = facts.focusSessions + 1,
                    focusMinutes = facts.focusMinutes + minutes,
                ),
            )
        }
    }

    fun awardDuoMilestoneXp(streakDays: Int) {
        val xp = GamificationEngine.duoMilestoneXp(streakDays)
        viewModelScope.launch {
            if (xp > 0) store.addXp(xp)
            val facts = loadFacts()
            evaluateAndPersist(
                facts.copy(
                    duoStarted = true,
                    duoMaxStreak = maxOf(facts.duoMaxStreak, streakDays),
                ),
            )
        }
    }

    fun consumeUnlock() {
        val rest = _state.value.unlockQueue.drop(1)
        _state.value = _state.value.copy(
            unlockQueue = rest,
            newlyUnlocked = rest.firstOrNull(),
        )
    }

    fun clearNewlyUnlocked() = consumeUnlock()

    fun restoreFromFirestore() {
        viewModelScope.launch { restoreFromFirestoreNow() }
    }

    /**
     * Cloud is source of truth for XP / level / completions / streaks / trophies.
     * Local DataStore is only a cache so the UI stays snappy offline.
     */
    suspend fun restoreFromFirestoreNow() {
        // Drop any ceremony queued while local store was empty (sign-in / account switch race).
        clearUnlockCeremonies()
        var sawCloudDoc = false
        try {
            val data = userRepository.fetchUserData()
            if (data != null) {
                sawCloudDoc = true
                cloudHydrated = true
                fun num(key: String): Int = (data[key] as? Number)?.toInt() ?: 0
                val cloudXp = num("xp")
                val cloudCompletions = num("totalCompletions")
                val cloudStreak = num("longestStreakEver")
                val cloudFocus = num("totalFocusMinutes")
                val localXp = store.totalXp.first()
                val localCompletions = store.totalCompletions.first()
                val localStreak = store.longestStreakEver.first()
                val localFocus = store.totalFocusMinutes.first()
                // Always take the higher of local vs cloud so mid-session offline XP is kept,
                // but a wiped install (0 local) fully restores from Firestore.
                store.restoreStats(
                    xp = maxOf(localXp, cloudXp),
                    completions = maxOf(localCompletions, cloudCompletions),
                    streak = maxOf(localStreak, cloudStreak),
                    focus = maxOf(localFocus, cloudFocus),
                    achievements = data["achievementsJson"] as? String ?: "[]",
                    trophyFacts = data["trophyFactsJson"] as? String ?: "",
                )
                val createdAt = when (val raw = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> raw.toDate().time
                    is Number -> raw.toLong()
                    else -> 0L
                }
                if (createdAt > 0L) {
                    store.restoreMemberSince(createdAt)
                } else {
                    store.setMemberSince(System.currentTimeMillis())
                }
                // Persist any trophies that facts already justify — never play the ceremony again.
                evaluateAndPersist(suppressCeremony = true)
                clearUnlockCeremonies()
            } else {
                store.setMemberSince(System.currentTimeMillis())
            }
        } catch (_: Exception) {
            store.setMemberSince(System.currentTimeMillis())
        } finally {
            // Only allow uploads after we either read the cloud doc, or local already has
            // progress (offline continuing session). Empty local + failed fetch must not
            // upload zeros and wipe Firestore.
            cloudHydrated = sawCloudDoc || store.totalXp.first() > 0 || store.totalCompletions.first() > 0
            clearUnlockCeremonies()
        }
    }

    /** Call when Firebase uid changes so empty-store races cannot re-queue old trophies. */
    fun onAccountSessionChanged() {
        habitsHydrated = false
        cloudHydrated = false
        latestHabits = emptyList()
        clearUnlockCeremonies()
    }

    fun clearUnlockCeremonies() {
        _state.value = _state.value.copy(unlockQueue = emptyList(), newlyUnlocked = null)
    }

    fun hardReset() {
        viewModelScope.launch {
            clearUnlockCeremonies()
            store.clearAllData()
            userRepository.resetUserAccount()
            latestHabits = emptyList()
            habitsHydrated = false
            cloudHydrated = false
            _state.value = UserStatsUiState()
        }
    }

    fun markLanguageChanged() {
        viewModelScope.launch {
            store.markLanguageChanged()
            evaluateAndPersist()
        }
    }

    suspend fun spendShield(): Boolean = store.spendShield()

    fun spendShieldFireAndForget() {
        viewModelScope.launch { store.spendShield() }
    }

    fun setProDebugEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setProDebugEnabled(enabled) }
    }

    private fun handleEvent(event: TrophyEvent) {
        if (event is TrophyEvent.HabitsBound) {
            latestHabits = event.habits
            return
        }
        viewModelScope.launch {
            when (event) {
                is TrophyEvent.HabitsBound -> Unit
                is TrophyEvent.HabitCompleted -> applyHabitCompletion(
                    streak = event.streak,
                    isPerfectDay = false,
                    isFirstCompletion = event.isFirstCompletion,
                    habits = event.habits.ifEmpty { latestHabits },
                    difficulty = Difficulty.fromName(event.difficultyName),
                    withinCommitmentWindow = event.withinCommitmentWindow,
                    habitId = event.habitId,
                    previousStreak = event.previousStreak,
                    hasPhotoProof = event.hasPhotoProof,
                    reminderEnabled = event.reminderEnabled,
                    reminderTime = event.reminderTime,
                    plantedAt = event.plantedAt,
                )
                TrophyEvent.HabitCreated -> evaluateAndPersist()
                TrophyEvent.StatsOpened -> evaluateAndPersist(TrophyFactsEngine.onStatsOpen(loadFacts(), LocalDate.now()))
                TrophyEvent.AppOpened -> {
                    // Never celebrate on cold resume / sign-in while stats are still empty.
                    if (!cloudHydrated) return@launch
                    val before = loadFacts()
                    val after = TrophyFactsEngine.onAppOpen(before, LocalDate.now())
                    if (after != before) evaluateAndPersist(after, suppressCeremony = false)
                }
                TrophyEvent.LanguageChanged -> {
                    store.markLanguageChanged()
                    evaluateAndPersist()
                }
                is TrophyEvent.FocusDone -> {
                    val facts = loadFacts()
                    evaluateAndPersist(
                        facts.copy(
                            focusSessions = facts.focusSessions + 1,
                            focusMinutes = facts.focusMinutes + event.minutes,
                        ),
                    )
                }
                TrophyEvent.GoalCreated -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(goalsCreated = facts.goalsCreated + 1))
                }
                TrophyEvent.GoalCompleted -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(goalsCompleted = facts.goalsCompleted + 1))
                }
                is TrophyEvent.FriendsCount -> {
                    val facts = loadFacts()
                    if (event.count > facts.friendsCount) {
                        evaluateAndPersist(facts.copy(friendsCount = event.count))
                    }
                }
                TrophyEvent.ProfileViewedOther -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(profilesViewed = facts.profilesViewed + 1))
                }
                is TrophyEvent.ProfileViewsReceived -> {
                    val facts = loadFacts()
                    if (event.count > facts.profileViewsReceived) {
                        evaluateAndPersist(facts.copy(profileViewsReceived = event.count))
                    }
                }
                TrophyEvent.Shared -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(shares = facts.shares + 1))
                }
                is TrophyEvent.DuoStreak -> {
                    val facts = loadFacts()
                    evaluateAndPersist(
                        facts.copy(
                            duoStarted = true,
                            duoMaxStreak = maxOf(facts.duoMaxStreak, event.days),
                        ),
                    )
                }
                TrophyEvent.ChallengeJoined -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(challengesJoined = facts.challengesJoined + 1))
                }
                is TrophyEvent.ChallengeWins -> {
                    val facts = loadFacts()
                    if (event.count > facts.challengeWins) {
                        evaluateAndPersist(facts.copy(challengeWins = event.count))
                    }
                }
                TrophyEvent.GardenOpened -> {
                    val facts = loadFacts()
                    if (!facts.gardenOpened) evaluateAndPersist(facts.copy(gardenOpened = true))
                }
                TrophyEvent.HallOpened -> {
                    val facts = loadFacts()
                    if (!facts.hallOpened) evaluateAndPersist(facts.copy(hallOpened = true))
                }
                is TrophyEvent.OathLocked -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(oathsLocked = facts.oathsLocked + 1))
                }
                is TrophyEvent.OathConverted -> {
                    val facts = loadFacts()
                    evaluateAndPersist(
                        facts.copy(
                            oathsConverted = facts.oathsConverted + 1,
                            coinsPlanted = facts.coinsPlanted + event.amount,
                        ),
                    )
                }
                is TrophyEvent.OathForfeited -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(oathsForfeited = facts.oathsForfeited + 1))
                }
                is TrophyEvent.CoinsGifted -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(coinsGifted = facts.coinsGifted + event.amount))
                }
                TrophyEvent.CosmeticOwned -> {
                    val facts = loadFacts()
                    evaluateAndPersist(facts.copy(cosmeticsOwned = facts.cosmeticsOwned + 1))
                }
            }
        }
    }

    private suspend fun evaluateAndPersist(
        updatedFacts: TrophyFacts? = null,
        suppressCeremony: Boolean = false,
    ) {
        evalMutex.withLock {
            // Sign-in / account-switch: local achievements are empty until cloud restore.
            // Treating that as "brand new unlocks" replays the trophy ceremony every login.
            val allowCeremony = cloudHydrated && !suppressCeremony
            val facts = updatedFacts ?: loadFacts()
            if (updatedFacts != null) {
                store.saveTrophyFacts(gson.toJson(facts))
            }
            val current = parseAchievements(store.achievementsJson.first())
            val langChanged = store.languageChanged.first()
            val installMillis = store.memberSince.first()
            val updated = AchievementEngine.evaluate(
                habits = latestHabits,
                totalCompletions = store.totalCompletions.first(),
                currentLevel = GamificationEngine.calculateLevel(store.totalXp.first()),
                languageChanged = langChanged,
                existing = current,
                facts = facts,
                installDateMillis = installMillis,
            )

            val newlyUnlocked = updated.filter { new ->
                new.isUnlocked && current.find { it.id == new.id }?.isUnlocked != true
            }
            val progressChanged = updated.any { new ->
                val old = current.find { it.id == new.id }
                old == null ||
                    old.isUnlocked != new.isUnlocked ||
                    old.progressCurrent != new.progressCurrent
            }
            if (!progressChanged && updatedFacts == null) return@withLock

            var finalAchievements = updated
            if (newlyUnlocked.isNotEmpty() && allowCeremony) {
                val bonusXp = newlyUnlocked.sumOf { it.xpReward }
                if (bonusXp > 0) store.addXp(bonusXp)
                val afterXp = AchievementEngine.evaluate(
                    habits = latestHabits,
                    totalCompletions = store.totalCompletions.first(),
                    currentLevel = GamificationEngine.calculateLevel(store.totalXp.first()),
                    languageChanged = langChanged,
                    existing = updated,
                    facts = facts,
                    installDateMillis = installMillis,
                )
                val extra = afterXp.filter { next ->
                    next.isUnlocked && updated.find { it.id == next.id }?.isUnlocked != true
                }
                if (extra.isNotEmpty()) {
                    val extraXp = extra.sumOf { it.xpReward }
                    if (extraXp > 0) store.addXp(extraXp)
                }
                finalAchievements = afterXp
                store.saveAchievements(gson.toJson(finalAchievements.map { it.toStorable() }))
                val queued = _state.value.unlockQueue + (newlyUnlocked + extra).filter { fresh ->
                    _state.value.unlockQueue.none { it.id == fresh.id }
                }
                _state.value = _state.value.copy(
                    achievements = finalAchievements,
                    unlockQueue = queued,
                    newlyUnlocked = queued.firstOrNull(),
                    oathsLocked = facts.oathsLocked,
                    oathsConverted = facts.oathsConverted,
                    coinsPlanted = facts.coinsPlanted,
                    coinsGifted = facts.coinsGifted,
                )
            } else {
                store.saveAchievements(gson.toJson(finalAchievements.map { it.toStorable() }))
                _state.value = _state.value.copy(
                    achievements = finalAchievements,
                    // Keep ceremony quiet when restoring / not hydrated.
                    unlockQueue = if (allowCeremony) _state.value.unlockQueue else emptyList(),
                    newlyUnlocked = if (allowCeremony) _state.value.newlyUnlocked else null,
                    oathsLocked = facts.oathsLocked,
                    oathsConverted = facts.oathsConverted,
                    coinsPlanted = facts.coinsPlanted,
                    coinsGifted = facts.coinsGifted,
                )
            }
            syncToCloud()
        }
    }

    private suspend fun applyHabitCompletion(
        streak: Int,
        isPerfectDay: Boolean,
        isFirstCompletion: Boolean,
        habits: List<HabitItem>,
        difficulty: Difficulty,
        withinCommitmentWindow: Boolean,
        habitId: String,
        previousStreak: Int,
        hasPhotoProof: Boolean,
        reminderEnabled: Boolean,
        reminderTime: String?,
        plantedAt: Long,
    ) {
        val xp = GamificationEngine.xpForCompletion(
            streak, isPerfectDay, isFirstCompletion, difficulty, withinCommitmentWindow,
        )
        val burst = GamificationEngine.streakBurstXp(streak)
        val total = xp + burst
        store.addXp(total)
        store.incrementCompletions()
        store.updateLongestStreak(streak)
        _state.value = _state.value.copy(xpEarnedToday = _state.value.xpEarnedToday + total)

        val shields = StreakShieldEngine.shieldsEarnedFor(streak)
        if (shields > 0) store.earnShields(shields)

        val today = LocalDate.now()
        val todayIso = today.format(iso)
        val merged = habits.map { habit ->
            if (habit.id == habitId) {
                habit.copy(
                    isCompleted = true,
                    completedDates = habit.completedDates + todayIso,
                    streak = streak,
                )
            } else {
                habit
            }
        }
        latestHabits = merged

        val active = merged.filter { !it.isPaused && !it.isArchived }
        val dueToday = active.filter {
            HabitScheduling.isDueOn(it.frequency, it.customDaysMask, today)
        }
        val perfect = TrophyFactsEngine.isPerfectDayAfterCompleting(dueToday, habitId, todayIso)
        val completedToday = TrophyFactsEngine.completedTodayCount(dueToday, habitId, todayIso)
        val installMillis = store.memberSince.first()
        val facts = TrophyFactsEngine.onHabitComplete(
            facts = loadFacts(),
            today = today,
            time = LocalTime.now(),
            habitId = habitId,
            plantedAt = plantedAt,
            previousStreak = previousStreak,
            newStreak = streak,
            isPerfectDay = perfect || isPerfectDay,
            completedTodayCount = completedToday,
            reminderEnabled = reminderEnabled,
            reminderMinutes = TrophyFactsEngine.parseReminderMinutes(reminderTime),
            hasPhotoProof = hasPhotoProof,
            isInstallDay = TrophyFactsEngine.isInstallCalendarDay(installMillis, today),
        )
        evaluateAndPersist(facts)
    }

    private suspend fun loadFacts(): TrophyFacts {
        return try {
            gson.fromJson(store.trophyFactsJson.first(), TrophyFacts::class.java)?.sanitized()
                ?: TrophyFacts()
        } catch (_: Exception) {
            TrophyFacts()
        }
    }

    private var syncRetrying = false

    private suspend fun syncToCloud() {
        if (!cloudHydrated) {
            if (syncRetrying) return
            syncRetrying = true
            try {
                runCatching { restoreFromFirestoreNow() }
            } finally {
                syncRetrying = false
            }
            if (!cloudHydrated) return
        }
        try {
            userRepository.saveStats(
                xp = store.totalXp.first(),
                completions = store.totalCompletions.first(),
                longestStreak = store.longestStreakEver.first(),
                focusMinutes = store.totalFocusMinutes.first(),
                achievementsJson = store.achievementsJson.first(),
                trophyFactsJson = store.trophyFactsJson.first(),
                memberSinceMillis = store.memberSince.first(),
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseAchievements(json: String): List<Achievement> {
        return try {
            val type = object : TypeToken<List<StorableAchievement>>() {}.type
            val storable: List<StorableAchievement> = gson.fromJson(json, type)
                ?: return AchievementEngine.allAchievements
            AchievementEngine.allAchievements.map { template ->
                val stored = storable.find { it.id == template.id }
                if (stored != null) {
                    template.copy(
                        isUnlocked = stored.isUnlocked,
                        unlockedAt = stored.unlockedAt,
                        progressCurrent = stored.progressCurrent,
                    )
                } else {
                    template
                }
            }
        } catch (_: Exception) {
            AchievementEngine.allAchievements
        }
    }

    private data class StorableAchievement(
        val id: String,
        val isUnlocked: Boolean,
        val unlockedAt: Long?,
        val progressCurrent: Int,
    )

    private fun Achievement.toStorable() = StorableAchievement(id, isUnlocked, unlockedAt, progressCurrent)
}
