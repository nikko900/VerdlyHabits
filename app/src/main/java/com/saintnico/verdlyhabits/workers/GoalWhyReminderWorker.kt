package com.saintnico.verdlyhabits.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import kotlinx.coroutines.flow.first

class GoalWhyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habits = HabitStoreReader.loadHabits(applicationContext)
        val goalsDao = AppDatabase.getDatabase(applicationContext).goalsDao()
        val goals = goalsDao.getActiveGoals().first()
        val goal = goals.firstOrNull { g ->
            g.whyStatement.isNotBlank() && GoalProgressEngine.missedYesterday(g, habits)
        } ?: return Result.success()

        showWhyNotification(goal.title, goal.whyStatement, goal.id)
        return Result.success()
    }

    private fun showWhyNotification(goalTitle: String, why: String, goalId: String) {
        val channelId = "goal_why_reminder"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Goal Motivation", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_goal_detail", goalId)
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            goalId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = why.take(120).let { if (why.length > 120) "$it…" else it }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Remember why — $goalTitle")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(1002, notification)
    }
}
