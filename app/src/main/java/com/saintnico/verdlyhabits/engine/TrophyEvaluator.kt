package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Evaluates which trophies should be unlocked based on current user state.
 *
 * Maps [TrophyDef] → [Achievement] at the persistence boundary.
 */
object TrophyEvaluator {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun evaluate(
        habits: List<HabitItem>,
        totalCompletions: Int,
        currentLevel: Int,
        languageChanged: Boolean,
        existing: List<Achievement>,
        facts: TrophyFacts = TrophyFacts(),
        installDateMillis: Long = 0L,
    ): List<Achievement> {
        val existingMap = existing.associateBy { it.id }.toMutableMap()
        val now = System.currentTimeMillis()
        val active = habits.filter { !it.isPaused && !it.isArchived }
        val maxStreak = active.maxOfOrNull { it.streak } ?: 0
        val daysSinceInstall = if (installDateMillis > 0) {
            ((now - installDateMillis) / (1000L * 60 * 60 * 24)).toInt()
        } else {
            0
        }

        val habitsWithStreak7 = active.count { it.streak >= 7 }
        val habitsWithStreak14 = active.count { it.streak >= 14 }
        val habitsWithStreak30 = active.count { it.streak >= 30 }
        val habitsWithStreak50 = active.count { it.streak >= 50 }
        val today = LocalDate.now().format(fmt)
        val completedToday = active.count { it.completedDates.contains(today) || it.isCompleted }
        val categoriesUsed = active.map { it.category }.distinct().size
        val pantheonIds = TrophyCatalog.idsForTier(TrophyTierLevel.FOUNDATION) +
            TrophyCatalog.idsForTier(TrophyTierLevel.MOMENTUM)
        val pantheonTarget = pantheonIds.size

        fun progress(id: String, current: Int, targetOverride: Int? = null) {
            val template = TrophyCatalog.find(id) ?: return
            val target = targetOverride ?: template.progressTarget
            val a = existingMap[id] ?: Achievement(
                id = template.id,
                title = template.title,
                description = template.description,
                icon = template.icon,
                xpReward = template.xpReward,
                progressTarget = target,
            )
            if (a.isUnlocked) return
            val clamped = minOf(current, target)
            val unlocked = clamped >= target
            existingMap[id] = a.copy(
                progressCurrent = clamped,
                progressTarget = target,
                isUnlocked = unlocked,
                unlockedAt = if (unlocked && a.unlockedAt == null) now else a.unlockedAt,
            )
        }

        fun unlock(id: String) {
            val template = TrophyCatalog.find(id) ?: return
            val a = existingMap[id] ?: Achievement(
                id = template.id,
                title = template.title,
                description = template.description,
                icon = template.icon,
                xpReward = template.xpReward,
                progressTarget = template.progressTarget,
            )
            if (!a.isUnlocked) {
                existingMap[id] = a.copy(
                    isUnlocked = true,
                    unlockedAt = now,
                    progressCurrent = template.progressTarget,
                )
            }
        }

        // ── TIER 1 — FOUNDATION ────────────────────────────────────────
        if (totalCompletions >= 1 || habits.any { it.completedDates.isNotEmpty() }) unlock("first_light")
        if (active.isNotEmpty() && completedToday >= active.size) unlock("the_spark")
        progress("threes_company", minOf(maxStreak, 3))
        progress("week_one", minOf(maxStreak, 7))
        progress("early_bird", facts.consecutiveEarlyBirdDays)
        progress("full_house", facts.perfectDayCount)
        progress("committed", habits.size)
        progress("the_ritual", minOf(maxStreak, 7))
        if (facts.noExcuses) unlock("no_excuses")
        progress("quiet_achiever", completedToday)
        if (facts.statsVisitCount >= 1) unlock("first_stats")
        if (facts.goalsCreated >= 1) unlock("first_goal")
        if (facts.focusSessions >= 1) unlock("first_focus")
        if (facts.friendsCount >= 1) unlock("first_friend")
        if (facts.photoProofs >= 1) unlock("first_proof")
        if (facts.twoHabitDays >= 1) unlock("two_today")
        if (facts.profilesViewed >= 1) unlock("first_profile_visit")
        if (facts.profileViewsReceived >= 1) unlock("first_visitor")

        // ── TIER 2 — MOMENTUM ──────────────────────────────────────────
        progress("iron_will", minOf(maxStreak, 14))
        progress("the_compound", habitsWithStreak7)
        progress("midnight_oil", facts.afterElevenCount)
        progress("perfectionist", facts.consecutivePerfectDays, targetOverride = 7)
        progress("the_architect", active.size)
        progress("chain_reaction", TrophyFactsEngine.chainReactionProgress(facts).coerceAtLeast(habitsWithStreak14.coerceAtMost(1)))
        progress("rise_and_grind", facts.earlyMorningCount)
        progress("recovery", facts.recoveryCount)
        progress("the_long_game", facts.consecutiveOpenDays)
        progress("data_driven", facts.statsVisitCount)
        progress("stats_habit", facts.consecutiveStatsDays)
        if (facts.duoStarted || facts.duoMaxStreak >= 1) unlock("first_duo")
        if (facts.challengesJoined >= 1) unlock("join_arena")
        progress("hour_in_garden", facts.focusMinutes)
        progress("photo_five", facts.photoProofs)
        if (facts.shares >= 1) unlock("first_share")
        if (facts.oathsLocked >= 1) unlock("first_oath")
        if (facts.oathsConverted >= 1) unlock("oath_proven")

        // ── TIER 3 — MASTERY ───────────────────────────────────────────
        progress("centurion", minOf(maxStreak, 100))
        progress("ghost_protocol", facts.consecutivePerfectDays)
        val mindfulHabit = active.find { it.category.name.contains("MIND", ignoreCase = true) }
        progress("the_philosopher", (mindfulHabit?.streak ?: 0))
        progress("unseen_hours", facts.earlyMorningCount)
        progress("dual_threat", habitsWithStreak50)
        progress("the_collector", habits.count { !it.isArchived })
        progress("ironclad", if (active.isNotEmpty() && active.all { it.streak >= 60 }) 60 else 0)
        progress("the_observer", facts.consecutiveStatsDays)
        progress("velocity", facts.velocityStreak)
        progress("legacy", daysSinceInstall)
        progress("five_hundred", totalCompletions)
        if (languageChanged) unlock("polyglot")
        if (facts.goalsCompleted >= 1) unlock("goal_done")
        progress("social_five", facts.friendsCount)
        progress("focus_flow", facts.focusSessions)
        progress("challenge_three", facts.challengeWins)
        val unbroken = if (facts.oathsForfeited == 0) facts.oathsConverted else 0
        progress("unbroken_oath", unbroken)
        if (facts.oathsForfeited >= 1) unlock("benefactor")
        progress("oath_grove", facts.coinsPlanted)
        progress("oath_five", facts.oathsConverted)
        progress("grove_patron", facts.coinsGifted)
        if (facts.cosmeticsOwned >= 1) unlock("grove_dressed")

        // ── TIER 4 — LEGEND ────────────────────────────────────────────
        progress("the_constant", minOf(maxStreak, 365))
        val pantheonCount = pantheonIds.count { existingMap[it]?.isUnlocked == true }
        progress("pantheon", pantheonCount, targetOverride = pantheonTarget)
        progress(
            "architects_master",
            if (active.size >= 10 && active.all { it.streak >= 30 }) 10 else active.size.coerceAtMost(9),
        )
        progress("eclipse", facts.monthsWithActivity().size)
        progress("obsidian_chain", totalCompletions)
        progress("first_challenge_w", facts.challengeWins)
        progress("night_shift", facts.lateNightCount)
        progress("deep_roots", currentLevel)
        progress("garden_master", habitsWithStreak30)
        progress("royalty", currentLevel)
        progress("ancient_one", currentLevel)
        progress("full_spectrum", categoriesUsed)
        progress("vowkeeper", facts.oathsConverted)
        progress("deep_planted", facts.coinsPlanted)

        // ── TIER 5 — MYTHIC ────────────────────────────────────────────
        progress("the_long_shadow", minOf(maxStreak, 500))
        progress("infinite", totalCompletions)
        progress("unseen_war", facts.veryEarlyCount)
        progress("still_standing", facts.surpassedCount)
        if (facts.originDone) unlock("the_origin")
        if (facts.launchDayPerfect) unlock("launch_day")

        return TrophyCatalog.all.map { def ->
            existingMap[def.id] ?: Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                icon = def.icon,
                xpReward = def.xpReward,
                progressTarget = def.progressTarget,
            )
        }
    }

    /**
     * Full Hall of Trophies — every catalog entry, earned or locked with progress.
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
                progressTarget = def.progressTarget,
            )
            VisibleTrophy(
                achievement = a,
                tier = def.tier,
                state = if (a.isUnlocked) VisibleState.EARNED else VisibleState.LOCKED,
            )
        }.sortedWith(
            compareBy<VisibleTrophy> { it.tier.ordinalIndex }
                .thenByDescending { it.state == VisibleState.EARNED }
                .thenBy { !it.achievement.isUnlocked }
                .thenBy { it.achievement.progressTarget }
                .thenBy { it.achievement.title },
        )
    }

    /** @deprecated Use [hallTrophies]; kept for any stale call sites. */
    fun visibleTrophies(allEvaluated: List<Achievement>): List<VisibleTrophy> =
        hallTrophies(allEvaluated)

    data class VisibleTrophy(
        val achievement: Achievement,
        val tier: TrophyTierLevel,
        val state: VisibleState,
    )

    enum class VisibleState { EARNED, LOCKED }
}
