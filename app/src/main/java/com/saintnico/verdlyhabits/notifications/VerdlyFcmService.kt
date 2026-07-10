package com.saintnico.verdlyhabits.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.widget.WidgetNavigation
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

        val title = message.notification?.title ?: message.data["title"] ?: "Verdly Challenge"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Something happened in your challenge!"
        val type = message.data["type"] ?: "general"
        val challengeId = message.data["challengeId"].orEmpty()
        val route = message.data["route"].orEmpty()

        showNotification(title, body, type, challengeId, route)
    }

    private fun showNotification(title: String, body: String, type: String, challengeId: String, route: String) {
        val notificationsEnabled = runCatching {
            runBlocking { ThemePreference(this@VerdlyFcmService).isNotificationsEnabled.first() }
        }.getOrDefault(false)
        if (!notificationsEnabled) return

        val (vibrationEnabled, soundEnabled) = runCatching {
            runBlocking { NotificationHelper.readAlertPrefs(this@VerdlyFcmService) }
        }.getOrDefault(true to true)
        createChannels(vibrationEnabled, soundEnabled)

        // Deep link: social notifications open Activity hub; challenge ones open arena.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            when {
                route == WidgetNavigation.ROUTE_DUO ||
                    type == "duo_buddy_done" ||
                    type == "duo_milestone" ||
                    type == "duo_invite" -> {
                    putExtra("open_duo", true)
                }
                route == "home" -> {
                    putExtra(com.saintnico.verdlyhabits.widget.WidgetNavigation.EXTRA_ROUTE, com.saintnico.verdlyhabits.widget.WidgetNavigation.ROUTE_HOME)
                }
                type in setOf("friend_request", "arena_invite", "nudge") || route == "notifications" -> {
                    putExtra("open_notifications", true)
                }
                else -> {
                    putExtra(com.saintnico.verdlyhabits.widget.WidgetNavigation.EXTRA_ROUTE, com.saintnico.verdlyhabits.widget.WidgetNavigation.ROUTE_CHALLENGES)
                    if (challengeId.isNotBlank()) {
                        putExtra(com.saintnico.verdlyhabits.widget.WidgetNavigation.EXTRA_CHALLENGE_ID, challengeId)
                    }
                }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            challengeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelFor(type))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        if (vibrationEnabled) {
            builder.setVibrate(NotificationHelper.REMINDER_VIBRATE_PATTERN)
        } else {
            builder.setVibrate(longArrayOf(0))
        }
        if (soundEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        } else {
            builder.setSound(null)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun channelFor(type: String): String = when (type) {
        "took_lead", "overtaken", "climbed" -> CHANNEL_LEADERBOARD
        "proof_posted", "reaction_received" -> CHANNEL_SOCIAL
        "friend_request", "duo_invite", "arena_invite", "duo_buddy_done", "duo_milestone", "nudge" -> CHANNEL_CONNECTIONS
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
