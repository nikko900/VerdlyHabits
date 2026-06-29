package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.engine.Achievement

/**
 * Derives signature titles and an equipable catalog for profile identity.
 * Auto-signature picks the rarest earned title; users can override via [equippedTitleId].
 */
object ProfileTitleEngine {

    enum class Tier { LEGENDARY, EPIC, RARE, COMMON }

    data class SignatureTitle(
        val id: String,
        val label: String,
        val tier: Tier,
    )

    data class ProfileTitle(
        val id: String,
        val label: String,
        val tier: Tier,
        val hint: String = "",
    )

    fun signatureTitle(
        wins: Int,
        podiums: Int,
        bestStreak: Int,
        totalShared: Int,
        level: Int,
    ): SignatureTitle {
        val title = when {
            wins >= 5 -> ProfileTitle("sig_grand_champion", "Grand Champion", Tier.LEGENDARY, "5+ challenge wins")
            wins >= 3 -> ProfileTitle("sig_multi_champion", "$wins-Time Champion", Tier.LEGENDARY, "Multiple wins")
            bestStreak >= 30 -> ProfileTitle("sig_30_day", "30-Day Legend", Tier.LEGENDARY, "30-day arena streak")
            wins >= 1 -> ProfileTitle("sig_champion", "Champion", Tier.EPIC, "Challenge winner")
            bestStreak >= 21 -> ProfileTitle("sig_iron_will", "Iron Will", Tier.EPIC, "21-day streak")
            podiums >= 3 -> ProfileTitle("sig_podium_regular", "Podium Regular", Tier.RARE, "3+ podiums")
            bestStreak >= 14 -> ProfileTitle("sig_fortnight", "Fortnight Flame", Tier.RARE, "14-day streak")
            podiums >= 1 -> ProfileTitle("sig_podium", "Podium Finisher", Tier.RARE, "Top-3 finish")
            bestStreak >= 7 -> ProfileTitle("sig_week_warrior", "Week Warrior", Tier.COMMON, "7-day streak")
            totalShared >= 1 -> ProfileTitle("sig_challenger", "Challenger", Tier.COMMON, "Joined a challenge")
            else -> ProfileTitle("sig_level", GamificationEngine.levelTitle(level), Tier.COMMON, "Level title")
        }
        return SignatureTitle(title.id, title.label, title.tier)
    }

    /** All titles the player has unlocked — sorted rarest first. */
    fun unlockedTitles(
        wins: Int,
        podiums: Int,
        bestStreak: Int,
        totalShared: Int,
        level: Int,
        duoStreakDays: Int,
        totalFocusMinutes: Int,
        totalCompletions: Int,
        isPro: Boolean,
        achievements: List<Achievement>,
    ): List<ProfileTitle> {
        val out = linkedMapOf<String, ProfileTitle>()

        fun add(id: String, label: String, tier: Tier, hint: String) {
            out[id] = ProfileTitle(id, label, tier, hint)
        }

        // --- Signature ladder (Wins / Competitions) ---
        if (wins >= 50) add("sig_god", "God of the Arena", Tier.LEGENDARY, "50+ challenge wins")
        if (wins >= 25) add("sig_titan", "Titan", Tier.LEGENDARY, "25+ challenge wins")
        if (wins >= 10) add("sig_conqueror", "Conqueror", Tier.LEGENDARY, "10+ challenge wins")
        if (wins >= 5) add("sig_grand_champion", "Grand Champion", Tier.LEGENDARY, "5+ wins")
        if (wins >= 3) add("sig_multi_champion", "$wins-Time Champion", Tier.LEGENDARY, "Multiple wins")
        if (wins >= 1) add("sig_champion", "Champion", Tier.EPIC, "Won a challenge")
        
        if (podiums >= 50) add("sig_eternal_contender", "Eternal Contender", Tier.LEGENDARY, "50+ podiums")
        if (podiums >= 20) add("sig_podium_king", "Podium King", Tier.EPIC, "20+ podiums")
        if (podiums >= 10) add("sig_elite_finisher", "Elite Finisher", Tier.EPIC, "10+ podiums")
        if (podiums >= 3) add("sig_podium_regular", "Podium Regular", Tier.RARE, "3+ podiums")
        if (podiums >= 1) add("sig_podium", "Podium Finisher", Tier.RARE, "Top-3 finish")

        if (totalShared >= 100) add("sig_arena_vet", "Arena Veteran", Tier.EPIC, "100+ challenges joined")
        if (totalShared >= 50) add("sig_gladiator", "Gladiator", Tier.RARE, "50+ challenges joined")
        if (totalShared >= 10) add("sig_competitor", "Competitor", Tier.COMMON, "10+ challenges joined")
        if (totalShared >= 1) add("sig_challenger", "Challenger", Tier.COMMON, "In the arena")

        // --- Streak Ladder ---
        if (bestStreak >= 1000) add("sig_millennium", "Millennium Master", Tier.LEGENDARY, "1000-day streak")
        if (bestStreak >= 500) add("sig_half_millennium", "500 Club", Tier.LEGENDARY, "500-day streak")
        if (bestStreak >= 365) add("sig_year_long", "Year-Long Myth", Tier.LEGENDARY, "365-day streak")
        if (bestStreak >= 200) add("sig_unstoppable", "Unstoppable", Tier.LEGENDARY, "200-day streak")
        if (bestStreak >= 100) add("sig_century", "Century Club", Tier.LEGENDARY, "100-day streak")
        if (bestStreak >= 50) add("sig_half_century", "Half-Century", Tier.EPIC, "50-day streak")
        if (bestStreak >= 30) add("sig_30_day", "30-Day Legend", Tier.LEGENDARY, "30-day streak") // Legacy override if needed
        if (bestStreak >= 21) add("sig_iron_will", "Iron Will", Tier.EPIC, "21-day streak")
        if (bestStreak >= 14) add("sig_fortnight", "Fortnight Flame", Tier.RARE, "14-day streak")
        if (bestStreak >= 7) add("sig_week_warrior", "Week Warrior", Tier.COMMON, "7-day streak")
        if (bestStreak >= 3) add("sig_spark", "Spark", Tier.COMMON, "3-day streak")

        // --- Level milestones ---
        if (level >= 100) add("lvl_world_tree", "World Tree", Tier.LEGENDARY, "Level 100+")
        if (level >= 80) add("lvl_ancient", "Ancient One", Tier.LEGENDARY, "Level 80+")
        if (level >= 60) add("lvl_archdruid", "Archdruid", Tier.LEGENDARY, "Level 60+")
        if (level >= 40) add("lvl_forest", "Forest Sage", Tier.LEGENDARY, "Level 40+")
        if (level >= 30) add("lvl_nature_guardian", "Nature Guardian", Tier.EPIC, "Level 30+")
        if (level >= 20) add("lvl_grove", "Grove Keeper", Tier.EPIC, "Level 20+")
        if (level >= 15) add("lvl_botanist", "Botanist", Tier.RARE, "Level 15+")
        if (level >= 10) add("lvl_sapling", "Sapling", Tier.RARE, "Level 10+")
        if (level >= 5) add("lvl_sprout", "Sprout", Tier.COMMON, "Level 5+")
        add("lvl_current", GamificationEngine.levelTitle(level), Tier.COMMON, "Current level")

        // --- Duo ---
        if (duoStreakDays >= 365) add("duo_soulmates", "Soulmates", Tier.LEGENDARY, "365-day duo streak")
        if (duoStreakDays >= 200) add("duo_unbreakable", "Unbreakable Bond", Tier.LEGENDARY, "200-day duo streak")
        if (duoStreakDays >= 100) add("duo_centurion", "Duo Centurion", Tier.LEGENDARY, "100-day duo streak")
        if (duoStreakDays >= 50) add("duo_dynamic", "Dynamic Duo", Tier.EPIC, "50-day duo streak")
        if (duoStreakDays >= 30) add("duo_eternal", "Eternal Duo", Tier.LEGENDARY, "30-day duo streak")
        if (duoStreakDays >= 14) add("duo_legend", "Duo Legend", Tier.EPIC, "14-day duo streak")
        if (duoStreakDays >= 7) add("duo_flame", "Duo Flame", Tier.RARE, "7-day duo streak")

        // --- Focus ---
        if (totalFocusMinutes >= 10000) add("focus_enlightened", "Enlightened", Tier.LEGENDARY, "10,000+ focus mins")
        if (totalFocusMinutes >= 5000) add("focus_master", "Master of Time", Tier.LEGENDARY, "5000+ focus mins")
        if (totalFocusMinutes >= 2000) add("focus_flow", "Flow State", Tier.EPIC, "2000+ focus mins")
        if (totalFocusMinutes >= 1000) add("focus_sage", "Deep Work Sage", Tier.EPIC, "1000+ focus mins")
        if (totalFocusMinutes >= 300) add("focus_monk", "Focus Monk", Tier.RARE, "300+ focus mins")
        if (totalFocusMinutes >= 60) add("focus_apprentice", "Focus Apprentice", Tier.COMMON, "60+ focus mins")

        // --- Completions ---
        if (totalCompletions >= 10000) add("comp_mythic", "Mythic Executor", Tier.LEGENDARY, "10,000+ completions")
        if (totalCompletions >= 5000) add("comp_grandmaster", "Grandmaster", Tier.LEGENDARY, "5000+ completions")
        if (totalCompletions >= 2500) add("comp_relentless", "Relentless", Tier.LEGENDARY, "2500+ completions")
        if (totalCompletions >= 1000) add("comp_machine", "The Machine", Tier.EPIC, "1000+ completions")
        if (totalCompletions >= 500) add("architect", "Architect of Self", Tier.LEGENDARY, "500+ completions")
        if (totalCompletions >= 250) add("comp_artisan", "Artisan", Tier.EPIC, "250+ completions")
        if (totalCompletions >= 100) add("centurion", "Centurion", Tier.RARE, "100+ completions")
        if (totalCompletions >= 50) add("comp_builder", "Builder", Tier.COMMON, "50+ completions")

        // --- Pro ---
        if (isPro) {
            add("pro_crown", "Verdly Pro", Tier.EPIC, "Pro member")
            add("pro_vip", "VIP", Tier.EPIC, "Pro member")
            add("pro_supporter", "Supporter", Tier.RARE, "Pro member")
            add("pro_elite", "Elite", Tier.EPIC, "Pro member")
        }

        // Trophy-derived titles (unlocked achievements only)
        achievements.filter { it.isUnlocked }.forEach { a ->
            add("trophy_${a.id}", a.title, tierForAchievement(a), a.description)
        }

        if (out.isEmpty()) {
            out["sig_level"] = ProfileTitle("sig_level", GamificationEngine.levelTitle(level), Tier.COMMON, "Level title")
        }
        return out.values.sortedWith(
            compareByDescending<ProfileTitle> { tierRank(it.tier) }
                .thenBy { it.label },
        )
    }

    fun resolveEquipped(
        equippedId: String?,
        unlocked: List<ProfileTitle>,
        autoSignature: SignatureTitle,
    ): ProfileTitle {
        if (!equippedId.isNullOrBlank()) {
            unlocked.find { it.id == equippedId }?.let { return it }
        }
        return ProfileTitle(autoSignature.id, autoSignature.label, autoSignature.tier, "Auto-equipped")
    }

    fun tierColorArgb(tier: Tier): Long = when (tier) {
        Tier.LEGENDARY -> 0xFFFFC857
        Tier.EPIC -> 0xFFB388FF
        Tier.RARE -> 0xFF4FC3F7
        Tier.COMMON -> 0xFF95D5B2
    }

    private fun tierRank(tier: Tier): Int = when (tier) {
        Tier.LEGENDARY -> 4
        Tier.EPIC -> 3
        Tier.RARE -> 2
        Tier.COMMON -> 1
    }

    private fun tierForAchievement(a: Achievement): Tier = when {
        a.xpReward >= 500 -> Tier.LEGENDARY
        a.xpReward >= 200 -> Tier.EPIC
        a.xpReward >= 75 -> Tier.RARE
        else -> Tier.COMMON
    }
}
