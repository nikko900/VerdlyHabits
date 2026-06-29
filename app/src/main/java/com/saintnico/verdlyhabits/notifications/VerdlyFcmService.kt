package com.saintnico.verdlyhabits.notifications

import android.app.NotificationChannel
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
        createChannels()
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
                type in setOf("friend_request", "arena_invite") || route == "notifications" -> {
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

        val notification = NotificationCompat.Builder(this, channelFor(type))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun channelFor(type: String): String = when (type) {
        "took_lead", "overtaken", "climbed" -> CHANNEL_LEADERBOARD
        "proof_posted", "reaction_received" -> CHANNEL_SOCIAL
        "friend_request", "duo_invite", "arena_invite", "duo_buddy_done", "duo_milestone" -> CHANNEL_CONNECTIONS
        else -> CHANNEL_CHALLENGES
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONNECTIONS,
                "Connections",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Friend requests, duo streak invites, and arena requests" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LEADERBOARD,
                "Leaderboard",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "When you take the lead, climb, or get overtaken" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SOCIAL,
                "Proofs & Reactions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "When rivals post proof or react to yours" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONNECTIONS,
                "Connections",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Friend requests, duo invites, and arena requests" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHALLENGES,
                "Challenge Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "General challenge updates and reminders" }
        )
    }
}
