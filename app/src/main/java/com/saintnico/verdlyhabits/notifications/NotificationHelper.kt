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

object NotificationHelper {

    /** Strong pattern for habit/goal reminders — always fired on device + notification channel. */
    val REMINDER_VIBRATE_PATTERN = longArrayOf(0, 450, 120, 450, 120, 650)

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            enableVibration(true)
            vibrationPattern = REMINDER_VIBRATE_PATTERN
            enableLights(true)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun vibrateForReminder(context: Context) {
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

    fun showReminder(
        context: Context,
        notificationId: Int,
        channelId: String,
        channelName: String,
        title: String,
        body: String,
        intentExtras: Map<String, Any?> = emptyMap(),
    ) {
        if (!canPostNotifications(context)) return

        ensureChannel(context, channelId, channelName)
        vibrateForReminder(context)

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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(REMINDER_VIBRATE_PATTERN)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        nm.notify(notificationId, notification)
    }
}
