package com.saintnico.verdlyhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Your Habit"
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
        val appContext = context.applicationContext

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.showReminderRespectingPrefs(
                    context = appContext,
                    notificationId = habitId?.hashCode() ?: 0,
                    channelId = CHANNEL_ID,
                    channelName = "Habit Reminders",
                    title = habitTitle,
                    body = "Don't break your streak — tap to complete.",
                )
                ReminderScheduler.rescheduleFromIntent(appContext, intent)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val EXTRA_HABIT_ID = "HABIT_ID"
        const val EXTRA_HABIT_TITLE = "HABIT_TITLE"
        const val EXTRA_REMINDER_TIME = "REMINDER_TIME"
        const val EXTRA_REQUEST_CODE = "REQUEST_CODE"
    }
}
