package com.saintnico.verdlyhabits.engine

/**
 * Streak shield mechanic — Habitica/Duolingo style.
 *
 *  - Earn 1 shield each time *any* habit reaches a multiple of 7 days
 *    (7, 14, 21, 28, …).
 *  - A shield can be spent to "protect" a streak when the user misses a day.
 *  - Shields are scoped to the whole account (not per-habit), so any earned
 *    shield can save any habit.
 *
 * The engine is pure logic; persistence lives in [com.saintnico.verdlyhabits.data.local.AppDataStore].
 */
object StreakShieldEngine {

    const val SHIELD_INTERVAL_DAYS: Int = 7
    const val MAX_SHIELDS: Int = 5

    /**
     * How many shields a habit would award when its streak just *became* [newStreak].
     * Returns 1 every time the streak crosses a multiple of [SHIELD_INTERVAL_DAYS],
     * otherwise 0.
     */
    fun shieldsEarnedFor(newStreak: Int): Int {
        if (newStreak <= 0) return 0
        return if (newStreak % SHIELD_INTERVAL_DAYS == 0) 1 else 0
    }

    /** Clamps to [MAX_SHIELDS]. */
    fun addShields(current: Int, earned: Int): Int =
        (current + earned).coerceAtMost(MAX_SHIELDS)

    fun canSpend(current: Int): Boolean = current > 0

    fun spend(current: Int): Int = (current - 1).coerceAtLeast(0)
}
