package com.saintnico.verdlyhabits.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Achievement data class — the serializable runtime state of one trophy.
 *
 * This is NOT the definition catalog (see [TrophyCatalog]). It's the user's
 * progress state for a specific trophy. Kept as a data class for Gson compat
 * with the existing DataStore persistence layer.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1
)

/**
 * Legacy bridge — delegates to [TrophyEvaluator] while keeping the existing
 * call sites in [UserStatsViewModel] working. The old 16-item list is replaced
 * by the 50-trophy catalog, but the API shape is identical.
 */
object AchievementEngine {

    /** All trophies with default (unevaluated) state. */
    val allAchievements: List<Achievement> by lazy {
        TrophyCatalog.all.map { def ->
            Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                icon = def.icon,
                xpReward = def.xpReward,
                progressTarget = def.progressTarget
            )
        }
    }

    /**
     * Evaluate trophies against current state. Backwards-compatible wrapper
     * around [TrophyEvaluator.evaluate].
     */
    fun evaluate(
        habits: List<com.saintnico.verdlyhabits.ui.screens.home.HabitItem>,
        totalCompletions: Int,
        currentLevel: Int,
        languageChanged: Boolean,
        existing: List<Achievement>,
        installDateMillis: Long = 0L,
        statsViewCount: Int = 0,
        earlyMorningCount: Int = 0,
        lateNightCount: Int = 0,
        veryEarlyCount: Int = 0,
        recoveryCount: Int = 0,
        consecutiveActiveDays: Int = 0,
        perfectDayCount: Int = 0,
        challengesWon: Int = 0,
        monthsWithActivity: Set<Int> = emptySet(),
        categoriesUsed: Int = 0,
        surpassedStreaks: Int = 0
    ): List<Achievement> = TrophyEvaluator.evaluate(
        habits = habits,
        totalCompletions = totalCompletions,
        currentLevel = currentLevel,
        languageChanged = languageChanged,
        existing = existing,
        installDateMillis = installDateMillis,
        statsViewCount = statsViewCount,
        earlyMorningCount = earlyMorningCount,
        lateNightCount = lateNightCount,
        veryEarlyCount = veryEarlyCount,
        recoveryCount = recoveryCount,
        consecutiveActiveDays = consecutiveActiveDays,
        perfectDayCount = perfectDayCount,
        challengesWon = challengesWon,
        monthsWithActivity = monthsWithActivity,
        categoriesUsed = categoriesUsed,
        surpassedStreaks = surpassedStreaks
    )
}
