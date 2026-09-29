package com.saintnico.verdlyhabits.data.engagement

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single entry point for recording how the user interacts with Verdly as a product —
 * app opens, habit lifecycle, goals, coins, focus, etc.
 */
object SessionEngagementTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var store: SessionEngagementStore? = null

    fun get(context: Context): SessionEngagementStore {
        return store ?: synchronized(this) {
            store ?: SessionEngagementStore(context.applicationContext).also { store = it }
        }
    }

    suspend fun onForeground(context: Context): SessionEngagementSnapshot {
        return get(context).recordForeground()
    }

    fun recordEngagement(context: Context, kind: EngagementKind) {
        val appContext = context.applicationContext
        scope.launch {
            get(appContext).recordEngagement(kind)
        }
    }

    suspend fun acknowledgeWelcomeBack(context: Context, absenceEpoch: Long) {
        get(context).acknowledgeWelcomeBack(absenceEpoch)
    }
}
