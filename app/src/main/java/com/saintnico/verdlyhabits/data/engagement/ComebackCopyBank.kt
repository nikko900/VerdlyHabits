package com.saintnico.verdlyhabits.data.engagement

/**
 * Non-shame copy for Welcome Back, keyed by gap tier and last engagement context.
 * Multiple variants per tier so repeat lapses do not feel robotic.
 */
object ComebackCopyBank {

    data class CopySet(
        val headline: String,
        val body: String,
        val contextLine: String? = null,
        val primaryLabel: String,
        val destination: ComebackDestination,
    )

    fun pick(
        tier: EngagementGapTier,
        lastKind: EngagementKind,
        hasIncompleteHabitToday: Boolean,
        hasActiveGoals: Boolean,
        hasRepairableStreak: Boolean,
        seed: Long = System.currentTimeMillis(),
    ): CopySet {
        val variants = headlinesFor(tier)
        val headline = variants[(seed % variants.size).toInt().coerceAtLeast(0)]
        val body = bodiesFor(tier, hasRepairableStreak)[(seed % 4).toInt().coerceIn(0, 3)]
        val contextLine = contextLineFor(lastKind)
        val (label, dest) = primaryActionFor(
            lastKind = lastKind,
            hasIncompleteHabitToday = hasIncompleteHabitToday,
            hasActiveGoals = hasActiveGoals,
        )
        return CopySet(
            headline = headline,
            body = body,
            contextLine = contextLine,
            primaryLabel = label,
            destination = dest,
        )
    }

    private fun headlinesFor(tier: EngagementGapTier): List<String> = when (tier) {
        EngagementGapTier.SHORT -> listOf(
            "Good to see you",
            "Welcome back",
            "You're here. That counts",
            "Ready when you are",
        )
        EngagementGapTier.MEDIUM -> listOf(
            "Glad you're back",
            "We kept your spot warm",
            "No rush. Start small",
            "Pick up whenever you're ready",
        )
        EngagementGapTier.LONG -> listOf(
            "Welcome back to Verdly",
            "Your habits are still here",
            "Fresh start, same you",
            "One small step is enough",
        )
        EngagementGapTier.EXTENDED -> listOf(
            "Hey. Welcome back",
            "Your garden waited patiently",
            "Starting again is still progress",
            "Glad you opened the app",
        )
    }

    private fun bodiesFor(tier: EngagementGapTier, hasRepairableStreak: Boolean): List<String> {
        val streakNote = if (hasRepairableStreak) {
            "Your streaks paused while you were away. One small action today picks things back up."
        } else {
            when (tier) {
                EngagementGapTier.SHORT ->
                    "Life happens. One small action today is all it takes to stay connected."
                EngagementGapTier.MEDIUM ->
                    "You do not need to catch up on everything. Just do one thing that feels easy."
                EngagementGapTier.LONG ->
                    "There is nothing to make up for. Start with a single habit and build from there."
                EngagementGapTier.EXTENDED ->
                    "Coming back is the hardest part. And you already did it by opening Verdly."
            }
        }
        return listOf(streakNote, streakNote, streakNote, streakNote)
    }

    private fun contextLineFor(kind: EngagementKind): String? = when (kind) {
        EngagementKind.HABIT_CREATED ->
            "Last time you were planting a new habit."
        EngagementKind.HABIT_ARCHIVED, EngagementKind.HABIT_PAUSED ->
            "Last time you were simplifying your routine."
        EngagementKind.GOAL_CHECKIN, EngagementKind.GOAL_CREATED, EngagementKind.GOAL_VIEWED ->
            "Last time you were working on your goals."
        EngagementKind.COIN_SPENT, EngagementKind.COIN_EARNED, EngagementKind.CHALLENGE_JOINED,
        EngagementKind.CHALLENGE_VIEWED ->
            "Last time you were in the challenge arena."
        EngagementKind.FOCUS_SESSION ->
            "Last time you were in focus mode."
        EngagementKind.DUO_INTERACTION ->
            "Last time you were checking in with your duo."
        EngagementKind.STATS_VIEWED ->
            "Last time you were reviewing your progress."
        EngagementKind.HABIT_COMPLETED ->
            "Last time you were building momentum."
        EngagementKind.APP_OPEN, EngagementKind.UNKNOWN -> null
    }

    private fun primaryActionFor(
        lastKind: EngagementKind,
        hasIncompleteHabitToday: Boolean,
        hasActiveGoals: Boolean,
    ): Pair<String, ComebackDestination> {
        return when (lastKind) {
            EngagementKind.GOAL_CHECKIN, EngagementKind.GOAL_CREATED, EngagementKind.GOAL_VIEWED ->
                if (hasActiveGoals) {
                    "Check on your goals" to ComebackDestination.GOALS
                } else {
                    fallbackHabitAction(hasIncompleteHabitToday)
                }
            EngagementKind.COIN_SPENT, EngagementKind.COIN_EARNED,
            EngagementKind.CHALLENGE_JOINED, EngagementKind.CHALLENGE_VIEWED ->
                "See what's happening" to ComebackDestination.CHALLENGES
            EngagementKind.FOCUS_SESSION ->
                "Start a focus session" to ComebackDestination.FOCUS
            EngagementKind.HABIT_ARCHIVED, EngagementKind.HABIT_PAUSED ->
                "Do one thing today" to ComebackDestination.HOME_COMPLETE_HABIT
            else -> fallbackHabitAction(hasIncompleteHabitToday)
        }
    }

    private fun fallbackHabitAction(hasIncompleteHabitToday: Boolean): Pair<String, ComebackDestination> =
        if (hasIncompleteHabitToday) {
            "Do one thing today" to ComebackDestination.HOME_COMPLETE_HABIT
        } else {
            "Browse your habits" to ComebackDestination.HOME_BROWSE
        }

    /** Secondary dismiss label — always low pressure. */
    fun dismissLabel(): String = "Just look around"
}
