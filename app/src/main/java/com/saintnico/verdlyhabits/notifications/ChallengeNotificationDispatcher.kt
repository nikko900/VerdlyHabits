package com.saintnico.verdlyhabits.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ChallengeMode
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.model.ReactionWeights
import com.saintnico.verdlyhabits.data.remote.firestore.InboxRepository
import kotlinx.coroutines.tasks.await

/**
 * Client-side activity notifications: writes the recipient's Firestore inbox (always)
 * and queues FCM payloads for [notificationQueue] when Cloud Functions are available.
 */
object ChallengeNotificationDispatcher {
    private const val TAG = "ChallengeNotify"
    private val firestore = FirebaseFirestore.getInstance()
    private val inbox = InboxRepository()

    private fun arenaData(challengeId: String, referenceId: String, extra: Map<String, String> = emptyMap()) =
        buildMap {
            put("route", "arena")
            put("challengeId", challengeId)
            put("referenceId", referenceId)
            putAll(extra)
        }

    /** Core delivery — inbox is the source of truth for in-app + local push bridge. */
    private suspend fun deliver(
        targetUid: String,
        referenceId: String,
        title: String,
        body: String,
        pushType: String,
        challengeId: String,
        actorUsername: String = "",
        actorPhotoUrl: String? = null,
        extraPushData: Map<String, String> = emptyMap(),
        allowSelfTarget: Boolean = false,
    ) {
        if (targetUid.isBlank() || referenceId.isBlank()) return
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (actorUid.isBlank()) return
        if (!allowSelfTarget && targetUid == actorUid) return

        val write = inbox.writeNotification(
            targetUid = targetUid,
            notification = InboxNotification(
                id = referenceId,
                type = InboxNotificationType.CHALLENGE_UPDATE,
                title = title.ifBlank { "Challenge" },
                body = body,
                actorUid = actorUid,
                actorUsername = actorUsername,
                actorPhotoUrl = actorPhotoUrl,
                referenceId = referenceId,
                challengeId = challengeId.takeIf { it.isNotBlank() },
                route = "arena",
                actionState = "none",
            ),
        )
        if (write.isFailure) {
            Log.w(TAG, "inbox write failed ($pushType): ${write.exceptionOrNull()?.message}")
        }
        queue(
            targetUid = targetUid,
            type = pushType,
            title = title.ifBlank { "Challenge" },
            body = body,
            challengeId = challengeId,
            data = arenaData(challengeId, referenceId, extraPushData),
            actorUsername = actorUsername,
            actorPhotoUrl = actorPhotoUrl,
        )
    }

    suspend fun queue(
        targetUid: String,
        type: String,
        title: String,
        body: String,
        challengeId: String,
        data: Map<String, String> = emptyMap(),
        actorUsername: String = "",
        actorPhotoUrl: String? = null,
    ) {
        if (targetUid.isBlank()) return
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        try {
            firestore.collection("notificationQueue").add(
                buildMap {
                    put("targetUid", targetUid)
                    put("actorUid", actorUid)
                    put("type", type)
                    put("title", title)
                    put("body", body)
                    put("challengeId", challengeId)
                    put("data", data)
                    put("createdAt", System.currentTimeMillis())
                    if (actorUsername.isNotBlank()) put("actorUsername", actorUsername)
                    if (!actorPhotoUrl.isNullOrBlank()) put("actorPhotoUrl", actorPhotoUrl)
                },
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "queue failed: ${e.message}")
        }
    }

    suspend fun notifyOvertaken(
        targetUid: String,
        overtakerUsername: String,
        overtakerPhotoUrl: String?,
        challengeId: String,
        challengeTitle: String,
        victimNewRank: Int,
        eventDate: String,
    ) {
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val name = overtakerUsername.ifBlank { "A rival" }
        val title = challengeTitle.ifBlank { "Challenge" }
        val body = "@$name overtook you. You're #$victimNewRank now. Reclaim your spot!"
        val referenceId = "overtake_${challengeId}_${actorUid}_${targetUid}_$eventDate"
        deliver(
            targetUid = targetUid,
            referenceId = referenceId,
            title = title,
            body = body,
            pushType = "rank_overtaken",
            challengeId = challengeId,
            actorUsername = overtakerUsername,
            actorPhotoUrl = overtakerPhotoUrl,
        )
    }

    suspend fun notifyProofPosted(
        memberUids: List<String>,
        posterUid: String,
        posterName: String,
        posterPhotoUrl: String?,
        challengeId: String,
        challengeTitle: String,
        eventDate: String,
    ) {
        val title = challengeTitle.ifBlank { "Challenge" }
        val body = "${posterName.ifBlank { "A rival" }} just posted. React with Fire"
        val referenceId = "proof_${challengeId}_${posterUid}_$eventDate"
        memberUids.filter { it.isNotBlank() && it != posterUid }.forEach { memberUid ->
            deliver(
                targetUid = memberUid,
                referenceId = "${referenceId}_$memberUid",
                title = title,
                body = body,
                pushType = "proof_posted",
                challengeId = challengeId,
                actorUsername = posterName,
                actorPhotoUrl = posterPhotoUrl,
            )
        }
    }

    suspend fun notifyReactionReceived(
        proofOwnerUid: String,
        reactorName: String,
        reactorPhotoUrl: String?,
        challengeId: String,
        challengeTitle: String,
        proofKey: String,
        reactionEmoji: String,
        points: Int,
        ownerRank: Int,
        eventDate: String,
    ) {
        if (proofOwnerUid.isBlank()) return
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (proofOwnerUid == actorUid) return
        val label = reactionLabel(reactionEmoji)
        val title = challengeTitle.ifBlank { "Challenge" }
        val rankSuffix = if (ownerRank > 0) ". You're now #$ownerRank" else ""
        val body = "${reactorName.ifBlank { "Someone" }} gave you $label +$points$rankSuffix"
        val referenceId = "reaction_${challengeId}_${actorUid}_${proofKey}_$eventDate"
        deliver(
            targetUid = proofOwnerUid,
            referenceId = referenceId,
            title = title,
            body = body,
            pushType = "reaction_received",
            challengeId = challengeId,
            actorUsername = reactorName,
            actorPhotoUrl = reactorPhotoUrl,
            extraPushData = mapOf("proofKey" to proofKey, "points" to points.toString()),
        )
    }

    /** Rank movement for the member whose score changed (check-in or reaction). */
    suspend fun notifySelfRankChange(
        targetUid: String,
        challengeId: String,
        challengeTitle: String,
        oldRank: Int,
        newRank: Int,
        eventDate: String,
    ) {
        if (targetUid.isBlank() || oldRank == newRank || oldRank <= 0 || newRank <= 0) return
        val title = challengeTitle.ifBlank { "Challenge" }
        val referenceId = "self_rank_${challengeId}_${targetUid}_${oldRank}_${newRank}_$eventDate"
        val (body, pushType) = when {
            newRank == 1 && oldRank > 1 ->
                "You took the lead. You're #1. Defend it." to "took_lead"
            newRank in 1..3 && oldRank > 3 ->
                "You just entered the top 3. You're #$newRank" to "top_three"
            newRank < oldRank ->
                "You climbed to #$newRank. Keep the momentum." to "climbed"
            else ->
                "You dropped to #$newRank. Post your proof to fight back" to "rank_down"
        }
        deliver(
            targetUid = targetUid,
            referenceId = referenceId,
            title = title,
            body = body,
            pushType = pushType,
            challengeId = challengeId,
            allowSelfTarget = true,
        )
    }

    /** Previous #1 when someone else takes the lead. */
    suspend fun notifyLostLead(
        targetUid: String,
        newLeaderName: String,
        newLeaderPhotoUrl: String?,
        challengeId: String,
        challengeTitle: String,
        eventDate: String,
    ) {
        val name = newLeaderName.ifBlank { "A rival" }
        val title = challengeTitle.ifBlank { "Challenge" }
        val body = "@$name just took the lead. Reclaim #1!"
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val referenceId = "lost_lead_${challengeId}_${actorUid}_${targetUid}_$eventDate"
        deliver(
            targetUid = targetUid,
            referenceId = referenceId,
            title = title,
            body = body,
            pushType = "lost_lead",
            challengeId = challengeId,
            actorUsername = newLeaderName,
            actorPhotoUrl = newLeaderPhotoUrl,
        )
    }

    suspend fun notifyWithinStrikingDistance(
        targetUid: String,
        challenge: Challenge,
        gap: Int,
        eventDate: String,
    ) {
        if (gap !in 1..5 || challenge.challengeMode == ChallengeMode.STREAK) return
        val referenceId = "strike_${challenge.id}_${targetUid}_$eventDate"
        deliver(
            targetUid = targetUid,
            referenceId = referenceId,
            title = challenge.habitName.ifBlank { "Challenge" },
            body = "You're $gap pts from #1. One more cheer could do it",
            pushType = "within_striking_distance",
            challengeId = challenge.id,
            allowSelfTarget = true,
        )
    }

    suspend fun notifyCoinGift(
        targetUid: String,
        senderName: String,
        amount: Int,
        challengeId: String,
        challengeTitle: String,
        giftId: String,
    ) {
        val title = challengeTitle.ifBlank { "Challenge" }
        val body = "$senderName sent you $amount coins to keep going."
        deliver(
            targetUid = targetUid,
            referenceId = "gift_$giftId",
            title = title,
            body = body,
            pushType = "coin_gift",
            challengeId = challengeId,
            actorUsername = senderName,
        )
    }

    // Legacy helpers — delegate to deliver where possible
    suspend fun notifyProofPosted(challenge: Challenge, posterUid: String, posterName: String) {
        val date = java.time.LocalDate.now().toString()
        notifyProofPosted(
            memberUids = challenge.members,
            posterUid = posterUid,
            posterName = posterName,
            posterPhotoUrl = null,
            challengeId = challenge.id,
            challengeTitle = challenge.habitName,
            eventDate = date,
        )
    }

    suspend fun notifyReactionReceived(
        challenge: Challenge,
        proofOwnerUid: String,
        reactorName: String,
        reactionKey: String,
        points: Int,
    ) {
        val rank = challenge.rankOf(proofOwnerUid)
        val date = proofDateFromKey(reactionKey).ifBlank { java.time.LocalDate.now().toString() }
        notifyReactionReceived(
            proofOwnerUid = proofOwnerUid,
            reactorName = reactorName,
            reactorPhotoUrl = null,
            challengeId = challenge.id,
            challengeTitle = challenge.habitName,
            proofKey = reactionKey,
            reactionEmoji = ReactionWeights.FIRE,
            points = points,
            ownerRank = rank,
            eventDate = date,
        )
    }

    suspend fun notifyRankChange(
        challenge: Challenge,
        memberUid: String,
        oldRank: Int,
        newRank: Int,
    ) {
        val date = java.time.LocalDate.now().toString()
        notifySelfRankChange(
            targetUid = memberUid,
            challengeId = challenge.id,
            challengeTitle = challenge.habitName,
            oldRank = oldRank,
            newRank = newRank,
            eventDate = date,
        )
        if (newRank < oldRank) {
            val leaderScore = challenge.leaderboard().firstOrNull()?.second ?: 0
            val myScore = challenge.scoreFor(memberUid)
            val gap = (leaderScore - myScore).coerceAtLeast(0)
            notifyWithinStrikingDistance(memberUid, challenge, gap, date)
        }
    }

    suspend fun notifyFinalDayStandings(challenge: Challenge) {
        val board = challenge.leaderboard()
        val date = java.time.LocalDate.now().toString()
        challenge.members.forEach { uid ->
            val rank = board.indexOfFirst { it.first == uid } + 1
            if (rank <= 0) return@forEach
            val leaderPts = board.firstOrNull()?.second ?: 0
            val myPts = challenge.scoreFor(uid)
            val gap = (leaderPts - myPts).coerceAtLeast(0)
            deliver(
                targetUid = uid,
                referenceId = "final_${challenge.id}_${uid}_$date",
                title = challenge.habitName.ifBlank { "Challenge" },
                body = "Final day standings. You're $gap pts behind #1",
                pushType = "final_day_standings",
                challengeId = challenge.id,
                allowSelfTarget = true,
            )
        }
    }

    fun reactionLabel(key: String): String = when (key) {
        ReactionWeights.FIRE, ReactionWeights.AWESOME, ReactionWeights.YASS, ReactionWeights.DONE -> "Fire"
        ReactionWeights.HEART, ReactionWeights.HEART_PINK, ReactionWeights.HEART_EYES,
        ReactionWeights.HEART_EYES2, ReactionWeights.THANKS, ReactionWeights.APPRECIATE,
        ReactionWeights.APPRECIATE2, ReactionWeights.PLEASE -> "Heart"
        ReactionWeights.LIGHTNING, ReactionWeights.WOW, ReactionWeights.OMG -> "Wow"
        ReactionWeights.SAD, ReactionWeights.SAD_EYES -> "Sad"
        ReactionWeights.ANGRY -> "Angry"
        ReactionWeights.SUSPICIOUS -> "Side-eye"
        ReactionWeights.OK -> "OK"
        else -> "React"
    }

    private fun proofDateFromKey(proofKey: String): String {
        val parts = proofKey.split("_")
        return parts.getOrNull(1).orEmpty()
    }
}
