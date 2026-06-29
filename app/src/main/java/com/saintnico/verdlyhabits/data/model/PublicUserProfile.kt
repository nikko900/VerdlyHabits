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
)
