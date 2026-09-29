package com.saintnico.verdlyhabits.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.widget.WidgetNavigation
import kotlinx.coroutines.flow.first

object NotificationHelper {

    /** Strong pattern for habit/goal reminders — always fired on device + notification channel. */
    val REMINDER_VIBRATE_PATTERN = longArrayOf(0, 450, 120, 450, 120, 650)

    const val GROUP_PEOPLE = "verdly_people"
    const val EXTRA_NOTIFICATION_ID = "verdly_notification_id"

    private const val CHANNEL_PREFS_NAME = "verdly_notification_channel_cfg"
    private const val PERSON_TYPES = "friend_request,duo_invite,arena_invite,nudge," +
        "profile_view,proof_posted,reaction_received,duo_buddy_done,rank_overtaken," +
        "lost_lead,overtaken,coin_gift,grove_overtake"

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads the user's Settings → Vibration / Sound effects preferences.
     * Safe to call from a background thread (Firebase's messaging callback,
     * or a BroadcastReceiver's `goAsync()` coroutine) — never from the main thread.
     */
    suspend fun readAlertPrefs(context: Context): Pair<Boolean, Boolean> {
        val vibration = runCatching { ThemePreference(context).isVibrationEnabled.first() }.getOrDefault(true)
        val sound = runCatching { AppDataStore(context).soundEnabled.first() }.getOrDefault(true)
        return vibration to sound
    }

    /**
     * Creates (or recreates) a notification channel so its vibration/sound behavior matches
     * the user's current Settings toggles. Android locks a channel's sound/vibration once
     * created, so this deletes and re-creates it only when the desired config actually changed
     * — cheap no-op on every other call.
     */
    fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        vibrationEnabled: Boolean = true,
        soundEnabled: Boolean = true,
        description: String? = null,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prefs = context.getSharedPreferences(CHANNEL_PREFS_NAME, Context.MODE_PRIVATE)
        val prefKey = "cfg_$channelId"
        val desiredCfg = "$vibrationEnabled:$soundEnabled"
        val existing = nm.getNotificationChannel(channelId)

        if (existing != null && prefs.getString(prefKey, null) == desiredCfg) return

        if (existing != null) {
            nm.deleteNotificationChannel(channelId)
        }
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            if (vibrationEnabled) {
                enableVibration(true)
                vibrationPattern = REMINDER_VIBRATE_PATTERN
            } else {
                enableVibration(false)
            }
            if (!soundEnabled) {
                setSound(null, null)
            }
            enableLights(true)
            setShowBadge(true)
            description?.let { this.description = it }
        }
        nm.createNotificationChannel(channel)
        prefs.edit().putString(prefKey, desiredCfg).apply()
    }

    fun vibrateForReminder(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(REMINDER_VIBRATE_PATTERN, -1),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(REMINDER_VIBRATE_PATTERN, -1)
            }
        } catch (_: Exception) {
        }
    }

    /** Local reminder entry point that honors the user's live vibration/sound preferences. */
    suspend fun showReminderRespectingPrefs(
        context: Context,
        notificationId: Int,
        channelId: String,
        channelName: String,
        title: String,
        body: String,
        intentExtras: Map<String, Any?> = emptyMap(),
        largeIcon: Bitmap? = null,
    ) {
        if (channelId == ReminderReceiver.CHANNEL_ID &&
            !ReminderScheduler.globalHabitRemindersEnabled(context)
        ) {
            return
        }
        val (vibration, sound) = readAlertPrefs(context)
        showReminder(
            context = context,
            notificationId = notificationId,
            channelId = channelId,
            channelName = channelName,
            title = title,
            body = body,
            intentExtras = intentExtras,
            vibrationEnabled = vibration,
            soundEnabled = sound,
            largeIcon = largeIcon,
        )
    }

    fun showReminder(
        context: Context,
        notificationId: Int,
        channelId: String,
        channelName: String,
        title: String,
        body: String,
        intentExtras: Map<String, Any?> = emptyMap(),
        vibrationEnabled: Boolean = true,
        soundEnabled: Boolean = true,
        largeIcon: Bitmap? = null,
    ) {
        if (!canPostNotifications(context)) return

        ensureChannel(
            context,
            channelId,
            channelName,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
        )
        vibrateForReminder(context, vibrationEnabled)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intentExtras.forEach { (k, v) ->
                when (v) {
                    is String -> putExtra(k, v)
                    is Boolean -> putExtra(k, v)
                    is Int -> putExtra(k, v)
                }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_verdly)
            .setColor(ContextCompat.getColor(context, R.color.verdly_notification))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
        val avatar = largeIcon
            ?: NotificationAvatars.brandCircle(context)
        avatar?.let { builder.setLargeIcon(it) }
        applyAlerts(builder, vibrationEnabled, soundEnabled)

        nm.notify(notificationId, builder.build())
    }

    /**
     * Instagram-style shade notification: circular avatar, person's name, conversation chrome,
     * and Accept / Decline when the event is a request.
     */
    suspend fun showPersonNotification(
        context: Context,
        channelId: String,
        channelName: String,
        title: String,
        body: String,
        type: String = "",
        actorUid: String = "",
        actorName: String = "",
        actorPhotoUrl: String? = null,
        referenceId: String = "",
        challengeId: String = "",
        inboxId: String = "",
        intentExtras: Map<String, Any?> = emptyMap(),
        vibrationEnabled: Boolean = true,
        soundEnabled: Boolean = true,
        notificationId: Int? = null,
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(
            context,
            channelId,
            channelName,
            vibrationEnabled = vibrationEnabled,
            soundEnabled = soundEnabled,
        )
        vibrateForReminder(context, vibrationEnabled)

        val extras = intentExtras.ifEmpty { extrasFor(type, challengeId) }
        val id = notificationId ?: idFor(type, referenceId, actorUid, title, body)
        val personName = displayName(actorName, title, body)
        val fromPerson = isPersonType(type) && personName.isNotBlank()
        val showAvatar = fromPerson || !actorPhotoUrl.isNullOrBlank() || actorName.isNotBlank()
        val avatar = if (showAvatar) {
            NotificationAvatars.loadCircular(context, actorPhotoUrl, personName.ifBlank { "V" })
        } else {
            NotificationAvatars.brandCircle(context)
                ?: NotificationAvatars.letterCircle(personName.ifBlank { "V" })
        }
        val message = if (fromPerson) messageText(personName, title, body) else body
        val headline = if (fromPerson) personName else title
        val pendingIntent = activityPendingIntent(context, id, extras)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_verdly)
            .setColor(ContextCompat.getColor(context, R.color.verdly_notification))
            .setContentTitle(headline)
            .setContentText(message)
            .setLargeIcon(avatar)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setOnlyAlertOnce(type == "nudge")
            .setNumber(1)

        if (fromPerson) {
            val sender = person(personName, actorUid, avatar)
            builder
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup(GROUP_PEOPLE)
                .addPerson(sender)
                .setStyle(
                    NotificationCompat.MessagingStyle(selfPerson())
                        .addMessage(message, System.currentTimeMillis(), sender),
                )
            attachActions(
                context = context,
                builder = builder,
                notificationId = id,
                type = type,
                actorUid = actorUid,
                actorName = personName,
                actorPhotoUrl = actorPhotoUrl,
                referenceId = referenceId,
                challengeId = challengeId,
                inboxId = inboxId.ifBlank { referenceId },
            )
        } else {
            builder
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(headline))
        }
        applyAlerts(builder, vibrationEnabled, soundEnabled)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, builder.build())
    }

    fun extrasFor(type: String, challengeId: String, route: String = ""): Map<String, Any?> {
        return when {
            route == WidgetNavigation.ROUTE_DUO ||
                type == "duo_buddy_done" ||
                type == "duo_invite" ||
                type == "duo_milestone" -> mapOf("open_duo" to true)
            route == "notifications" ||
                (
                    type in setOf("friend_request", "arena_invite", "profile_view", "nudge") &&
                        route != "arena" &&
                        route != WidgetNavigation.ROUTE_DUO &&
                        route != "friends_grove"
                    ) -> mapOf("open_notifications" to true)
            route == "friends_grove" -> mapOf(WidgetNavigation.EXTRA_ROUTE to "friends_grove")
            route == "home" -> mapOf(WidgetNavigation.EXTRA_ROUTE to WidgetNavigation.ROUTE_HOME)
            route == "arena" || challengeId.isNotBlank() -> buildMap {
                put(WidgetNavigation.EXTRA_ROUTE, WidgetNavigation.ROUTE_CHALLENGES)
                if (challengeId.isNotBlank()) put(WidgetNavigation.EXTRA_CHALLENGE_ID, challengeId)
            }
            type == "nudge" && route == "arena" ->
                mapOf(WidgetNavigation.EXTRA_ROUTE to WidgetNavigation.ROUTE_DUO)
            else -> mapOf("open_notifications" to true)
        }
    }

    fun idFor(type: String, referenceId: String, actorUid: String, title: String, body: String): Int {
        val key = when {
            type == "nudge" && actorUid.isNotBlank() -> "nudge:$actorUid"
            referenceId.isNotBlank() -> "$type:$referenceId"
            actorUid.isNotBlank() -> "$type:$actorUid"
            else -> "$type:$title:$body"
        }
        return key.hashCode()
    }

    fun cancel(context: Context, notificationId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.cancel(notificationId)
    }

    private fun applyAlerts(
        builder: NotificationCompat.Builder,
        vibrationEnabled: Boolean,
        soundEnabled: Boolean,
    ) {
        if (vibrationEnabled) {
            builder.setVibrate(REMINDER_VIBRATE_PATTERN)
        } else {
            builder.setVibrate(longArrayOf(0))
        }
        if (soundEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        } else {
            builder.setSound(null)
        }
    }

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        extras: Map<String, Any?>,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            extras.forEach { (k, v) ->
                when (v) {
                    is String -> putExtra(k, v)
                    is Boolean -> putExtra(k, v)
                    is Int -> putExtra(k, v)
                }
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun attachActions(
        context: Context,
        builder: NotificationCompat.Builder,
        notificationId: Int,
        type: String,
        actorUid: String,
        actorName: String,
        actorPhotoUrl: String?,
        referenceId: String,
        challengeId: String,
        inboxId: String,
    ) {
        fun action(action: String, label: String, codeOffset: Int): NotificationCompat.Action {
            val intent = Intent(context, SocialNotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(SocialNotificationActionReceiver.EXTRA_TYPE, type)
                putExtra(SocialNotificationActionReceiver.EXTRA_REFERENCE_ID, referenceId)
                putExtra(SocialNotificationActionReceiver.EXTRA_INBOX_ID, inboxId)
                putExtra(SocialNotificationActionReceiver.EXTRA_ACTOR_UID, actorUid)
                putExtra(SocialNotificationActionReceiver.EXTRA_ACTOR_NAME, actorName)
                putExtra(SocialNotificationActionReceiver.EXTRA_ACTOR_PHOTO, actorPhotoUrl)
                putExtra(SocialNotificationActionReceiver.EXTRA_CHALLENGE_ID, challengeId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + codeOffset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Action.Builder(0, label, pi).build()
        }
        when (type) {
            "friend_request" -> {
                builder.addAction(action(SocialNotificationActionReceiver.ACTION_ACCEPT_FRIEND, "Accept", 1))
                builder.addAction(action(SocialNotificationActionReceiver.ACTION_DECLINE_FRIEND, "Decline", 2))
            }
            "duo_invite" -> {
                builder.addAction(action(SocialNotificationActionReceiver.ACTION_ACCEPT_DUO, "Start duo", 1))
                builder.addAction(action(SocialNotificationActionReceiver.ACTION_DECLINE_DUO, "Not now", 2))
            }
            "arena_invite" -> {
                builder.addAction(action(SocialNotificationActionReceiver.ACTION_APPROVE_ARENA, "Let them in", 1))
                builder.addAction(action(SocialNotificationActionReceiver.ACTION_DECLINE_ARENA, "Decline", 2))
            }
        }
    }

    private fun isPersonType(type: String): Boolean =
        type.isNotBlank() && type in PERSON_TYPES.split(",")

    private fun displayName(actorName: String, title: String, body: String): String {
        val raw = actorName.trim().removePrefix("@")
        if (raw.isNotBlank() && !raw.equals("Someone", ignoreCase = true) &&
            !raw.equals("rival", ignoreCase = true)
        ) {
            return raw.replaceFirstChar { it.uppercase() }
        }
        val fromBody = Regex("@([A-Za-z0-9_.]+)").find(body)?.groupValues?.getOrNull(1)
        if (!fromBody.isNullOrBlank()) return fromBody.replaceFirstChar { it.uppercase() }
        return raw.replaceFirstChar { it.uppercase() }.ifBlank { title }
    }

    private fun messageText(name: String, title: String, body: String): String {
        val stripped = body
            .replace(Regex("^@?${Regex.escape(name)}\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^@?[A-Za-z0-9_.]+\\s+"), "")
            .trim()
            .trimStart('—', '-', '·')
            .trim()
        return stripped.ifBlank { title }
    }

    private fun person(name: String, uid: String, avatar: Bitmap): Person {
        return Person.Builder()
            .setName(name)
            .setKey(uid.ifBlank { name })
            .setIcon(IconCompat.createWithBitmap(avatar))
            .setImportant(true)
            .build()
    }

    private fun selfPerson(): Person =
        Person.Builder().setName("You").setKey("me").build()

    suspend fun showQuitConfirmReminder(
        context: Context,
        goalId: String,
        goalTitle: String,
    ) {
        if (!canPostNotifications(context)) return
        val (vibration, sound) = readAlertPrefs(context)
        val channelId = "goal_daily_reminder"
        ensureChannel(
            context,
            channelId,
            "Goal Reminders",
            vibrationEnabled = vibration,
            soundEnabled = sound,
        )
        vibrateForReminder(context, vibration)

        val notificationId = 2100 + (goalId.hashCode() and 0x7FFF)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_goal_detail", goalId)
        }
        val openPi = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val clearPi = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            Intent(context, QuitConfirmReceiver::class.java).apply {
                action = QuitConfirmReceiver.ACTION_STILL_CLEAR
                putExtra(QuitConfirmReceiver.EXTRA_GOAL_ID, goalId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val slipPi = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            Intent(context, QuitConfirmReceiver::class.java).apply {
                action = QuitConfirmReceiver.ACTION_SLIP
                putExtra(QuitConfirmReceiver.EXTRA_GOAL_ID, goalId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = "Still clear. $goalTitle?"
        val body = "One tap keeps today on your streak. We won't guess if you stay quiet."
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_verdly)
            .setColor(ContextCompat.getColor(context, R.color.verdly_notification))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .addAction(0, "Still clear", clearPi)
            .addAction(0, "I slipped", slipPi)
            .addAction(0, "Open", openPi)
        builder.setLargeIcon(NotificationGlyphs.forGoalType(com.saintnico.verdlyhabits.data.local.goals.GoalType.QUIT, null))
        applyAlerts(builder, vibration, sound)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, builder.build())
    }
}
