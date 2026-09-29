package com.saintnico.verdlyhabits.data.model

/**
 * Fields loaded from `users/{uid}` for display on member profiles.
 * Firestore rules allow signed-in reads of user documents.
 */
data class PublicUserProfile(
    val uid: String,
    val displayName: String,
    val username: String,
    val photoUrl: String?,
    val bio: String,
    val motto: String,
    val favoritePlant: String,
    val profileAccent: String = "sage",
    val level: Int = 0,
    val xp: Int = 0,
    /** When false, the "Recent proof" highlight reel is hidden from other viewers. */
    val showRecentProof: Boolean = true,
    /** User-selected title id; null = auto signature on public profile. */
    val equippedTitleId: String? = null,
    val equippedTitleLabel: String? = null,
    val titleGlowId: String? = null,
    val profileFrameId: String? = null,
    val grovePlantId: String? = null,
    val oathChipId: String? = null,
    val pinnedTrophyLabel: String? = null,
    val pinnedTrophyUntil: Long = 0L,
    /** Synced membership badge for friends, leaderboards, and public profile. */
    val membershipTier: MembershipTier = MembershipTier.FREE,
    val lastPlantedAt: Long = 0L,
    val lastActiveAt: Long = 0L,
    val shareLastActive: Boolean = true,
    val lastPlantedDay: String? = null,
    val plantedCountToday: Int = 0,
    val weeklyPlantDays: Int = 0,
    val weeklyPlantWeekKey: String? = null,
    val currentStreakPublic: Int = 0,
    val standInOpenGrove: Boolean = false,
    val openGroveWeekRank: Int = 0,
    val openGrovePercentile: Int = 0,
    val openGroveWeekKey: String? = null,
    val groveLeadUntil: Long = 0L,
    val witnessCountToday: Int = 0,
    val witnessDay: String? = null,
    /** Opt-in habit title shown on the home grove pulse. Ignored unless [sharedPulseDay] is today. */
    val sharedPulseHabit: String? = null,
    val sharedPulseDay: String? = null,
)
