package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.engine.DuoStreakEngine

object GamificationEngine {

    /**
     * XP awarded for a habit completion.
     * - Base XP comes from the [difficulty] tier (10 / 25 / 50 / 100).
     * - Streak bonus is capped to avoid runaway scoring.
     * - Perfect-day and first-completion bonuses unchanged.
     */
    fun xpForCompletion(
        currentStreak: Int,
        isPerfectDay: Boolean,
        isFirstCompletion: Boolean,
        difficulty: Difficulty = Difficulty.EASY,
        withinCommitmentWindow: Boolean = false,
    ): Int {
        var xp = difficulty.xp
        xp += minOf(currentStreak * 2, 30)
        if (isPerfectDay) xp += 50
        if (isFirstCompletion) xp += 25
        if (withinCommitmentWindow) {
            xp += com.saintnico.verdlyhabits.domain.CompletionWindow.onTimeBonusXp(difficulty)
        }
        return xp
    }

    /** Backwards-compat overload — callers that don't pass difficulty still work. */
    fun xpForCompletion(currentStreak: Int, isPerfectDay: Boolean, isFirstCompletion: Boolean): Int =
        xpForCompletion(currentStreak, isPerfectDay, isFirstCompletion, Difficulty.EASY)

    fun streakBurstXp(streak: Int): Int = when {
        streak == 7 -> 100
        streak == 30 -> 500
        else -> 0
    }

    fun duoMilestoneXp(streakDays: Int): Int = DuoStreakEngine.milestoneXp(streakDays)

    fun calculateLevel(totalXp: Int): Int {
        // XP thresholds grow: 100, 250, 500, 900, 1500, 2300 …
        var level = 0
        var xpLeft = totalXp
        var threshold = 100
        while (xpLeft >= threshold) {
            xpLeft -= threshold
            level++
            threshold = (threshold * 1.4).toInt()
        }
        return level
    }

    fun levelTitle(level: Int): String = when (level) {
        in 0..4   -> "Seed"
        in 5..9   -> "Sprout"
        in 10..14 -> "Sapling"
        in 15..19 -> "Young Tree"
        in 20..29 -> "Grove"
        in 30..44 -> "Forest"
        in 45..59 -> "Ancient Grove"
        in 60..79 -> "Living Legend"
        in 80..99 -> "Eternal Root"
        else      -> "Architect of Self"
    }

    /**
     * Alternative "warrior" track titles (Habitica-flavoured). Available if the
     * UI wants to surface a second progression ladder later — not used today.
     */
    fun warriorTitle(level: Int): String = when (level) {
        in 0..4    -> "Beginner"
        in 5..14   -> "Consistent"
        in 15..29  -> "Warrior"
        in 30..49  -> "Legend"
        else       -> "Myth"
    }

    fun xpToNextLevel(totalXp: Int): Int {
        var xpLeft = totalXp
        var threshold = 100
        while (xpLeft >= threshold) {
            xpLeft -= threshold
            threshold = (threshold * 1.4).toInt()
        }
        return threshold - xpLeft
    }

    fun progressToNextLevel(totalXp: Int): Float {
        var xpLeft = totalXp
        var threshold = 100
        while (xpLeft >= threshold) {
            xpLeft -= threshold
            threshold = (threshold * 1.4).toInt()
        }
        return xpLeft.toFloat() / threshold.toFloat()
    }
}
