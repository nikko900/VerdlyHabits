package com.saintnico.verdlyhabits.engine

/**
 * Friend last-active. Both people must have sharing on, otherwise nothing is shown.
 * Missing [shareLastActive] is treated as on so older accounts stay visible.
 */
object LastActive {
    const val ONLINE_WITHIN_MS = 5 * 60 * 1000L
    const val SHOW_WITHIN_MS = 7 * 24 * 60 * 60 * 1000L

    data class Snapshot(
        val online: Boolean,
        /** Short chip for lists: "2m", "3h", "1d" — null when online. */
        val compactLabel: String?,
        /** Profile timeline line: "Active now" / "2 min ago" / "3 days ago". */
        val profileLabel: String,
    )

    fun resolve(
        viewerShares: Boolean,
        targetShares: Boolean,
        lastActiveAt: Long,
        nowMillis: Long = System.currentTimeMillis(),
        isSelf: Boolean = false,
    ): Snapshot? {
        if (isSelf) return null
        if (!viewerShares || !targetShares) return null
        if (lastActiveAt <= 0L) return null
        val age = nowMillis - lastActiveAt
        if (age < 0L || age > SHOW_WITHIN_MS) return null
        val online = age <= ONLINE_WITHIN_MS
        val ageLabel = relativeAge(age)
        val compact = if (online) null else compactAge(age)
        val profile = if (online) "Active now" else ageLabel
        return Snapshot(online = online, compactLabel = compact, profileLabel = profile)
    }

    /** Human timeline: "1 min ago", "15 min ago", "2 hours ago", "3 days ago". */
    fun relativeAge(ageMs: Long): String {
        val minutes = ageMs / 60_000L
        if (minutes < 1L) return "just now"
        if (minutes < 60L) {
            val m = minutes.coerceAtLeast(1L)
            return if (m == 1L) "1 min ago" else "$m min ago"
        }
        val hours = minutes / 60L
        if (hours < 24L) {
            return if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        val days = hours / 24L
        return if (days == 1L) "1 day ago" else "$days days ago"
    }

    internal fun compactAge(ageMs: Long): String {
        val minutes = ageMs / 60_000L
        if (minutes < 60L) return "${minutes.coerceAtLeast(1L)}m"
        val hours = minutes / 60L
        if (hours < 24L) return "${hours}h"
        val days = hours / 24L
        return "${days}d"
    }
}
