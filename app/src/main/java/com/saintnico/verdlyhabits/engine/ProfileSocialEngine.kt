package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ProfileSocialEngine {

enum class FlairType { PRO, DUO_FIRE, CHALLENGE_LEADER, PERFECT_WEEK, GROVE_LEAD }

    data class ProfileFlair(
        val type: FlairType,
        val label: String,
    )

    data class ProfileActivityItem(
        val id: String,
        val message: String,
        val timestamp: Long,
        val actorUsername: String? = null,
        val accent: ActivityAccent = ActivityAccent.NEUTRAL,
    )

    enum class ActivityAccent { NEUTRAL, SUCCESS, RANK, DUO }

    data class SocialStats(
        val friendsCount: Int,
        val duoStreakDays: Int,
        val challengeWins: Int,
        val referralsSent: Int,
    )

    data class MotivatedPerson(
        val uid: String,
        val username: String,
        val photoUrl: String?,
        val reason: String,
    )

    data class ProfileSocialSnapshot(
        val stats: SocialStats,
        val peopleMotivatedToday: Int,
        val activityFeed: List<ProfileActivityItem>,
        val flairs: List<ProfileFlair>,
        val memberStoryLine: String,
        val motivatedPeople: List<MotivatedPerson> = emptyList(),
    )

    fun buildSnapshot(
        userId: String,
        userName: String,
        memberSinceMillis: Long,
        totalCompletions: Int,
        longestStreak: Int,
        friends: List<FriendSummary>,
        duoState: DuoStreakState?,
        allChallenges: List<Challenge>,
        referralsSent: Int,
        isPro: Boolean,
        hasPerfectWeek: Boolean,
        groveLead: Boolean = false,
    ): ProfileSocialSnapshot {
        val wins = challengeWinsFor(allChallenges, userId)
        val bestArenaStreak = allChallenges.maxOfOrNull { it.streakFor(userId) } ?: 0
        val stats = SocialStats(
            friendsCount = friends.size,
            duoStreakDays = duoState?.streakDays ?: 0,
            challengeWins = wins,
            referralsSent = referralsSent,
        )
        val motivatedPeople = peopleMotivatedToday(userId, friends, duoState, allChallenges)
        val feed = buildActivityFeed(userId, userName, friends, duoState, allChallenges)
        val flairs = buildFlairs(isPro, duoState, allChallenges, userId, hasPerfectWeek, groveLead)
        val story = memberStoryLine(memberSinceMillis, totalCompletions, longestStreak.coerceAtLeast(bestArenaStreak))
        return ProfileSocialSnapshot(stats, motivatedPeople.size, feed, flairs, story, motivatedPeople)
    }

    fun memberStoryLine(memberSinceMillis: Long, totalCompletions: Int, bestStreak: Int): String {
        val monthYear = Instant.ofEpochMilli(memberSinceMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        return buildString {
            append("Building since $monthYear")
            append(" · ")
            append("$totalCompletions habit${if (totalCompletions == 1) "" else "s"} completed")
            if (bestStreak > 0) {
                append(" · Best streak $bestStreak")
            }
        }
    }

    fun challengeWinsFor(challenges: List<Challenge>, userId: String): Int =
        challenges.count { !it.isActive && finalPlacement(it, userId) == 1 }

    fun careerInputs(challenges: List<Challenge>, userId: String): CareerInputs {
        var wins = 0
        var podiums = 0
        var bestStreak = 0
        var totalShared = 0
        challenges.forEach { ch ->
            if (userId !in ch.members) return@forEach
            totalShared++
            bestStreak = maxOf(bestStreak, ch.streakFor(userId))
            if (!ch.isActive) {
                val place = finalPlacement(ch, userId)
                if (place == 1) wins++
                if (place != null && place <= 3) podiums++
            }
        }
        return CareerInputs(wins, podiums, bestStreak, totalShared)
    }

    data class CareerInputs(
        val wins: Int,
        val podiums: Int,
        val bestStreak: Int,
        val totalShared: Int,
    )

    private fun buildFlairs(
        isPro: Boolean,
        duoState: DuoStreakState?,
        challenges: List<Challenge>,
        userId: String,
        hasPerfectWeek: Boolean,
        groveLead: Boolean = false,
    ): List<ProfileFlair> {
        val out = mutableListOf<ProfileFlair>()
        if (isPro) out.add(ProfileFlair(FlairType.PRO, "Premium"))
        if ((duoState?.streakDays ?: 0) >= 3) {
            out.add(ProfileFlair(FlairType.DUO_FIRE, "${duoState!!.streakDays}d duo"))
        }
        val rankOne = challenges.filter { it.isActive && userId in it.members }
            .count { ch -> ch.leaderboard().firstOrNull()?.first == userId }
        if (rankOne > 0) {
            out.add(ProfileFlair(FlairType.CHALLENGE_LEADER, if (rankOne == 1) "#1" else "#1 ×$rankOne"))
        }
        if (hasPerfectWeek) out.add(ProfileFlair(FlairType.PERFECT_WEEK, "7-day perfect"))
        if (groveLead) out.add(ProfileFlair(FlairType.GROVE_LEAD, "Grove Lead"))
        return out
    }

    private fun peopleMotivatedToday(
        userId: String,
        friends: List<FriendSummary>,
        duoState: DuoStreakState?,
        challenges: List<Challenge>,
    ): List<MotivatedPerson> {
        val friendByUid = friends.associateBy { it.uid }
        val today = LocalDate.now().toString()
        val out = linkedMapOf<String, MotivatedPerson>()

        if (duoState != null && duoState.buddyDoneToday && duoState.buddyUid.isNotBlank()) {
            val buddy = friendByUid[duoState.buddyUid]
            out[duoState.buddyUid] = MotivatedPerson(
                uid = duoState.buddyUid,
                username = duoState.buddyUsername.ifBlank { buddy?.username }.orEmpty().ifBlank { "buddy" },
                photoUrl = duoState.buddyPhotoUrl ?: buddy?.photoUrl,
                reason = "Duo locked in today",
            )
        }

        challenges.filter { it.isActive && userId in it.members }.forEach { ch ->
            ch.members.filter { it != userId }.forEach { uid ->
                if (!ch.hasCompletedToday(uid, today)) return@forEach
                if (out.containsKey(uid)) return@forEach
                val friend = friendByUid[uid]
                val name = ch.memberNames[uid]?.ifBlank { null }
                    ?: friend?.username
                    ?: "rival"
                out[uid] = MotivatedPerson(
                    uid = uid,
                    username = name,
                    photoUrl = ch.memberPhotos[uid] ?: friend?.photoUrl,
                    reason = "Finished ${ch.habitName}",
                )
            }
        }
        return out.values.toList()
    }

    private fun buildActivityFeed(
        userId: String,
        userName: String,
        friends: List<FriendSummary>,
        duoState: DuoStreakState?,
        challenges: List<Challenge>,
    ): List<ProfileActivityItem> {
        val today = LocalDate.now().toString()
        val todayStart = LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val items = mutableListOf<ProfileActivityItem>()
        val friendNames = friends.associate { it.uid to it.username }

        challenges.filter { it.isActive && userId in it.members }.forEach { ch ->
            val rank = ch.rankOf(userId)
            if (rank in 2..5) {
                items.add(
                    ProfileActivityItem(
                        id = "rank_${ch.id}",
                        message = "You're #$rank in ${ch.habitName}",
                        timestamp = todayStart,
                        accent = ActivityAccent.RANK,
                    ),
                )
            } else if (rank == 1) {
                items.add(
                    ProfileActivityItem(
                        id = "lead_${ch.id}",
                        message = "Leading ${ch.habitName}. Defend your crown",
                        timestamp = todayStart,
                        accent = ActivityAccent.SUCCESS,
                    ),
                )
            }

            ch.members.filter { it != userId }.forEach { uid ->
                if (ch.hasCompletedToday(uid, today)) {
                    val name = ch.memberNames[uid]?.ifBlank { friendNames[uid] } ?: friendNames[uid] ?: "Rival"
                    items.add(
                        ProfileActivityItem(
                            id = "done_${ch.id}_$uid",
                            message = "@$name finished ${ch.habitName}",
                            timestamp = todayStart,
                            actorUsername = name,
                            accent = ActivityAccent.NEUTRAL,
                        ),
                    )
                }
            }
        }

        duoState?.let { duo ->
            if (duo.buddyDoneToday && !duo.myDoneToday) {
                val buddy = duo.buddyUsername.ifBlank { "Your buddy" }
                items.add(
                    ProfileActivityItem(
                        id = "duo_nudge_${duo.pairId}",
                        message = "@$buddy finished today. Your turn",
                        timestamp = todayStart,
                        actorUsername = buddy,
                        accent = ActivityAccent.DUO,
                    ),
                )
            } else if (duo.bothDoneToday) {
                items.add(
                    ProfileActivityItem(
                        id = "duo_both_${duo.pairId}",
                        message = "Duo streak ${duo.streakDays}d. Both locked in today",
                        timestamp = todayStart,
                        accent = ActivityAccent.DUO,
                    ),
                )
            }
        }

        return items
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
            .take(5)
    }

    private fun finalPlacement(challenge: Challenge, uid: String): Int? {
        val order = challenge.resultSnapshot?.finalLeaderboard
            ?.map { it.uid }
            ?.takeIf { it.isNotEmpty() }
            ?: challenge.finalStanding().map { it.first }
        val idx = order.indexOf(uid)
        return if (idx >= 0) idx + 1 else null
    }
}
