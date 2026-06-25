package com.saintnico.verdlyhabits.data.model

enum class FriendshipStatus {
    NONE,
    OUTGOING_PENDING,
    INCOMING_PENDING,
    ACCEPTED,
    BLOCKED
}

data class Friendship(
    val id: String = "",
    val from: String = "",
    val to: String = "",
    val status: String = "PENDING",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val fromUsername: String = "",
    val fromPhotoUrl: String = "",
    val toUsername: String = "",
    val toPhotoUrl: String = ""
)

data class FriendSummary(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val sharedChallenges: Int = 0
)

data class MemberPublicProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val bio: String,
    val motto: String,
    val accentKey: String,
    val links: List<ProfileLink>,
    val level: Int,
    val totalXp: Long,
    val longestStreak: Int,
    val totalCompletions: Long,
    val totalFocusMinutes: Long,
    val friendsCount: Int,
    val visibility: String,
    val createdAt: Long
)

data class ProfileLink(
    val label: String = "",
    val url: String = ""
)
