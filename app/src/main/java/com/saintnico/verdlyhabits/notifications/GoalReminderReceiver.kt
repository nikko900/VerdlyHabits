package com.saintnico.verdlyhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GoalReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    GoalReminderScheduler.ACTION_WEEKLY_CHECK_IN -> {
                        if (ThemePreference(app).goalWeeklyCheckInEnabled.first()) {
                            showWeeklyCheckIn(app)
                        }
                    }
                    GoalReminderScheduler.ACTION_DAILY_WHY -> {
                        if (ThemePreference(app).goalDailyReminderEnabled.first()) {
                            showDailyWhy(app)
                        }
                    }
                }
            } finally {
                GoalReminderScheduler.rescheduleAfterFire(app, action)
                pending.finish()
            }
        }
    }

    private suspend fun showWeeklyCheckIn(context: Context) {
        val goals = AppDatabase.getDatabase(context).goalsDao().getActiveGoals().first()
        if (goals.isEmpty()) return
        NotificationHelper.showReminderRespectingPrefs(
            context = context,
            notificationId = 1001,
            channelId = "goal_weekly_check_in",
            channelName = "Goal Weekly Check-In",
            title = "Weekly goal check",
            body = "How did this week go? Tap to check in on your goals.",
            intentExtras = mapOf("open_weekly_check_in" to true),
        )
    }

    private suspend fun showDailyWhy(context: Context) {
        val habits = HabitStoreReader.loadHabitsForReminders(context)
        val goals = AppDatabase.getDatabase(context).goalsDao().getActiveGoals().first()
        val goal = goals.firstOrNull { g ->
            g.whyStatement.isNotBlank()
        } ?: goals.firstOrNull() ?: return

        val missed = GoalProgressEngine.missedYesterday(goal, habits)
        val body = when {
            missed && goal.whyStatement.isNotBlank() ->
                goal.whyStatement.take(140).let { if (goal.whyStatement.length > 140) "$it…" else it }
            goal.whyStatement.isNotBlank() ->
                "Stay on track with \"${goal.title}\". ${goal.whyStatement.take(80)}"
            else ->
                "Open Verdly and log progress on \"${goal.title}\" today."
        }

        NotificationHelper.showReminderRespectingPrefs(
            context = context,
            notificationId = 1002,
            channelId = "goal_daily_reminder",
            channelName = "Goal Reminders",
            title = if (missed) "Remember why — ${goal.title}" else "Goal check-in — ${goal.title}",
            body = body,
            intentExtras = mapOf("open_goal_detail" to goal.id),
        )
    }
}
