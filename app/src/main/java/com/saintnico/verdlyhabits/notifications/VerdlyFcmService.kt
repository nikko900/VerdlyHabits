package com.saintnico.verdlyhabits.notifications

import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class VerdlyFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_CHALLENGES = "verdly_challenges"
        const val CHANNEL_LEADERBOARD = "verdly_leaderboard"
        const val CHANNEL_SOCIAL = "verdly_social"
        const val CHANNEL_CONNECTIONS = "verdly_connections"
    }

    override fun onCreate() {
        super.onCreate()
        // Pre-create with defaults; showNotification() recreates with live prefs before each push.
        createChannels(vibrationEnabled = true, soundEnabled = true)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val notificationsEnabled = runCatching {
            runBlocking { ThemePreference(this@VerdlyFcmService).isNotificationsEnabled.first() }
        }.getOrDefault(false)
        if (!notificationsEnabled) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update(mapOf("fcmToken" to token, "notificationsEnabled" to true))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: message.notification?.title ?: "Verdly"
        val body = message.data["body"]
            ?: message.notification?.body
            ?: "Something happened in your challenge!"
        val type = message.data["type"] ?: "general"
        val challengeId = message.data["challengeId"].orEmpty()
        val route = message.data["route"].orEmpty()
        val actorUid = message.data["actorUid"].orEmpty()
        val actorName = message.data["actorUsername"].orEmpty()
        val actorPhotoUrl = message.data["actorPhotoUrl"]
        val referenceId = message.data["referenceId"].orEmpty()

        showNotification(
            title = title,
            body = body,
            type = type,
            challengeId = challengeId,
            route = route,
            actorUid = actorUid,
            actorName = actorName,
            actorPhotoUrl = actorPhotoUrl,
            referenceId = referenceId,
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        challengeId: String,
        route: String,
        actorUid: String,
        actorName: String,
        actorPhotoUrl: String?,
        referenceId: String,
    ) {
        val notificationsEnabled = runCatching {
            runBlocking { ThemePreference(this@VerdlyFcmService).isNotificationsEnabled.first() }
        }.getOrDefault(false)
        if (!notificationsEnabled) return
        if (isSocialType(type) && !socialNotificationsEnabled()) return

        val (vibrationEnabled, soundEnabled) = runCatching {
            runBlocking { NotificationHelper.readAlertPrefs(this@VerdlyFcmService) }
        }.getOrDefault(true to true)
        createChannels(vibrationEnabled, soundEnabled)

        val extras = NotificationHelper.extrasFor(type, challengeId, route).toMutableMap()
        extras["notification_type"] = type

        runBlocking {
            NotificationHelper.showPersonNotification(
                context = this@VerdlyFcmService,
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
                intentExtras = extras,
                vibrationEnabled = vibrationEnabled,
                soundEnabled = soundEnabled,
            )
        }
    }

    private fun channelNameFor(type: String): String = when (type) {
        "friend_request", "duo_invite", "arena_invite", "nudge", "profile_view", "duo_buddy_done" -> "Connections"
        "proof_posted", "reaction_received" -> "Proofs & Reactions"
        "took_lead", "overtaken", "rank_overtaken", "climbed", "lost_lead" -> "Leaderboard"
        else -> "Challenge Updates"
    }

    private fun socialNotificationsEnabled(): Boolean {
        return runCatching {
            runBlocking { ThemePreference(this@VerdlyFcmService).socialNotificationsEnabled.first() }
        }.getOrDefault(true)
    }

    private fun isSocialType(type: String): Boolean = type in setOf(
        "friend_request",
        "nudge",
        "profile_view",
        "proof_posted",
        "reaction_received",
    )

    private fun channelFor(type: String): String = when (type) {
        "took_lead", "overtaken", "rank_overtaken", "climbed", "rank_down", "lost_lead", "top_three" -> CHANNEL_LEADERBOARD
        "proof_posted", "reaction_received" -> CHANNEL_SOCIAL
        "friend_request", "duo_invite", "arena_invite", "duo_buddy_done", "duo_milestone", "nudge", "profile_view" -> CHANNEL_CONNECTIONS
        else -> CHANNEL_CHALLENGES
    }

    private fun createChannels(vibrationEnabled: Boolean, soundEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        NotificationHelper.ensureChannel(
            context = this,
            channelId = CHANNEL_CONNECTIONS,
            channelName = "Connections",
            importance = NotificationManager.IMPORTANCE_HIGH,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
            description = "Friend requests, duo invites, arena requests, and nudges",
        )
        NotificationHelper.ensureChannel(
            context = this,
            channelId = CHANNEL_LEADERBOARD,
            channelName = "Leaderboard",
            importance = NotificationManager.IMPORTANCE_HIGH,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
            description = "When you take the lead, climb, or get overtaken",
        )
        NotificationHelper.ensureChannel(
            context = this,
            channelId = CHANNEL_SOCIAL,
            channelName = "Proofs & Reactions",
            importance = NotificationManager.IMPORTANCE_HIGH,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
            description = "When rivals post proof or react to yours",
        )
        NotificationHelper.ensureChannel(
            context = this,
            channelId = CHANNEL_CHALLENGES,
            channelName = "Challenge Updates",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
            description = "General challenge updates and reminders",
        )
    }
}
