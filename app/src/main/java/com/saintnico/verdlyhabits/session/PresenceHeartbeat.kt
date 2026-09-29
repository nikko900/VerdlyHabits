package com.saintnico.verdlyhabits.session

import android.content.Context
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.flow.first

/**
 * Lightweight last-active ping while the app is in the foreground.
 * Writes are skipped when the user turned Activity sharing off.
 */
object PresenceHeartbeat {
    private const val MIN_INTERVAL_MS = 90_000L
    @Volatile
    private var lastPingAt = 0L

    suspend fun ping(context: Context, force: Boolean = false) {
        val prefs = ThemePreference(context.applicationContext)
        val share = prefs.shareLastActive.first()
        if (!share) return
        val now = System.currentTimeMillis()
        if (!force && now - lastPingAt < MIN_INTERVAL_MS) return
        lastPingAt = now
        runCatching { UserRepository().touchLastActive() }
    }

    fun resetThrottle() {
        lastPingAt = 0L
    }
}
