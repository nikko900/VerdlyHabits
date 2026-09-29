package com.saintnico.verdlyhabits.engine

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
    val progressTarget: Int = 1,
)

/**
 * Legacy bridge — delegates to [TrophyEvaluator] while keeping existing call sites working.
 */
object AchievementEngine {

    val allAchievements: List<Achievement> by lazy {
        TrophyCatalog.all.map { def ->
            Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                icon = def.icon,
                xpReward = def.xpReward,
                progressTarget = def.progressTarget,
            )
        }
    }

    fun evaluate(
        habits: List<com.saintnico.verdlyhabits.ui.screens.home.HabitItem>,
        totalCompletions: Int,
        currentLevel: Int,
        languageChanged: Boolean,
        existing: List<Achievement>,
        facts: TrophyFacts = TrophyFacts(),
        installDateMillis: Long = 0L,
    ): List<Achievement> = TrophyEvaluator.evaluate(
        habits = habits,
        totalCompletions = totalCompletions,
        currentLevel = currentLevel,
        languageChanged = languageChanged,
        existing = existing,
        facts = facts,
        installDateMillis = installDateMillis,
    )
}
