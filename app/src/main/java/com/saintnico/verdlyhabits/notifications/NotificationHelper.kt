package com.saintnico.verdlyhabits.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.flow.first

object NotificationHelper {

    /** Strong pattern for habit/goal reminders — always fired on device + notification channel. */
    val REMINDER_VIBRATE_PATTERN = longArrayOf(0, 450, 120, 450, 120, 650)

    private const val CHANNEL_PREFS_NAME = "verdly_notification_channel_cfg"

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
    ) {
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

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

        nm.notify(notificationId, builder.build())
    }
}
