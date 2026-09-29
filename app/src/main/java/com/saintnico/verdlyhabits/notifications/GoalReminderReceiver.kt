package com.saintnico.verdlyhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.engine.QuitConfirmationEngine
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
            largeIcon = NotificationGlyphs.forWeeklyCheckIn(),
        )
    }

    private suspend fun showDailyWhy(context: Context) {
        val habits = HabitStoreReader.loadHabitsForReminders(context)
        val goals = AppDatabase.getDatabase(context).goalsDao().getActiveGoals().first()

        goals.filter { it.goalType == GoalType.QUIT && !it.isArchived }.forEach { goal ->
            val eval = QuitConfirmationEngine.evaluate(goal)
            if (!eval.confirmedToday) {
                NotificationHelper.showQuitConfirmReminder(context, goal.id, goal.title)
            }
        }

        val other = goals.firstOrNull { g ->
            g.goalType != GoalType.QUIT && g.whyStatement.isNotBlank()
        } ?: return

        val missed = GoalProgressEngine.missedYesterday(other, habits)
        val body = when {
            missed && other.whyStatement.isNotBlank() ->
                other.whyStatement.take(140).let { if (other.whyStatement.length > 140) "$it…" else it }
            other.whyStatement.isNotBlank() ->
                "Stay on track with \"${other.title}\". ${other.whyStatement.take(80)}"
            else ->
                "Open Verdly and log progress on \"${other.title}\" today."
        }

        NotificationHelper.showReminderRespectingPrefs(
            context = context,
            notificationId = 1002,
            channelId = "goal_daily_reminder",
            channelName = "Goal Reminders",
            title = if (missed) "Remember why. ${other.title}" else "Goal check-in. ${other.title}",
            body = body,
            intentExtras = mapOf("open_goal_detail" to other.id),
            largeIcon = if (missed) {
                NotificationGlyphs.forMotivation(other.colorHex)
            } else {
                NotificationGlyphs.forGoalType(other.goalType, other.colorHex)
            },
        )
    }
}
