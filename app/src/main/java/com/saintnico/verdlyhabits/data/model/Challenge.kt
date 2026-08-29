package com.saintnico.verdlyhabits.data.model

import com.saintnico.verdlyhabits.util.ChallengeStreakCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ChallengeMode {
    STREAK,
    REACTIONS,
    HYBRID;

    companion object {
        fun fromRaw(value: String?): ChallengeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HYBRID
    }
}

object ReactionWeights {
    const val FIRE = "fire"
    const val HEART = "heart"
    const val LIGHTNING = "lightning"

    val weights: Map<String, Int> = mapOf(
        FIRE to 3,
        HEART to 2,
        LIGHTNING to 1,
    )

    fun pointsFor(reactionKey: String): Int = weights[reactionKey] ?: 0

    fun isAllowed(reactionKey: String): Boolean = reactionKey in weights
}

data class ChallengeResultStanding(
    val uid: String = "",
    val score: Int = 0,
    val streak: Int = 0,
    val reactionPoints: Int = 0,
)

data class ChallengeTopPost(
    val proofKey: String = "",
    val ownerUid: String = "",
    val photoUrl: String = "",
    val reactionPoints: Int = 0,
)

data class ChallengeResultSnapshot(
    val winnerUid: String = "",
    val winnerTitle: String = "",
    val challengeMode: ChallengeMode = ChallengeMode.HYBRID,
    val finalLeaderboard: List<ChallengeResultStanding> = emptyList(),
    val topPosts: List<ChallengeTopPost> = emptyList(),
    val finalizedAt: Long = 0L,
)

data class Challenge(
    val id: String = "",
    val creatorId: String = "",
    val habitName: String = "",
    val stake: String = "",
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val memberPhotos: Map<String, String> = emptyMap(),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L,
    val completions: Map<String, Map<String, Any>> = emptyMap(),
    val proofPhotos: Map<String, Map<String, String>> = emptyMap(),
    val reactions: Map<String, Map<String, List<String>>> = emptyMap(),
    val memberReactionPoints: Map<String, Int> = emptyMap(),
    val challengeMode: ChallengeMode = ChallengeMode.HYBRID,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val endedAt: Long? = null,
    val archivedReason: String? = null,
    val resultSnapshot: ChallengeResultSnapshot? = null,
    val leftMembers: List<String> = emptyList(),
    val memberStreaks: Map<String, Int> = emptyMap(),
    val memberXp: Map<String, Int> = emptyMap(),
    val missedDays: Map<String, Int> = emptyMap(),
    val inviteCode: String = "",
    val streakFreezeAvailable: Map<String, Boolean> = emptyMap(),
    val streakFreezeUsed: Map<String, Boolean> = emptyMap(),
    val rankChanges: Map<String, Int> = emptyMap(),
    /** Creator-set daily proof window: MORNING, AFTERNOON, NIGHT, ANYTIME */
    val dailyDeadline: String = "ANYTIME"
) {
    fun streakFor(userId: String): Int {
        val userCompletions = completions[userId] ?: return memberStreaks[userId] ?: 0
        val anchor = ChallengeStreakCalculator.anchorDateFor(userCompletions)
        val fromHistory = ChallengeStreakCalculator.currentStreak(
            userCompletions = userCompletions,
            anchorDate = anchor,
            challengeStartMillis = startDate,
        )
        val stored = memberStreaks[userId]
        // Stored 0 after a missed-day reset is often wrong — completions are source of truth.
        return when {
            stored == null -> fromHistory
            stored <= 0 && fromHistory > 0 -> fromHistory
            fromHistory > stored -> fromHistory
            else -> stored
        }
    }

    private fun dayMs(): Long = 24 * 60 * 60 * 1000L

    fun durationDays(): Int =
        (((endDate - startDate) / dayMs()).toInt()).coerceAtLeast(1)

    fun daysRemaining(): Long {
        val now = System.currentTimeMillis()
        val remaining = endDate - now
        return if (remaining > 0) (remaining + dayMs() - 1) / dayMs() else 0
    }

    /** Calendar days since start (0 on day one). Uncapped — prefer [currentDayIndex] for UI. */
    fun daysElapsed(): Int {
        val elapsed = System.currentTimeMillis() - startDate
        return (elapsed / dayMs()).toInt().coerceAtLeast(0)
    }

    /** 1-based day within the challenge window, never above [durationDays]. */
    fun currentDayIndex(): Int = (daysElapsed() + 1).coerceIn(1, durationDays())

    fun isPastEndDate(): Boolean = System.currentTimeMillis() >= endDate

    fun isEffectivelyActive(): Boolean = isActive && !isArchived && !isPastEndDate()

    fun leaderboard(): List<Pair<String, Int>> {
        return when (challengeMode) {
            ChallengeMode.STREAK -> streakLeaderboard()
            ChallengeMode.REACTIONS -> reactionLeaderboard()
            ChallengeMode.HYBRID -> hybridLeaderboard()
        }
    }

    fun streakLeaderboard(): List<Pair<String, Int>> {
        return members.map { uid -> uid to streakFor(uid) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }

    fun reactionLeaderboard(): List<Pair<String, Int>> {
        return members.map { uid -> uid to reactionPointsFor(uid) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { streakFor(it.first) }.thenBy { it.first })
    }

    fun hybridLeaderboard(): List<Pair<String, Int>> {
        return members.map { uid -> uid to hybridScoreFor(uid) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { reactionPointsFor(it.first) }.thenBy { it.first })
    }

    fun isExpired(): Boolean = System.currentTimeMillis() > endDate && isActive

    fun rankOf(userId: String): Int {
        return leaderboard().indexOfFirst { it.first == userId } + 1
    }

    /** Points/score behind the leader; null when already #1 or board empty. */
    fun gapToLeader(userId: String): Int? {
        val board = leaderboard()
        if (board.isEmpty()) return null
        val rank = rankOf(userId)
        if (rank <= 1) return null
        return (board.first().second - scoreFor(userId)).coerceAtLeast(0)
    }

    fun reactionPointsFor(userId: String): Int = memberReactionPoints[userId] ?: 0

    fun hybridScoreFor(userId: String): Int = (streakFor(userId) * 10) + reactionPointsFor(userId)

    fun scoreFor(userId: String): Int = when (challengeMode) {
        ChallengeMode.STREAK -> streakFor(userId)
        ChallengeMode.REACTIONS -> reactionPointsFor(userId)
        ChallengeMode.HYBRID -> hybridScoreFor(userId)
    }

    fun scoreLabel(): String = when (challengeMode) {
        ChallengeMode.STREAK -> "streak"
        ChallengeMode.REACTIONS -> "pts"
        ChallengeMode.HYBRID -> "score"
    }

    fun winnerTitle(): String = when (challengeMode) {
        ChallengeMode.STREAK -> "Most Consistent"
        ChallengeMode.REACTIONS -> "Crowd Favourite"
        ChallengeMode.HYBRID -> "Hybrid Champion"
    }

    fun completionRate(userId: String): Float {
        val totalDays = currentDayIndex().coerceAtLeast(1)
        val daysCompleted = completions[userId]?.count { entry ->
            val value = entry.value
            if (value is Boolean) value
            else (value as? Map<*, *>)?.get("completed") == true
        } ?: 0
        return daysCompleted.toFloat() / totalDays.toFloat()
    }

    fun hasCompletedToday(userId: String, todayDate: String): Boolean {
        val dayData = completions[userId]?.get(todayDate)
        return when (dayData) {
            is Boolean -> dayData
            is Map<*, *> -> dayData["completed"] == true
            else -> false
        }
    }

    fun hasStreakFreeze(userId: String): Boolean {
        val available = streakFreezeAvailable[userId] ?: true  // default: available
        val used = streakFreezeUsed[userId] ?: false
        return available && !used
    }

    fun finalStanding(): List<Triple<String, Int, Int>> {
        return members.map { uid ->
            Triple(uid, streakFor(uid), scoreFor(uid))
        }.sortedByDescending { it.third }
    }

    fun deadlineHour(): Int = when (dailyDeadline) {
        "MORNING" -> 12
        "AFTERNOON" -> 18
        "NIGHT" -> 23
        "ANYTIME" -> 24
        else -> 24
    }

    fun deadlineLabel(): String = when (dailyDeadline) {
        "MORNING" -> "Morning · by 12PM"
        "AFTERNOON" -> "Afternoon · by 6PM"
        "NIGHT" -> "Night · by 11PM"
        "ANYTIME" -> "Anytime · by midnight"
        else -> "Anytime · by midnight"
    }

    fun isLateProof(userId: String, date: String): Boolean {
        val dayData = completions[userId]?.get(date)
        return (dayData as? Map<*, *>)?.get("isLate") == true
    }

    fun isAfterDeadlineProof(userId: String, date: String): Boolean {
        val dayData = completions[userId]?.get(date)
        return (dayData as? Map<*, *>)?.get("isAfterDeadline") == true
    }

    fun currentProofWindowState(userId: String, todayStr: String): ProofWindowState {
        val todayEntry = completions[userId]?.get(todayStr)
        val postedToday = when (todayEntry) {
            is Boolean -> todayEntry
            is Map<*, *> -> todayEntry["completed"] == true
            else -> false
        }
        val isLateToday = (todayEntry as? Map<*, *>)?.get("isLate") == true
        if (postedToday && isLateToday) return ProofWindowState.AlreadyPostedLate
        if (postedToday) return ProofWindowState.AlreadyPosted

        val hour = LocalTime.now().hour
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val yesterdayStr = LocalDate.now().minusDays(1).format(fmt)
        if (hour < 3) {
            val yesterdayEntry = completions[userId]?.get(yesterdayStr)
            val postedYesterday = when (yesterdayEntry) {
                is Boolean -> yesterdayEntry
                is Map<*, *> -> yesterdayEntry["completed"] == true
                else -> false
            }
            return if (!postedYesterday) ProofWindowState.GraceWindow else ProofWindowState.Locked
        }

        val dh = deadlineHour()
        return if (hour < dh) ProofWindowState.OpenFullXP else ProofWindowState.OpenReducedXP
    }
}

sealed class ProofWindowState {
    data object OpenFullXP : ProofWindowState()
    data object OpenReducedXP : ProofWindowState()
    data object GraceWindow : ProofWindowState()
    data object Locked : ProofWindowState()
    data object AlreadyPosted : ProofWindowState()
    data object AlreadyPostedLate : ProofWindowState()
}

data class ActivityItem(
    val id: String = "",
    val type: String = "",
    val actorId: String = "",
    val actorUsername: String = "UnknownRival",
    val actorPhotoURL: String = "",
    val message: String = "",
    val proofPhotoUrl: String? = null,
    val timestamp: Long = 0L,
    val metadata: Map<String, Any> = emptyMap()
)
