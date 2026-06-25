package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Evaluates which trophies should be unlocked based on current user state.
 *
 * This replaces the old [AchievementEngine.evaluate] with a much richer
 * evaluation against the 50-trophy catalog. The old [Achievement] data class
 * is still used for storage compat — we map [TrophyDef] → [Achievement] at
 * the boundary.
 */
object TrophyEvaluator {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Main evaluation entry point. Runs all 50 trophy checks against the
     * current state and returns the updated achievement list.
     *
     * @param habits          All user habits (active, paused, archived)
     * @param totalCompletions Lifetime count
     * @param currentLevel    Gamification level
     * @param languageChanged Whether the user changed the app language
     * @param existing        Previously persisted achievement states
     * @param installDateMillis When the app was first installed
     * @param statsViewCount  How many times the stats screen was opened
     * @param earlyMorningCount Completions before 6 AM (lifetime)
     * @param lateNightCount  Completions after 10 PM (lifetime)
     * @param veryEarlyCount  Completions before 5 AM (lifetime)
     * @param recoveryCount   Times user resumed a streak within 24h
     * @param consecutiveActiveDays Days the user has opened the app in a row
     * @param perfectDayCount Days where ALL habits were completed
     * @param challengesWon   Group challenges won
     * @param monthsWithActivity Set of months (1-12) where user has completions
     * @param categoriesUsed  Number of distinct habit categories
     * @param surpassedStreaks Times user recovered and surpassed a broken streak
     */
    fun evaluate(
        habits: List<HabitItem>,
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
    ): List<Achievement> {
        val existingMap = existing.associateBy { it.id }.toMutableMap()
        val now = System.currentTimeMillis()
        val active = habits.filter { !it.isPaused && !it.isArchived }
        val maxStreak = active.maxOfOrNull { it.streak } ?: 0
        val daysSinceInstall = if (installDateMillis > 0)
            ((now - installDateMillis) / (1000L * 60 * 60 * 24)).toInt() else 0

        // Streak counts for compound checks
        val habitsWithStreak7 = active.count { it.streak >= 7 }
        val habitsWithStreak14 = active.count { it.streak >= 14 }
        val habitsWithStreak30 = active.count { it.streak >= 30 }
        val habitsWithStreak50 = active.count { it.streak >= 50 }
        val today = LocalDate.now().format(fmt)
        val completedToday = active.count { it.completedDates.contains(today) }

        fun progress(id: String, current: Int) {
            val template = TrophyCatalog.find(id) ?: return
            val a = existingMap[id] ?: Achievement(
                id = template.id, title = template.title, description = template.description,
                icon = template.icon, xpReward = template.xpReward,
                progressTarget = template.progressTarget
            )
            if (a.isUnlocked) return // Don't regress
            val clamped = minOf(current, template.progressTarget)
            val unlocked = clamped >= template.progressTarget
            existingMap[id] = a.copy(
                progressCurrent = clamped,
                isUnlocked = unlocked,
                unlockedAt = if (unlocked && a.unlockedAt == null) now else a.unlockedAt
            )
        }

        fun unlock(id: String) {
            val template = TrophyCatalog.find(id) ?: return
            val a = existingMap[id] ?: Achievement(
                id = template.id, title = template.title, description = template.description,
                icon = template.icon, xpReward = template.xpReward,
                progressTarget = template.progressTarget
            )
            if (!a.isUnlocked) {
                existingMap[id] = a.copy(
                    isUnlocked = true, unlockedAt = now,
                    progressCurrent = template.progressTarget
                )
            }
        }

        // ── TIER 1 — FOUNDATION ────────────────────────────────────────
        if (habits.isNotEmpty()) unlock("first_light")
        if (active.isNotEmpty() && completedToday >= active.size) unlock("the_spark")
        progress("threes_company", minOf(maxStreak, 3))
        progress("week_one", minOf(maxStreak, 7))
        progress("early_bird", minOf(earlyMorningCount, 5))
        progress("full_house", perfectDayCount.coerceAtMost(3))
        progress("committed", habits.size.coerceAtMost(3))
        progress("the_ritual", minOf(maxStreak, 7))
        // no_excuses: tracked externally when user opens after ignored reminder
        progress("quiet_achiever", completedToday.coerceAtMost(5))

        // ── TIER 2 — MOMENTUM ──────────────────────────────────────────
        progress("iron_will", minOf(maxStreak, 14))
        progress("the_compound", habitsWithStreak7.coerceAtMost(3))
        progress("midnight_oil", lateNightCount.coerceAtMost(7))
        if (perfectDayCount >= 7) unlock("perfectionist") else progress("perfectionist", 0)
        progress("the_architect", active.size.coerceAtMost(5))
        progress("chain_reaction", habitsWithStreak14.coerceAtMost(2))
        progress("rise_and_grind", earlyMorningCount.coerceAtMost(10))
        progress("recovery", recoveryCount.coerceAtMost(3))
        progress("the_long_game", consecutiveActiveDays.coerceAtMost(30))
        progress("data_driven", statsViewCount.coerceAtMost(20))

        // ── TIER 3 — MASTERY ───────────────────────────────────────────
        progress("centurion", minOf(maxStreak, 100))
        progress("ghost_protocol", if (perfectDayCount >= 30) 30 else perfectDayCount.coerceAtMost(29))
        // the_philosopher: needs mindfulness category check
        val mindfulHabit = active.find {
            it.category.name.contains("MIND", ignoreCase = true) ||
            it.category.name.contains("MEDITAT", ignoreCase = true)
        }
        progress("the_philosopher", (mindfulHabit?.streak ?: 0).coerceAtMost(60))
        progress("unseen_hours", earlyMorningCount.coerceAtMost(50))
        progress("dual_threat", habitsWithStreak50.coerceAtMost(2))
        progress("the_collector", habits.count { !it.isArchived }.coerceAtMost(8))
        progress("ironclad", if (active.all { it.streak >= 60 } && active.isNotEmpty()) 60 else 0)
        progress("the_observer", statsViewCount.coerceAtMost(21)) // consecutive check is simplified
        progress("velocity", 0) // complex — tracked externally
        progress("legacy", daysSinceInstall.coerceAtMost(365))
        progress("five_hundred", totalCompletions.coerceAtMost(500))
        if (languageChanged) unlock("polyglot")

        // ── TIER 4 — LEGEND ────────────────────────────────────────────
        progress("the_constant", minOf(maxStreak, 365))
        val tier1And2Count = TrophyCatalog.idsForTier(TrophyTierLevel.FOUNDATION)
            .plus(TrophyCatalog.idsForTier(TrophyTierLevel.MOMENTUM))
            .count { existingMap[it]?.isUnlocked == true }
        progress("pantheon", tier1And2Count.coerceAtMost(20))
        progress("architects_master", if (active.size >= 10 && active.all { it.streak >= 30 }) 10 else active.size.coerceAtMost(9))
        progress("eclipse", monthsWithActivity.size.coerceAtMost(12))
        progress("obsidian_chain", totalCompletions.coerceAtMost(1000))
        progress("first_challenge_w", challengesWon.coerceAtMost(1))
        progress("night_shift", lateNightCount.coerceAtMost(100))
        progress("deep_roots", currentLevel.coerceAtMost(30))
        progress("garden_master", if (habitsWithStreak30 >= 7) 7 else habitsWithStreak30.coerceAtMost(6))
        progress("royalty", currentLevel.coerceAtMost(20))
        progress("ancient_one", currentLevel.coerceAtMost(60))
        progress("full_spectrum", categoriesUsed.coerceAtMost(5))

        // ── TIER 5 — MYTHIC (secret) ───────────────────────────────────
        progress("the_long_shadow", minOf(maxStreak, 500))
        progress("infinite", totalCompletions.coerceAtMost(2000))
        progress("unseen_war", veryEarlyCount.coerceAtMost(100))
        progress("still_standing", surpassedStreaks.coerceAtMost(10))
        // the_origin: tracked externally when first habit is completed on anniversary
        if (active.isNotEmpty() && completedToday >= active.size) unlock("launch_day")

        // Build final list: map every catalog entry to its evaluated state
        return TrophyCatalog.all.map { def ->
            existingMap[def.id] ?: Achievement(
                id = def.id, title = def.title, description = def.description,
                icon = def.icon, xpReward = def.xpReward,
                progressTarget = def.progressTarget
            )
        }
    }

    /**
     * Full Hall of Trophies — every catalog entry, earned or locked with progress.
     * Nothing is hidden behind "???" or tier gates; higher ranks stay aspirational.
     */
    fun hallTrophies(allEvaluated: List<Achievement>): List<VisibleTrophy> {
        val byId = allEvaluated.associateBy { it.id }
        return TrophyCatalog.all.map { def ->
            val a = byId[def.id] ?: Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                icon = def.icon,
                xpReward = def.xpReward,
                progressTarget = def.progressTarget
            )
            VisibleTrophy(
                achievement = a,
                tier = def.tier,
                state = if (a.isUnlocked) VisibleState.EARNED else VisibleState.LOCKED
            )
        }.sortedWith(
            compareBy<VisibleTrophy> { it.tier.ordinalIndex }
                .thenByDescending { it.state == VisibleState.EARNED }
                .thenBy { !it.achievement.isUnlocked }
                .thenBy { it.achievement.progressTarget }
                .thenBy { it.achievement.title }
        )
    }

    /** @deprecated Use [hallTrophies]; kept for any stale call sites. */
    fun visibleTrophies(allEvaluated: List<Achievement>): List<VisibleTrophy> =
        hallTrophies(allEvaluated)

    data class VisibleTrophy(
        val achievement: Achievement,
        val tier: TrophyTierLevel,
        val state: VisibleState
    )

    enum class VisibleState { EARNED, LOCKED }
}
