package com.saintnico.verdlyhabits.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/** Exact alarms for goal weekly check-in + daily motivation (replaces flaky WorkManager for these). */
object GoalReminderScheduler {

    const val ACTION_WEEKLY_CHECK_IN = "com.saintnico.verdlyhabits.GOAL_WEEKLY_CHECK_IN"
    const val ACTION_DAILY_WHY = "com.saintnico.verdlyhabits.GOAL_DAILY_WHY"

    private const val REQ_WEEKLY = 91001
    private const val REQ_DAILY = 91002

    fun scheduleAll(context: Context) {
        val prefs = ThemePreference(context)
        val (weeklyOn, dailyOn, dailyTime) = runBlocking {
            Triple(
                prefs.goalWeeklyCheckInEnabled.first(),
                prefs.goalDailyReminderEnabled.first(),
                prefs.goalDailyReminderTime.first(),
            )
        }
        if (weeklyOn) {
            scheduleWeekly(context)
        } else {
            cancelWeekly(context)
        }
        if (dailyOn) {
            scheduleDaily(context, dailyTime)
        } else {
            cancelDaily(context)
        }
    }

    private fun scheduleWeekly(context: Context) {
        val trigger = millisUntilNext(dayOfWeek = Calendar.SUNDAY, hour = 18, minute = 0)
        scheduleExact(context, ACTION_WEEKLY_CHECK_IN, REQ_WEEKLY, trigger)
    }

    private fun scheduleDaily(context: Context, time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 30
        val trigger = millisUntilNext(dayOfWeek = null, hour = hour, minute = minute)
        scheduleExact(context, ACTION_DAILY_WHY, REQ_DAILY, trigger)
    }

    fun rescheduleAfterFire(context: Context, action: String) {
        when (action) {
            ACTION_WEEKLY_CHECK_IN -> scheduleWeekly(context)
            ACTION_DAILY_WHY -> {
                val time = runBlocking { ThemePreference(context).goalDailyReminderTime.first() }
                scheduleDaily(context, time)
            }
        }
    }

    private fun scheduleExact(context: Context, action: String, requestCode: Int, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply { this.action = action }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelWeekly(context: Context) = cancel(context, ACTION_WEEKLY_CHECK_IN, REQ_WEEKLY)
    private fun cancelDaily(context: Context) = cancel(context, ACTION_DAILY_WHY, REQ_DAILY)

    private fun cancel(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, GoalReminderReceiver::class.java).apply { this.action = action }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
    }

    private fun millisUntilNext(dayOfWeek: Int?, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (dayOfWeek != null) {
                while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                    add(Calendar.DATE, 1)
                }
            }
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, if (dayOfWeek != null) 7 else 1)
            }
        }
        return cal.timeInMillis
    }
}
