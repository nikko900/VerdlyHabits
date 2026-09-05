package com.saintnico.verdlyhabits.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saintnico.verdlyhabits.sensors.DigitalHabitTracker
import com.saintnico.verdlyhabits.sensors.HealthConnectManager
import com.saintnico.verdlyhabits.ml.AddictionPredictionEngine
import com.saintnico.verdlyhabits.ml.RelapsePredictionEngine
import com.saintnico.verdlyhabits.ml.SmartNudgeEngine
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.notifications.NotificationHelper

class HabitSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("HabitSyncWorker", "Starting background sync...")
        val context = applicationContext

        // Initialize Managers and Engines
        val healthConnectManager = HealthConnectManager(context)
        val digitalHabitTracker = DigitalHabitTracker(context)
        val smartNudgeEngine = SmartNudgeEngine(context)
        val relapsePredictionEngine = RelapsePredictionEngine(context)
        val addictionPredictionEngine = AddictionPredictionEngine(context)
        val appDataStore = AppDataStore(context)

        try {
            // 1. Sync Health Connect Data
            if (healthConnectManager.isAvailable() && healthConnectManager.hasAllPermissions()) {
                // We would typically fetch a stored token from DataStore here
                // For demonstration, we just request a new token
                val token = healthConnectManager.getChangesToken()
                val changes = healthConnectManager.getChanges(token)
                Log.d("HabitSyncWorker", "Fetched health changes: $changes")
                // Store updated token...
            } else {
                Log.d("HabitSyncWorker", "Health Connect not available or missing permissions")
            }

            // 2. Track Digital Habits (UsageStats)
            // Note: UsageStats requires PACKAGE_USAGE_STATS permission
            var screenTimeMillis = 0L
            try {
                screenTimeMillis = digitalHabitTracker.getTotalScreenTimeMillis()
                Log.d("HabitSyncWorker", "Total Screen Time today: ${screenTimeMillis / 1000 / 60} minutes")
            } catch (e: Exception) {
                Log.e("HabitSyncWorker", "Could not access usage stats: ${e.message}")
            }

            // 3. Run ML Inference (Placeholder values used here)
            val nudgeProb = smartNudgeEngine.predictEngagementProbability(
                timeOfDay = 0.6f, // e.g. 2 PM
                dayOfWeek = 0.2f, // e.g. Tuesday
                activityState = 1.0f, // e.g. Walking
                timeSinceLastCompletion = 24.0f
            )
            Log.d("HabitSyncWorker", "Smart Nudge Engagement Probability: $nudgeProb")

            val churnProb = relapsePredictionEngine.predictChurnProbability(
                currentStreakLength = 3.0f,
                completionRatio = 0.5f,
                responseTime = 120.0f
            )
            Log.d("HabitSyncWorker", "Relapse Prediction Churn Probability: $churnProb")

            val screenTimeHours = screenTimeMillis / (1000f * 60f * 60f)
            val addictionScore = addictionPredictionEngine.predictAddictionScore(screenTimeHours)
            Log.d("HabitSyncWorker", "Predicted Addiction Score: $addictionScore")

            // Save to DataStore so UI can react
            appDataStore.setNudgeProbability(nudgeProb)
            appDataStore.setChurnProbability(churnProb)

            if (churnProb > 0.8f) {
                // Suggest a micro-habit next time the user opens the app
                Log.d("HabitSyncWorker", "High churn probability detected. Queueing micro-habit suggestion.")
            }

            if (nudgeProb > 0.7f) {
                Log.d("HabitSyncWorker", "High engagement probability detected. Firing Smart Nudge notification.")
                NotificationHelper.showReminderRespectingPrefs(
                    context = context,
                    notificationId = 9999, // Unique ID for smart nudge
                    channelId = "verdly_smart_nudge",
                    channelName = "Smart Nudges",
                    title = "Optimal Time to Habit!",
                    body = "Our AI suggests right now is the best time to complete your habit."
                )
            }

        } catch (e: Exception) {
            Log.e("HabitSyncWorker", "Error during sync: ${e.message}", e)
            return Result.retry()
        } finally {
            smartNudgeEngine.close()
            relapsePredictionEngine.close()
            addictionPredictionEngine.close()
        }

        return Result.success()
    }
}
