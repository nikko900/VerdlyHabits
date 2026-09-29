package com.saintnico.verdlyhabits.data.engagement

/**
 * How the user last meaningfully interacted with Verdly — not habit completion alone,
 * but any signal that they were actively using the app.
 */
enum class EngagementKind(val wireValue: String) {
    APP_OPEN("app_open"),
    HABIT_COMPLETED("habit_completed"),
    HABIT_CREATED("habit_created"),
    HABIT_ARCHIVED("habit_archived"),
    HABIT_PAUSED("habit_paused"),
    GOAL_CHECKIN("goal_checkin"),
    GOAL_CREATED("goal_created"),
    GOAL_VIEWED("goal_viewed"),
    COIN_SPENT("coin_spent"),
    COIN_EARNED("coin_earned"),
    CHALLENGE_JOINED("challenge_joined"),
    CHALLENGE_VIEWED("challenge_viewed"),
    FOCUS_SESSION("focus_session"),
    DUO_INTERACTION("duo_interaction"),
    STATS_VIEWED("stats_viewed"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromWire(value: String?): EngagementKind =
            entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

/** Days since the user last opened the app — drives tone, never pressure escalation. */
enum class EngagementGapTier {
    /** 2 days — light nudge */
    SHORT,
    /** 3–4 days — warm check-in */
    MEDIUM,
    /** 5–7 days — supportive, no guilt */
    LONG,
    /** 8+ days — gentle re-entry */
    EXTENDED,
    ;

    companion object {
        fun fromDaysAway(days: Long): EngagementGapTier = when {
            days <= 2 -> SHORT
            days <= 4 -> MEDIUM
            days <= 7 -> LONG
            else -> EXTENDED
        }

        /** Minimum whole days away before Welcome Back surfaces. */
        const val WELCOME_BACK_THRESHOLD_DAYS = 2L
    }
}

/** Where the Welcome Back primary CTA should route after dismiss. */
enum class ComebackDestination {
    HOME_COMPLETE_HABIT,
    GOALS,
    CHALLENGES,
    FOCUS,
    HOME_BROWSE,
}

data class SessionEngagementSnapshot(
    val daysAway: Long = 0,
    val gapTier: EngagementGapTier? = null,
    val lastEngagementKind: EngagementKind = EngagementKind.UNKNOWN,
    val lastEngagementAt: Long = 0L,
    val sessionCount: Int = 0,
    val shouldShowWelcomeBack: Boolean = false,
    val absenceEpoch: Long = 0L,
)
