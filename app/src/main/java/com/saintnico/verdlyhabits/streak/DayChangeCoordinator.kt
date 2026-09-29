package com.saintnico.verdlyhabits.streak

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.saintnico.verdlyhabits.data.streak.StreakRepository
import java.util.concurrent.TimeUnit

/**
 * Keeps streak state honest about what day it is.
 *
 * The old implementation computed `streak` and `isCompleted` only when habits were loaded at
 * process start, so an app left open overnight kept showing yesterday's state — and the first
 * tap on a stale card was swallowed entirely. This coordinator pushes a fresh day into
 * [StreakRepository] whenever the calendar day, the timezone, or the wall clock changes, plus
 * on every foreground and on a periodic safety tick.
 */
object DayChangeCoordinator {

    private const val TAG = "DayChangeCoordinator"
    private const val PERIODIC_WORK = "streak_day_tick"

    @Volatile
    private var receiver: BroadcastReceiver? = null

    fun start(context: Context) {
        val app = context.applicationContext
        try {
            registerSystemReceiver(app)
            schedulePeriodicTick(app)
            onForeground(app)
        } catch (e: Exception) {
            // Never let day-change wiring take down the process on launch.
            Log.e(TAG, "start failed", e)
        }
    }

    /** Call from the activity's resume so returning to the app never shows a stale day. */
    fun onForeground(context: Context) {
        try {
            StreakRepository.get(context).refreshDay()
        } catch (e: Exception) {
            Log.e(TAG, "onForeground failed", e)
        }
    }

    @Synchronized
    private fun registerSystemReceiver(context: Context) {
        if (receiver != null) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        val instance = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val ctx = context?.applicationContext ?: return
                Log.d(TAG, "day boundary event: ${intent?.action}")
                try {
                    StreakRepository.get(ctx).refreshDay()
                    StreakNotifier.rescheduleAll(ctx)
                } catch (e: Exception) {
                    Log.e(TAG, "day boundary handling failed", e)
                }
            }
        }
        // API 33+ requires an explicit export flag — omitting it crashes on open.
        ContextCompat.registerReceiver(
            context,
            instance,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiver = instance
    }

    private fun schedulePeriodicTick(context: Context) {
        val request = PeriodicWorkRequestBuilder<StreakDayTickWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

class StreakDayTickWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = StreakRepository.get(applicationContext)
            repo.migrateLegacyIfNeeded()
            repo.refreshDay()
            val losses = repo.reconcile()
            StreakNotifier.rescheduleAll(applicationContext)
            if (losses.isNotEmpty()) {
                StreakNotifier.notifyLosses(applicationContext, losses)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("StreakDayTickWorker", "tick failed", e)
            Result.retry()
        }
    }
}
