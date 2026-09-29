package com.saintnico.verdlyhabits.data.model

/**
 * Public membership badge shown on profile avatars across the app.
 * Synced to `users/{uid}.membershipTier` so friends can see trial/premium status.
 */
enum class MembershipTier(val firestoreValue: String) {
    FREE("free"),
    TRIAL("trial"),
    PREMIUM("premium"),
    ;

    val showsPremiumBadge: Boolean
        get() = this != FREE

    val badgeLabel: String
        get() = when (this) {
            PREMIUM -> "PREMIUM"
            TRIAL -> "TRIAL"
            FREE -> ""
        }

    val compactBadgeLabel: String
        get() = when (this) {
            PREMIUM -> "Premium"
            TRIAL -> "Trial"
            FREE -> ""
        }

    companion object {
        fun fromFirestore(value: String?): MembershipTier =
            when (value?.lowercase()) {
                "premium" -> PREMIUM
                "trial" -> TRIAL
                else -> FREE
            }
    }
}
