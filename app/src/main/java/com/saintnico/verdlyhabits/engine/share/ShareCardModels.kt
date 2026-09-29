package com.saintnico.verdlyhabits.engine.share

enum class ShareCardType {
    TROPHY_TIER_BRONZE,
    TROPHY_TIER_SILVER,
    TROPHY_TIER_GOLD,
    TROPHY_TIER_OBSIDIAN,
    TROPHY_TIER_MYTHIC,
    GOAL_MILESTONE_STANDARD,
    GOAL_MILESTONE_RITUAL,
    GOAL_COMPLETION,
    GOAL_DAILY_RECAP,
    DUO_STREAK_MILESTONE,
    PROFILE_CARD,
    WRAPPED_RECAP,
}

enum class ShareMotifCategory {
    STREAK,
    TIME_OF_DAY,
    CONSISTENCY,
    SOCIAL,
    LEVEL,
    SECRET,
    DUO_RINGS,
    PROGRESS_RING,
    CLOSED_RING,
}

data class ShareCardTheme(
    val gradient: List<Long>,
    val accent: Long,
    val textColor: Long,
    val particleTreatment: Boolean = false,
)

data class ShareMotif(
    val category: ShareMotifCategory,
    val progressPercent: Int = 0,
    val growthMarkEnabled: Boolean = false,
    val streakDays: Int = 0,
)

data class ShareCardSpec(
    val cardType: ShareCardType,
    val width: Int,
    val height: Int,
    val theme: ShareCardTheme,
    val motif: ShareMotif?,
    val chipLabel: String?,
    val headline: String,
    val subhead: String?,
    val statLine: String?,
    val xpChip: String?,
    val whyQuote: String?,
    val footerLink: String,
    val caption: String,
    val reclaimedChip: String? = null,
    /** Profile trading card: @username shown beside the monogram. */
    val identityLabel: String? = null,
    val memberSinceLabel: String? = null,
    val monogram: String? = null,
    /** Full https referral URL encoded into the corner QR. */
    val qrPayload: String? = null,
    /** Featured trophy accent so the badge pops against the emerald frame. */
    val trophyAccent: Long? = null,
)
