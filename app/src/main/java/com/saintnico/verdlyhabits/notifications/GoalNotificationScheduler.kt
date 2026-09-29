package com.saintnico.verdlyhabits.notifications

import android.content.Context

/** Schedules goal retention notifications via exact alarms ([GoalReminderScheduler]). */
object GoalNotificationScheduler {

    fun scheduleAll(context: Context) {
        GoalReminderScheduler.scheduleAll(context)
    }
}
