package com.saintnico.verdlyhabits.engine

/**
 * Derives a single "signature" title for a player from accolades we can compute
 * client-side (challenge wins, podiums, best streak, level). Auto-equipped: the
 * rarest earned title wins, so a profile always shows its most impressive badge
 * without the user choosing anything.
 */
object ProfileTitleEngine {

    enum class Tier { LEGENDARY, EPIC, RARE, COMMON }

    data class SignatureTitle(
        val label: String,
        val tier: Tier,
    )

    /**
     * @param wins        first-place finishes in ended challenges
     * @param podiums     top-3 finishes in ended challenges
     * @param bestStreak  longest streak achieved in any shared arena
     * @param totalShared number of challenges entered
     * @param level       gamification level (for the fallback title)
     */
    fun signatureTitle(
        wins: Int,
        podiums: Int,
        bestStreak: Int,
        totalShared: Int,
        level: Int,
    ): SignatureTitle = when {
        wins >= 5 -> SignatureTitle("Grand Champion", Tier.LEGENDARY)
        wins >= 3 -> SignatureTitle("$wins-Time Champion", Tier.LEGENDARY)
        bestStreak >= 30 -> SignatureTitle("30-Day Legend", Tier.LEGENDARY)
        wins >= 1 -> SignatureTitle("Champion", Tier.EPIC)
        bestStreak >= 21 -> SignatureTitle("Iron Will", Tier.EPIC)
        podiums >= 3 -> SignatureTitle("Podium Regular", Tier.RARE)
        bestStreak >= 14 -> SignatureTitle("Fortnight Flame", Tier.RARE)
        podiums >= 1 -> SignatureTitle("Podium Finisher", Tier.RARE)
        bestStreak >= 7 -> SignatureTitle("Week Warrior", Tier.COMMON)
        totalShared >= 1 -> SignatureTitle("Challenger", Tier.COMMON)
        else -> SignatureTitle(GamificationEngine.levelTitle(level), Tier.COMMON)
    }
}
