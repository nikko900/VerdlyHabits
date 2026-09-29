package com.saintnico.verdlyhabits.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Shows device notifications when new inbox items arrive — works without Cloud Functions / FCM.
 * FCM still preferred when available; this is the offline fallback.
 */
object InboxLocalNotifier {
    private const val TAG = "InboxLocalPush"
    private val firestore = FirebaseFirestore.getInstance()
    private val seenIds = ConcurrentHashMap.newKeySet<String>()
    private var registration: ListenerRegistration? = null
    private var primed = false
    private var activeUid: String? = null

    fun start(context: Context, uid: String) {
        if (uid.isBlank()) {
            stop()
            return
        }
        if (uid == activeUid && registration != null) return
        stop()
        activeUid = uid
        primed = false
        registration = firestore.collection("users").document(uid).collection("inbox")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "listener: ${err.message}")
                    return@addSnapshotListener
                }
                val docs = snap?.documentChanges.orEmpty()
                if (!primed) {
                    snap?.documents?.forEach { seenIds.add(it.id) }
                    primed = true
                    return@addSnapshotListener
                }
                docs.filter { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val doc = change.document
                        if (!seenIds.add(doc.id)) return@forEach
                        val read = doc.getBoolean("read") ?: false
                        if (read) return@forEach
                        val title = doc.getString("title").orEmpty().ifBlank { "Verdly" }
                        val body = doc.getString("body").orEmpty().ifBlank { "New activity" }
                        val typeName = doc.getString("type").orEmpty()
                        val challengeId = doc.getString("challengeId").orEmpty()
                        val route = doc.getString("route").orEmpty()
                        val actorUid = doc.getString("actorUid").orEmpty()
                        val actorName = doc.getString("actorUsername").orEmpty()
                        val actorPhotoUrl = doc.getString("actorPhotoUrl")
                        val referenceId = doc.getString("referenceId").orEmpty().ifBlank { doc.id }
                        val pushType = pushTypeFor(typeName, body)
                        showLocal(
                            context = context.applicationContext,
                            title = title,
                            body = body,
                            type = pushType,
                            challengeId = challengeId,
                            route = route,
                            actorUid = actorUid,
                            actorName = actorName,
                            actorPhotoUrl = actorPhotoUrl,
                            referenceId = referenceId,
                            inboxId = doc.id,
                        )
                    }
            }
    }

    fun stop() {
        registration?.remove()
        registration = null
        activeUid = null
        primed = false
        seenIds.clear()
    }

    private fun pushTypeFor(typeName: String, body: String): String = when {
        typeName == InboxNotificationType.NUDGE.name -> "nudge"
        typeName == InboxNotificationType.FRIEND_REQUEST.name -> "friend_request"
        typeName == InboxNotificationType.DUO_INVITE.name -> "duo_invite"
        typeName == InboxNotificationType.ARENA_INVITE.name -> "arena_invite"
        typeName == InboxNotificationType.PROFILE_VIEW.name -> "profile_view"
        body.contains("overtook", ignoreCase = true) -> "rank_overtaken"
        body.contains("passed you", ignoreCase = true) -> "overtaken"
        body.contains("took the lead", ignoreCase = true) -> "took_lead"
        body.contains("react", ignoreCase = true) -> "reaction_received"
        body.contains("posted", ignoreCase = true) -> "proof_posted"
        body.contains("climbed", ignoreCase = true) -> "climbed"
        else -> "challenge_update"
    }

    private fun showLocal(
        context: Context,
        title: String,
        body: String,
        type: String,
        challengeId: String,
        route: String,
        actorUid: String,
        actorName: String,
        actorPhotoUrl: String?,
        referenceId: String,
        inboxId: String,
    ) {
        val extras = NotificationHelper.extrasFor(type, challengeId, route)
        CoroutineScope(Dispatchers.IO).launch {
            NotificationHelper.showPersonNotification(
                context = context,
                channelId = channelFor(type),
                channelName = channelNameFor(type),
                title = title,
                body = body,
                type = type,
                actorUid = actorUid,
                actorName = actorName,
                actorPhotoUrl = actorPhotoUrl,
                referenceId = referenceId,
                challengeId = challengeId,
                inboxId = inboxId,
                intentExtras = extras,
            )
        }
    }

    private fun channelFor(type: String): String = when (type) {
        "took_lead", "rank_overtaken", "overtaken", "climbed", "rank_down", "lost_lead", "top_three" ->
            VerdlyFcmService.CHANNEL_LEADERBOARD
        "proof_posted", "reaction_received" -> VerdlyFcmService.CHANNEL_SOCIAL
        "friend_request", "duo_invite", "arena_invite", "duo_buddy_done", "nudge", "profile_view" ->
            VerdlyFcmService.CHANNEL_CONNECTIONS
        else -> VerdlyFcmService.CHANNEL_CHALLENGES
    }

    private fun channelNameFor(type: String): String = when (type) {
        "friend_request", "duo_invite", "arena_invite", "nudge", "profile_view" -> "Connections"
        "proof_posted", "reaction_received" -> "Proofs & Reactions"
        "took_lead", "rank_overtaken", "overtaken", "climbed" -> "Leaderboard"
        else -> "Challenge Updates"
    }
}
