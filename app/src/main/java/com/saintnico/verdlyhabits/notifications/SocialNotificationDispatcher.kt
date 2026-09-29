package com.saintnico.verdlyhabits.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.model.NudgeSurface
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
        fromPhotoUrl: String? = null,
    ) {
        val body = "@${fromUsername.ifBlank { "rival" }} wants to join \"$challengeTitle\""
        deliver(
            targetUid = targetUid,
            type = InboxNotificationType.ARENA_INVITE,
            title = "Arena join request",
            body = body,
            actorUsername = fromUsername,
            actorPhotoUrl = fromPhotoUrl,
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

    /**
     * Sends a "nudge" — a friend/duo-buddy/rival poking this user with a chosen message.
     * Writes an inbox card (informational only, no accept/decline) and queues a push.
     * Caller is responsible for its own cooldown check ([NudgeRepository]) so this never
     * fires more often than the UI already allows.
     */
    suspend fun notifyNudge(
        targetUid: String,
        fromUsername: String,
        fromPhotoUrl: String?,
        message: String,
        surface: NudgeSurface,
        contextId: String? = null,
    ) {
        if (targetUid.isBlank()) return
        val name = fromUsername.ifBlank { "Someone" }
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val referenceId = "nudge_${actorUid}_${System.currentTimeMillis()}"
        val route = when (surface) {
            NudgeSurface.DUO -> "arena"
            NudgeSurface.GROVE -> "friends_grove"
            else -> "notifications"
        }

        val write = inbox.writeNotification(
            targetUid = targetUid,
            notification = InboxNotification(
                id = referenceId,
                type = InboxNotificationType.NUDGE,
                title = "@$name nudged you",
                body = message,
                actorUid = actorUid,
                actorUsername = fromUsername,
                actorPhotoUrl = fromPhotoUrl,
                referenceId = referenceId,
                challengeId = contextId?.takeIf { surface == NudgeSurface.CHALLENGE },
                route = route,
                actionState = "none",
            ),
        )
        if (write.isFailure) {
            Log.w(TAG, "inbox write failed (NUDGE): ${write.exceptionOrNull()?.message}")
        }
        queuePush(
            targetUid = targetUid,
            type = "nudge",
            title = "@$name nudged you",
            body = message,
            challengeId = contextId?.takeIf { surface == NudgeSurface.CHALLENGE }.orEmpty(),
            data = buildMap {
                put("route", route)
                put("referenceId", referenceId)
            },
            actorUsername = fromUsername,
            actorPhotoUrl = fromPhotoUrl,
        )
    }

    suspend fun notifyGroveOvertake(
        targetUid: String,
        fromUsername: String,
        weeklyDays: Int,
        fromPhotoUrl: String? = null,
    ) {
        if (targetUid.isBlank()) return
        val name = fromUsername.ifBlank { "A friend" }
        val day = java.time.LocalDate.now().toString()
        val referenceId = "grove_pass_${FirebaseAuth.getInstance().currentUser?.uid}_$day"
        val body = "@$name showed up $weeklyDays day${if (weeklyDays == 1) "" else "s"} this week. They passed you in the grove."
        val write = inbox.writeNotification(
            targetUid = targetUid,
            notification = InboxNotification(
                id = referenceId,
                type = InboxNotificationType.SYSTEM,
                title = "They passed you",
                body = body,
                actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                actorUsername = fromUsername,
                actorPhotoUrl = fromPhotoUrl,
                referenceId = referenceId,
                route = "friends_grove",
                actionState = "none",
            ),
        )
        if (write.isFailure) {
            Log.w(TAG, "inbox write failed (GROVE_OVERTAKE): ${write.exceptionOrNull()?.message}")
        }
        queuePush(
            targetUid = targetUid,
            type = "overtaken",
            title = "They passed you",
            body = body,
            challengeId = "",
            data = mapOf("route" to "friends_grove", "referenceId" to referenceId),
            actorUsername = fromUsername,
            actorPhotoUrl = fromPhotoUrl,
        )
    }

    suspend fun notifyDuoBuddyDone(
        targetUid: String,
        fromUsername: String,
        pairId: String,
        streakDays: Int,
        fromPhotoUrl: String? = null,
    ) {
        val name = fromUsername.ifBlank { "Your buddy" }
        val body = "$name finished today. Don't break the ${streakDays.coerceAtLeast(1)}-day duo streak!"
        val referenceId = "duo_done_${pairId}_${System.currentTimeMillis()}"
        val write = inbox.writeNotification(
            targetUid = targetUid,
            notification = InboxNotification(
                id = referenceId,
                type = InboxNotificationType.NUDGE,
                title = "Duo streak. Your turn",
                body = body,
                actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                actorUsername = fromUsername,
                actorPhotoUrl = fromPhotoUrl,
                referenceId = referenceId,
                route = "arena",
                actionState = "none",
            ),
        )
        if (write.isFailure) {
            Log.w(TAG, "inbox write failed (duo_buddy_done): ${write.exceptionOrNull()?.message}")
        }
        queuePush(
            targetUid = targetUid,
            type = "duo_buddy_done",
            title = "Duo streak. Your turn",
            body = body,
            challengeId = "",
            data = mapOf("route" to "arena", "referenceId" to referenceId),
            actorUsername = fromUsername,
            actorPhotoUrl = fromPhotoUrl,
        )
    }

    suspend fun notifyProfileView(
        targetUid: String,
        fromUsername: String,
        fromPhotoUrl: String?,
    ): Boolean {
        if (targetUid.isBlank()) return false
        val handle = fromUsername.trim().removePrefix("@").ifBlank { "Someone" }
        val title = "Viewed your profile"
        val body = if (handle.equals("Someone", ignoreCase = true)) {
            "Someone opened your profile card"
        } else {
            "@$handle opened your profile card"
        }
        val actorUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val write = inbox.writeNotification(
            targetUid = targetUid,
            notification = InboxNotification(
                id = "",
                type = InboxNotificationType.PROFILE_VIEW,
                title = title,
                body = body,
                actorUid = actorUid,
                actorUsername = handle,
                actorPhotoUrl = fromPhotoUrl,
                referenceId = "",
                route = "profile",
                actionState = "none",
            ),
        )
        if (write.isFailure) {
            Log.w(TAG, "inbox write failed (PROFILE_VIEW): ${write.exceptionOrNull()?.message}")
            return false
        }
        queuePush(
            targetUid = targetUid,
            type = "profile_view",
            title = title,
            body = body,
            challengeId = "",
            data = mapOf("route" to "notifications"),
            actorUsername = handle,
            actorPhotoUrl = fromPhotoUrl,
        )
        return true
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
        val write = inbox.writeNotification(
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
                route = pushData["route"],
                actionState = "pending",
            ),
        )
        if (write.isFailure) {
            Log.w(TAG, "inbox write failed (${type.name}): ${write.exceptionOrNull()?.message}")
        }
        queuePush(
            targetUid = targetUid,
            type = pushType,
            title = title,
            body = body,
            challengeId = challengeId.orEmpty(),
            data = pushData,
            actorUsername = actorUsername,
            actorPhotoUrl = actorPhotoUrl,
        )
    }

    private suspend fun queuePush(
        targetUid: String,
        type: String,
        title: String,
        body: String,
        challengeId: String,
        data: Map<String, String>,
        actorUsername: String = "",
        actorPhotoUrl: String? = null,
    ) {
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
            Log.w(TAG, "queuePush failed: ${e.message}")
        }
    }

    fun friendRequestId(fromUid: String, toUid: String) =
        FriendRepository.requestDocId(fromUid, toUid)
}
