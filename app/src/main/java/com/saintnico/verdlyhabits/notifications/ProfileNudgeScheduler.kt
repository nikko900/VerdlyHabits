package com.saintnico.verdlyhabits.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.saintnico.verdlyhabits.workers.ProfileNudgeWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Daily profile-completion nudges (photo, headline, bio). */
object ProfileNudgeScheduler {

    private const val TAG = "verdly_profile_nudge"

    fun schedule(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        val delay = (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = PeriodicWorkRequestBuilder<ProfileNudgeWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
