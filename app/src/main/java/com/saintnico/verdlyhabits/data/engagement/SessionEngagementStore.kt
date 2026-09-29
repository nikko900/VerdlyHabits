package com.saintnico.verdlyhabits.data.engagement

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionEngagementStore: DataStore<Preferences> by preferencesDataStore(
    name = "verdly_session_engagement",
)

/**
 * Persists user-session signals: when they last opened the app, what they were doing,
 * and whether Welcome Back was already shown for a given absence window.
 */
class SessionEngagementStore(private val context: Context) {

    companion object {
        private val LAST_FOREGROUND_AT = longPreferencesKey("last_foreground_at")
        private val LAST_ENGAGEMENT_AT = longPreferencesKey("last_engagement_at")
        private val LAST_ENGAGEMENT_KIND = stringPreferencesKey("last_engagement_kind")
        private val SESSION_COUNT = intPreferencesKey("session_count")
        /** Epoch ms of the absence start we already welcomed back from. */
        private val WELCOME_BACK_ACK_ABSENCE = longPreferencesKey("welcome_back_ack_absence")
    }

    val snapshot: Flow<SessionEngagementSnapshot> = context.sessionEngagementStore.data.map { prefs ->
        val lastForeground = prefs[LAST_FOREGROUND_AT] ?: 0L
        val now = System.currentTimeMillis()
        val daysAway = if (lastForeground > 0L) {
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - lastForeground)
        } else {
            0L
        }
        val ackAbsence = prefs[WELCOME_BACK_ACK_ABSENCE] ?: 0L
        val gapTier = if (daysAway >= EngagementGapTier.WELCOME_BACK_THRESHOLD_DAYS) {
            EngagementGapTier.fromDaysAway(daysAway)
        } else {
            null
        }
        val shouldShow = gapTier != null &&
            lastForeground > 0L &&
            ackAbsence != lastForeground

        SessionEngagementSnapshot(
            daysAway = daysAway,
            gapTier = gapTier,
            lastEngagementKind = EngagementKind.fromWire(prefs[LAST_ENGAGEMENT_KIND]),
            lastEngagementAt = prefs[LAST_ENGAGEMENT_AT] ?: 0L,
            sessionCount = prefs[SESSION_COUNT] ?: 0,
            shouldShowWelcomeBack = shouldShow,
            absenceEpoch = lastForeground,
        )
    }

    /**
     * Call on every app foreground. Returns the snapshot *before* this open is recorded
     * so callers can evaluate gap tier against the previous session.
     */
    suspend fun recordForeground(): SessionEngagementSnapshot {
        var previousSnapshot = SessionEngagementSnapshot()
        context.sessionEngagementStore.edit { prefs ->
            val previousForeground = prefs[LAST_FOREGROUND_AT] ?: 0L
            val now = System.currentTimeMillis()
            val daysAway = if (previousForeground > 0L) {
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - previousForeground)
            } else {
                0L
            }
            val ackAbsence = prefs[WELCOME_BACK_ACK_ABSENCE] ?: 0L
            val gapTier = if (daysAway >= EngagementGapTier.WELCOME_BACK_THRESHOLD_DAYS) {
                EngagementGapTier.fromDaysAway(daysAway)
            } else {
                null
            }
            previousSnapshot = SessionEngagementSnapshot(
                daysAway = daysAway,
                gapTier = gapTier,
                lastEngagementKind = EngagementKind.fromWire(prefs[LAST_ENGAGEMENT_KIND]),
                lastEngagementAt = prefs[LAST_ENGAGEMENT_AT] ?: 0L,
                sessionCount = prefs[SESSION_COUNT] ?: 0,
                shouldShowWelcomeBack = gapTier != null &&
                    previousForeground > 0L &&
                    ackAbsence != previousForeground,
                absenceEpoch = previousForeground,
            )
            prefs[LAST_FOREGROUND_AT] = now
            prefs[SESSION_COUNT] = (prefs[SESSION_COUNT] ?: 0) + 1
            if (prefs[LAST_ENGAGEMENT_AT] == null) {
                prefs[LAST_ENGAGEMENT_AT] = now
                prefs[LAST_ENGAGEMENT_KIND] = EngagementKind.APP_OPEN.wireValue
            }
        }
        return previousSnapshot
    }

    suspend fun recordEngagement(kind: EngagementKind) {
        val now = System.currentTimeMillis()
        context.sessionEngagementStore.edit { prefs ->
            prefs[LAST_ENGAGEMENT_AT] = now
            prefs[LAST_ENGAGEMENT_KIND] = kind.wireValue
        }
    }

    suspend fun acknowledgeWelcomeBack(absenceEpoch: Long) {
        context.sessionEngagementStore.edit { prefs ->
            prefs[WELCOME_BACK_ACK_ABSENCE] = absenceEpoch
        }
    }

    suspend fun clearAll() {
        context.sessionEngagementStore.edit { it.clear() }
    }
}
