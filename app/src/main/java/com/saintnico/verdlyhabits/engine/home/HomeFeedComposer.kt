package com.saintnico.verdlyhabits.engine.home

import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.ui.components.reactions.proofReactionKey
import com.saintnico.verdlyhabits.ui.components.reactions.reactionCountsFor
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.engine.grove.GroveCalendar
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember
import com.saintnico.verdlyhabits.ui.viewmodel.GrovePulse
import java.time.LocalDate

object HomeFeedComposer {

    fun compose(
        meUid: String,
        members: List<GroveMember>,
        pulse: GrovePulse,
        duo: DuoStreakState?,
        challenges: List<Challenge>,
        blockedUids: Set<String>,
        quote: String?,
        today: LocalDate = LocalDate.now(),
    ): List<HomeFeedPost> {
        val todayKey = GroveCalendar.todayKey(today)
        val todayMillis = localDayStartMillis(today)
        val candidates = mutableListOf<HomeFeedPost>()
        val friends = members.filter { !it.isSelf && it.uid.isNotBlank() && it.uid !in blockedUids }

        duo?.takeIf { it.status == "active" && it.buddyUid.isNotBlank() && it.buddyUid !in blockedUids }?.let { d ->
            if (d.buddyDoneToday || d.bothDoneToday || d.streakAtRisk) {
                candidates += HomeFeedPost.DuoMoment(
                    id = "duo_${d.pairId}",
                    actorUid = d.buddyUid,
                    eventAtMillis = todayMillis,
                    username = d.buddyUsername.ifBlank { "buddy" },
                    photoUrl = d.buddyPhotoUrl,
                    streakDays = d.streakDays,
                    bothLocked = d.bothDoneToday,
                    atRisk = d.streakAtRisk,
                    buddyDone = d.buddyDoneToday,
                    myDone = d.myDoneToday,
                )
            }
        }

        friends.filter { it.plantedToday }.forEach { m ->
            candidates += HomeFeedPost.Planted(
                id = "planted_${m.uid}",
                actorUid = m.uid,
                eventAtMillis = parseDayStartMillis(todayKey),
                username = m.username,
                photoUrl = m.photoUrl,
                streakDays = m.currentStreakPublic,
                witnessCount = m.witnessCount,
                titleGlowId = m.titleGlowId,
                profileFrameId = m.profileFrameId,
                sharedHabit = m.sharedHabitTitle,
            )
        }

        val proofKeys = mutableSetOf<String>()
        challenges.filter { it.isActive && meUid in it.members }.forEach { ch ->
            ch.members.filter { it != meUid && it !in blockedUids }.forEach { uid ->
                val proof = ch.proofPhotos[uid]?.get(todayKey)
                if (!proof.isNullOrBlank()) {
                    val key = "proof_${ch.id}_$uid"
                    proofKeys += "${ch.id}_$uid"
                    val name = ch.memberNames[uid]?.ifBlank { null }
                        ?: friends.firstOrNull { it.uid == uid }?.username
                        ?: "rival"
                    val proofKey = proofReactionKey(uid, todayKey)
                    candidates += HomeFeedPost.ProofPhoto(
                        id = key,
                        actorUid = uid,
                        eventAtMillis = todayMillis,
                        username = name,
                        photoUrl = ch.memberPhotos[uid],
                        habitName = ch.habitName,
                        proofUrl = proof,
                        challengeId = ch.id,
                        streakDays = ch.streakFor(uid),
                        reactionCounts = reactionCountsFor(ch, proofKey),
                    )
                } else if (ch.hasCompletedToday(uid, todayKey)) {
                    val name = ch.memberNames[uid]?.ifBlank { null }
                        ?: friends.firstOrNull { it.uid == uid }?.username
                        ?: "rival"
                    candidates += HomeFeedPost.ChallengeDone(
                        id = "chdone_${ch.id}_$uid",
                        actorUid = uid,
                        eventAtMillis = todayMillis,
                        username = name,
                        photoUrl = ch.memberPhotos[uid],
                        habitName = ch.habitName,
                        challengeId = ch.id,
                    )
                }
            }
        }

        friends.firstOrNull { it.groveLead }?.let { lead ->
            candidates += HomeFeedPost.GroveLead(
                id = "lead_${lead.uid}",
                actorUid = lead.uid,
                eventAtMillis = todayMillis,
                username = lead.username,
                photoUrl = lead.photoUrl,
            )
        }

        if (pulse.friendCount > 0 && pulse.plantedFriends.isEmpty()) {
            candidates += HomeFeedPost.GroveWaking()
        }

        if (!quote.isNullOrBlank()) {
            candidates += HomeFeedPost.Quote(id = "quote_$todayKey", text = quote)
        }

        return HomeFeedRanker.rank(candidates)
    }
}
