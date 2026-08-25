package com.saintnico.verdlyhabits.data.model

/**
 * Global dev broadcast shown in Activity / Live Pulse.
 * Create docs in Firestore `announcements/{id}` from Firebase Console or Cloud Functions.
 */
data class DevAnnouncement(
    val id: String,
    val title: String,
    val body: String,
    val route: String = "",
    val ctaLabel: String = "",
    val active: Boolean = true,
    val audience: String = "all",
    val expiresAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
)
