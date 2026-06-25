package com.saintnico.verdlyhabits.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.Calendar

private const val SECOND_ALARM_TAG = 0x5EED_5EED.toInt()

object ReminderScheduler {

    fun schedule(context: Context, habit: HabitItem) {
        cancel(context, habit.id)
        if (!habit.reminderEnabled || habit.isPaused || habit.isArchived) return
        habit.reminderTime?.let { scheduleAt(context, habit, it, habit.id.hashCode()) }
        habit.reminderTime2?.let { scheduleAt(context, habit, it, habit.id.hashCode() xor SECOND_ALARM_TAG) }
    }

    fun scheduleAll(context: Context) {
        val habits = runBlocking { HabitStoreReader.loadHabitsForReminders(context) }
        habits.forEach { schedule(context, it) }
    }

    private fun scheduleAt(context: Context, habit: HabitItem, time: String, requestCode: Int) {
        val triggerAt = nextTriggerMillis(habit, time) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(ReminderReceiver.EXTRA_HABIT_TITLE, habit.title)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TIME, time)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    /** Called from [ReminderReceiver] after a reminder fires. */
    fun rescheduleFromIntent(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(ReminderReceiver.EXTRA_HABIT_ID) ?: return
        val time = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_TIME) ?: return
        val requestCode = intent.getIntExtra(ReminderReceiver.EXTRA_REQUEST_CODE, habitId.hashCode())
        val habit = runBlocking { HabitStoreReader.loadHabitsForReminders(context) }
            .firstOrNull { it.id == habitId } ?: return
        if (!habit.reminderEnabled || habit.isPaused || habit.isArchived) return
        val triggerAt = nextTriggerMillis(habit, time, afterToday = true) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(ReminderReceiver.EXTRA_HABIT_TITLE, habit.title)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TIME, time)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            nextIntent,
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

    private fun nextTriggerMillis(
        habit: HabitItem,
        time: String,
        afterToday: Boolean = false,
    ): Long? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        var date = LocalDate.now()
        if (afterToday) date = date.plusDays(1)

        repeat(14) {
            if (HabitScheduling.isDueOn(habit.frequency, habit.customDaysMask, date)) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, date.year)
                    set(Calendar.MONTH, date.monthValue - 1)
                    set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (!afterToday && cal.before(Calendar.getInstance())) {
                    date = date.plusDays(1)
                    return@repeat
                }
                if (cal.timeInMillis > System.currentTimeMillis()) {
                    return cal.timeInMillis
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    fun cancel(context: Context, habitId: String) {
        cancelOne(context, habitId.hashCode())
        cancelOne(context, habitId.hashCode() xor SECOND_ALARM_TAG)
    }

    private fun cancelOne(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }
}
