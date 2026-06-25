package com.saintnico.verdlyhabits.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRepository
import com.saintnico.verdlyhabits.data.remote.firestore.InboxRepository
import kotlinx.coroutines.tasks.await

/**
 * Delivers social notifications to a user's inbox and queues FCM via [notificationQueue].
 */
object SocialNotificationDispatcher {
    private const val TAG = "SocialNotify"
    private val firestore = FirebaseFirestore.getInstance()
    private val inbox = InboxRepository()

    suspend fun notifyFriendRequest(
        targetUid: String,
        fromUsername: String,
        fromPhotoUrl: String?,
        requestId: String,
    ) {
        val body = "@${fromUsername.ifBlank { "rival" }} wants to connect on Verdly"
        deliver(
            targetUid = targetUid,
            type = InboxNotificationType.FRIEND_REQUEST,
            title = "New friend request",
            body = body,
            actorUsername = fromUsername,
            actorPhotoUrl = fromPhotoUrl,
            referenceId = requestId,
            pushType = "friend_request",
            pushData = mapOf("route" to "notifications", "referenceId" to requestId),
        )
    }

    suspend fun notifyDuoInvite(
        targetUid: String,
        fromUsername: String,
        fromPhotoUrl: String?,
        pairId: String,
    ) {
        val body = "@${fromUsername.ifBlank { "rival" }} invited you to a duo streak"
        deliver(
            targetUid = targetUid,
            type = InboxNotificationType.DUO_INVITE,
            title = "Accountability buddy invite",
            body = body,
            actorUsername = fromUsername,
            actorPhotoUrl = fromPhotoUrl,
            referenceId = pairId,
            pushType = "duo_invite",
            pushData = mapOf("route" to "notifications", "referenceId" to pairId),
        )
    }

    suspend fun notifyArenaInvite(
        targetUid: String,
        fromUsername: String,
        challengeTitle: String,
        challengeId: String,
        requestId: String,
    ) {
        val body = "@${fromUsername.ifBlank { "rival" }} wants to join \"$challengeTitle\""
        deliver(
            targetUid = targetUid,
            type = InboxNotificationType.ARENA_INVITE,
            title = "Arena join request",
            body = body,
            actorUsername = fromUsername,
            actorPhotoUrl = null,
            referenceId = requestId,
            challengeId = challengeId,
            pushType = "arena_invite",
            pushData = mapOf(
                "route" to "notifications",
                "referenceId" to requestId,
                "challengeId" to challengeId,
            ),
        )
    }

    private suspend fun deliver(
        targetUid: String,
        type: InboxNotificationType,
        title: String,
        body: String,
        actorUsername: String,
        actorPhotoUrl: String?,
        referenceId: String,
        challengeId: String? = null,
        pushType: String,
        pushData: Map<String, String>,
    ) {
        if (targetUid.isBlank()) return
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        inbox.writeNotification(
            targetUid = targetUid,
            notification = InboxNotification(
                id = referenceId,
                type = type,
                title = title,
                body = body,
                actorUid = actorUid,
                actorUsername = actorUsername,
                actorPhotoUrl = actorPhotoUrl,
                referenceId = referenceId,
                challengeId = challengeId,
                actionState = "pending",
            ),
        )
        queuePush(targetUid, pushType, title, body, challengeId.orEmpty(), pushData)
    }

    private suspend fun queuePush(
        targetUid: String,
        type: String,
        title: String,
        body: String,
        challengeId: String,
        data: Map<String, String>,
    ) {
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
            Log.w(TAG, "queuePush failed: ${e.message}")
        }
    }

    fun friendRequestId(fromUid: String, toUid: String) =
        FriendRepository.requestDocId(fromUid, toUid)
}
