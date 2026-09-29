package com.saintnico.verdlyhabits.streak

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.streak.PendingOpKind
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Drains the streak outbox. Network-constrained so offline completions and duo day-close
 * calls retry when connectivity returns, instead of falling back to a client-side decision.
 */
class StreakSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val gson = Gson()
    private val functions = FirebaseFunctions.getInstance()

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).streakDao()
        val due = dao.dueOps(System.currentTimeMillis(), limit = 40)
        if (due.isEmpty()) return Result.success()

        var hadFailure = false
        for (op in due) {
            try {
                when (op.kind) {
                    PendingOpKind.DUO_DAY_CLOSE.name -> {
                        val payload = gson.fromJson<Map<String, Any>>(
                            op.payload,
                            object : TypeToken<Map<String, Any>>() {}.type,
                        )
                        functions.getHttpsCallable("reportDuoDayProgress")
                            .withTimeout(20, TimeUnit.SECONDS)
                            .call(payload)
                            .await()
                        dao.deleteOp(op.id)
                    }

                    PendingOpKind.DUO_SWAP_GRANT.name -> {
                        val payload = gson.fromJson<Map<String, Any>>(
                            op.payload,
                            object : TypeToken<Map<String, Any>>() {}.type,
                        )
                        functions.getHttpsCallable("grantDuoSwap")
                            .withTimeout(20, TimeUnit.SECONDS)
                            .call(payload)
                            .await()
                        dao.deleteOp(op.id)
                    }

                    PendingOpKind.COMPLETION_UPSERT.name,
                    PendingOpKind.SHIELD_SPEND.name,
                    -> {
                        // Personal habit sync still rides the existing habitsJson blob for now;
                        // these ops are reserved so reconnect never invents a second writer.
                        dao.deleteOp(op.id)
                    }

                    else -> dao.deleteOp(op.id)
                }
            } catch (e: Exception) {
                hadFailure = true
                val attempts = op.attempts + 1
                val delayMs = (1L shl attempts.coerceAtMost(6)) * 30_000L
                dao.rescheduleOp(
                    id = op.id,
                    attempts = attempts,
                    nextAt = System.currentTimeMillis() + delayMs,
                    error = e.message?.take(200),
                )
                Log.w(TAG, "op ${op.kind} failed attempt=$attempts", e)
            }
        }
        return if (hadFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "StreakSyncWorker"
        private const val UNIQUE = "streak_outbox_drain"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<StreakSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
