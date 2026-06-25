package com.saintnico.verdlyhabits.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ChallengeMode
import com.saintnico.verdlyhabits.data.model.ReactionWeights
import kotlinx.coroutines.tasks.await

/**
 * Queues push payloads for Cloud Functions ([notificationQueue]) and mirrors key events locally
 * when FCM is disabled on the device.
 */
object ChallengeNotificationDispatcher {
    private const val TAG = "ChallengeNotify"
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun queue(
        targetUid: String,
        type: String,
        title: String,
        body: String,
        challengeId: String,
        data: Map<String, String> = emptyMap(),
    ) {
        if (targetUid.isBlank()) return
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        try {
            firestore.collection("notificationQueue").add(
                mapOf(
                    "targetUid" to targetUid,
                    "actorUid" to actorUid,
                    "type" to type,
                    "title" to title,
                    "body" to body,
                    "challengeId" to challengeId,
                    "data" to data,
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "queue failed: ${e.message}")
        }
    }

    suspend fun notifyProofPosted(challenge: Challenge, posterUid: String, posterName: String) {
        challenge.members
            .filter { it != posterUid }
            .forEach { memberUid ->
                queue(
                    targetUid = memberUid,
                    type = "proof_posted",
                    title = challenge.habitName.ifBlank { "Challenge" },
                    body = "$posterName just posted — react with Fire",
                    challengeId = challenge.id,
                    data = mapOf("route" to "challenges", "challengeId" to challenge.id),
                )
            }
    }

    suspend fun notifyReactionReceived(
        challenge: Challenge,
        proofOwnerUid: String,
        reactorName: String,
        reactionKey: String,
        points: Int,
    ) {
        if (proofOwnerUid.isBlank() || proofOwnerUid == FirebaseAuth.getInstance().currentUser?.uid) return
        val rank = challenge.rankOf(proofOwnerUid)
        queue(
            targetUid = proofOwnerUid,
            type = "reaction_received",
            title = challenge.habitName.ifBlank { "Challenge" },
            body = "$reactorName gave you Fire +$points — you're now #$rank",
            challengeId = challenge.id,
            data = mapOf(
                "route" to "challenges",
                "challengeId" to challenge.id,
                "proofKey" to reactionKey,
                "points" to points.toString(),
            ),
        )
    }

    suspend fun notifyRankChange(
        challenge: Challenge,
        memberUid: String,
        oldRank: Int,
        newRank: Int,
    ) {
        if (memberUid.isBlank() || oldRank == newRank) return
        val name = challenge.memberNames[memberUid] ?: "Rival"
        when {
            newRank in 1..3 && oldRank > 3 -> {
                queue(
                    targetUid = memberUid,
                    type = "top_three",
                    title = challenge.habitName.ifBlank { "Challenge" },
                    body = "You just entered the top 3 — you're #$newRank",
                    challengeId = challenge.id,
                )
            }
            newRank > oldRank && oldRank > 0 -> {
                queue(
                    targetUid = memberUid,
                    type = "rank_down",
                    title = challenge.habitName.ifBlank { "Challenge" },
                    body = "You dropped to #$newRank — post your proof to fight back",
                    challengeId = challenge.id,
                )
            }
            newRank < oldRank -> {
                val leaderScore = challenge.leaderboard().firstOrNull()?.second ?: 0
                val myScore = challenge.scoreFor(memberUid)
                val gap = (leaderScore - myScore).coerceAtLeast(0)
                if (gap in 1..5 && challenge.challengeMode != ChallengeMode.STREAK) {
                    queue(
                        targetUid = memberUid,
                        type = "within_striking_distance",
                        title = challenge.habitName.ifBlank { "Challenge" },
                        body = "You're $gap pts from #1 — one more cheer could do it",
                        challengeId = challenge.id,
                    )
                }
            }
        }
    }

    suspend fun notifyFinalDayStandings(challenge: Challenge) {
        val board = challenge.leaderboard()
        challenge.members.forEach { uid ->
            val rank = board.indexOfFirst { it.first == uid } + 1
            if (rank <= 0) return@forEach
            val leaderPts = board.firstOrNull()?.second ?: 0
            val myPts = challenge.scoreFor(uid)
            val gap = (leaderPts - myPts).coerceAtLeast(0)
            queue(
                targetUid = uid,
                type = "final_day_standings",
                title = challenge.habitName.ifBlank { "Challenge" },
                body = "Final day standings — you're $gap pts behind #1",
                challengeId = challenge.id,
            )
        }
    }

    fun reactionLabel(key: String): String = when (key) {
        ReactionWeights.FIRE -> "Fire"
        ReactionWeights.HEART -> "Heart"
        ReactionWeights.LIGHTNING -> "Lightning"
        else -> "React"
    }
}
