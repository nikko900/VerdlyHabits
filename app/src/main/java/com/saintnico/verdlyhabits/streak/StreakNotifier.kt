package com.saintnico.verdlyhabits.streak

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.data.streak.StreakRepository
import com.saintnico.verdlyhabits.domain.streak.StreakLossRecord
import com.saintnico.verdlyhabits.domain.streak.StreakResult
import com.saintnico.verdlyhabits.domain.streak.StreakStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Streak notifications: the escalating at-risk reminder, and the loss moment.
 *
 * The at-risk push replaces the old blanket "Don't break your streak" reminder, which fired on
 * a fixed schedule whether or not anything was actually at risk. These fire off the engine's
 * own [StreakStatus], at thresholds read from the same config the UI uses.
 */
object StreakNotifier {

    private const val TAG = "StreakNotifier"

    const val CHANNEL_AT_RISK = "streak_at_risk"
    const val CHANNEL_LOST = "streak_lost"

    const val EXTRA_HABIT_ID = "streak_habit_id"
    const val EXTRA_LOSS_ID = "streak_loss_id"
    const val EXTRA_URGENT = "streak_urgent"

    /** Deep-link flags read by the activity to route into the right surface. */
    const val EXTRA_OPEN_LOSS = "open_streak_loss"
    const val EXTRA_OPEN_HABIT = "open_habit_streak"

    private const val REQUEST_SOFT = 81_000
    private const val REQUEST_URGENT = 82_000
    private const val NOTIFICATION_AT_RISK = 83_000
    private const val NOTIFICATION_LOST = 84_000

    // ── Scheduling ────────────────────────────────────────────────────────────

    /**
     * Re-arms every streak alarm from current state. Safe to call repeatedly — alarms are
     * keyed per habit, so re-scheduling replaces rather than duplicates.
     */
    fun rescheduleAll(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = StreakRepository.get(app)
                val results = repo.evaluateAll()
                val titles = HabitStoreReader.loadHabits(app).associate { it.id to it.title }
                for ((habitId, result) in results) {
                    scheduleFor(app, habitId, titles[habitId] ?: "Your habit", result)
                }
                scheduleLossAlarms(app, results.values)
            } catch (e: Exception) {
                Log.w(TAG, "rescheduleAll failed", e)
            }
        }
    }

    /**
     * Arms the soft and urgent at-risk alarms for one habit. Nothing is scheduled when the run
     * is already safe for the day or is not alive.
     */
    private fun scheduleFor(context: Context, habitId: String, title: String, result: StreakResult) {
        cancelAtRisk(context, habitId)
        if (result.isCompleteToday || !result.isAlive) return
        if (!result.isExpectedToday) return
        val closesAt = result.closesAt ?: return
        val config = StreakRepository.get(context).config
        val now = Instant.now()

        val soft = closesAt.minus(config.atRiskLeadTime)
        val urgent = closesAt.minus(config.urgentLeadTime)

        if (soft.isAfter(now)) {
            setAlarm(context, habitId, title, result.currentStreak, soft, urgent = false)
        }
        if (urgent.isAfter(now)) {
            setAlarm(context, habitId, title, result.currentStreak, urgent, urgent = true)
        }
    }

    /**
     * Arms an alarm for the exact instant a repair window expires, so the loss moment lands
     * when it happens rather than whenever the user next opens the app.
     */
    private fun scheduleLossAlarms(context: Context, results: Collection<StreakResult>) {
        val now = Instant.now()
        for (result in results) {
            val offer = result.repairOffer ?: continue
            if (!offer.expiresAt.isAfter(now)) continue
            val intent = Intent(context, StreakAlarmReceiver::class.java).apply {
                action = ACTION_REPAIR_EXPIRED
                putExtra(EXTRA_HABIT_ID, result.habitId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_URGENT + result.habitId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            scheduleExactish(context, offer.expiresAt, pi)
        }
    }

    private fun setAlarm(
        context: Context,
        habitId: String,
        title: String,
        streak: Int,
        at: Instant,
        urgent: Boolean,
    ) {
        val intent = Intent(context, StreakAlarmReceiver::class.java).apply {
            action = ACTION_AT_RISK
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_URGENT, urgent)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_STREAK, streak)
        }
        val base = if (urgent) REQUEST_URGENT else REQUEST_SOFT
        val pi = PendingIntent.getBroadcast(
            context,
            base + habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleExactish(context, at, pi)
    }

    private fun scheduleExactish(context: Context, at: Instant, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val millis = at.toEpochMilli()
        try {
            // Exact alarms need a special permission on S+; a windowed alarm is accurate
            // enough for a streak deadline and never gets the app flagged.
            am.setWindow(AlarmManager.RTC_WAKEUP, millis, 5 * 60_000L, pi)
        } catch (e: SecurityException) {
            Log.w(TAG, "alarm denied, falling back to inexact", e)
            am.set(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun cancelAtRisk(context: Context, habitId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (base in listOf(REQUEST_SOFT, REQUEST_URGENT)) {
            val intent = Intent(context, StreakAlarmReceiver::class.java).apply {
                action = ACTION_AT_RISK
                putExtra(EXTRA_HABIT_ID, habitId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                base + habitId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pi != null) am.cancel(pi)
        }
    }

    // ── Showing ───────────────────────────────────────────────────────────────

    fun showAtRisk(context: Context, title: String, streak: Int, urgent: Boolean, largeIcon: android.graphics.Bitmap? = null) {
        ensureChannel(
            context,
            CHANNEL_AT_RISK,
            "Streak at risk",
            if (urgent) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT,
        )
        val body = if (urgent) {
            "Under an hour left to keep your $streak-day streak."
        } else {
            "Your $streak-day streak is still open today. Finish it before the day closes."
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            NOTIFICATION_AT_RISK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_AT_RISK)
            .setSmallIcon(R.drawable.ic_stat_verdly)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.verdly_notification))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setLargeIcon(
                largeIcon ?: com.saintnico.verdlyhabits.notifications.NotificationGlyphs.forStreak(),
            )
            .build()
        notify(context, NOTIFICATION_AT_RISK + title.hashCode(), notification)
    }

    /**
     * The loss moment. Fires once per lost run, at the instant the repair window closes, and is
     * marked notified so a retry or a second worker pass cannot repeat it.
     */
    fun notifyLosses(context: Context, losses: List<StreakLossRecord>) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val repo = StreakRepository.get(app)
            val habits = HabitStoreReader.loadHabits(app)
            val titles = habits.associate { it.id to it.title }
            for (loss in losses) {
                if (!loss.isWorthSurfacing()) {
                    repo.markLossNotified(loss.identity)
                    continue
                }
                val habit = habits.firstOrNull { it.id == loss.habitId }
                showLoss(
                    app,
                    titles[loss.habitId] ?: "Your habit",
                    loss,
                    habit?.let { com.saintnico.verdlyhabits.notifications.NotificationGlyphs.forHabit(it.icon, it.color) },
                )
                repo.markLossNotified(loss.identity)
            }
        }
    }

    fun showLoss(
        context: Context,
        habitTitle: String,
        loss: StreakLossRecord,
        largeIcon: android.graphics.Bitmap? = null,
    ) {
        ensureChannel(context, CHANNEL_LOST, "Streak ended", NotificationManager.IMPORTANCE_HIGH)
        val body = if (loss.wasPreventableWithShield) {
            "Your ${loss.finalStreak}-day streak ended. A shield would have saved it."
        } else {
            "Your ${loss.finalStreak}-day streak ended."
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_LOSS, true)
            putExtra(EXTRA_LOSS_ID, loss.identity)
        }
        val pi = PendingIntent.getActivity(
            context,
            NOTIFICATION_LOST + loss.identity.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LOST)
            .setSmallIcon(R.drawable.ic_stat_verdly)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.verdly_notification))
            .setContentTitle(habitTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setLargeIcon(
                largeIcon ?: com.saintnico.verdlyhabits.notifications.NotificationGlyphs.forStreak(),
            )
            .build()
        notify(context, NOTIFICATION_LOST + loss.identity.hashCode(), notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            nm.notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "notification permission missing", e)
        }
    }

    private fun ensureChannel(context: Context, id: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(id) != null) return
        nm.createNotificationChannel(NotificationChannel(id, name, importance))
    }

    const val ACTION_AT_RISK = "com.saintnico.verdlyhabits.STREAK_AT_RISK"
    const val ACTION_REPAIR_EXPIRED = "com.saintnico.verdlyhabits.STREAK_REPAIR_EXPIRED"
    const val EXTRA_TITLE = "streak_title"
    const val EXTRA_STREAK = "streak_value"
}

/**
 * Receives streak alarms.
 *
 * Both paths re-evaluate before showing anything, so a reminder can never fire for a habit the
 * user has since completed, and a loss is only announced if it is genuinely unrecoverable.
 */
class StreakAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val habitId = intent.getStringExtra(StreakNotifier.EXTRA_HABIT_ID) ?: return
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = StreakRepository.get(app)
                repo.refreshDay()

                when (intent.action) {
                    StreakNotifier.ACTION_AT_RISK -> {
                        val result = repo.evaluateNow(habitId)
                        if (result != null && result.isAtRisk && !result.isCompleteToday) {
                            val habit = HabitStoreReader.loadHabits(app)
                                .firstOrNull { it.id == habitId }
                            val title = intent.getStringExtra(StreakNotifier.EXTRA_TITLE)
                                ?: habit?.title
                                ?: "Your habit"
                            StreakNotifier.showAtRisk(
                                context = app,
                                title = title,
                                streak = result.currentStreak,
                                urgent = intent.getBooleanExtra(StreakNotifier.EXTRA_URGENT, false),
                                largeIcon = habit?.let {
                                    com.saintnico.verdlyhabits.notifications.NotificationGlyphs.forHabit(it.icon, it.color)
                                },
                            )
                        }
                    }

                    StreakNotifier.ACTION_REPAIR_EXPIRED -> {
                        val losses = repo.reconcile()
                        if (losses.isNotEmpty()) StreakNotifier.notifyLosses(app, losses)
                    }
                }
            } catch (e: Exception) {
                Log.w("StreakAlarmReceiver", "alarm handling failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
