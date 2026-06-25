package com.saintnico.verdlyhabits.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.notifications.ReminderScheduler
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.preferences.dataStore
import com.saintnico.verdlyhabits.widget.WidgetSnapshotStore
import kotlinx.coroutines.flow.first

/**
 * Keeps local caches aligned with the active Firebase account.
 * Wipes user-scoped data when the signed-in uid changes so a different Google
 * account never inherits habits, stats, goals, or profile from the previous user.
 */
object AccountSessionCoordinator {

    private val LAST_ACCOUNT_UID = stringPreferencesKey("last_account_uid")
    private val HABITS_KEY = stringPreferencesKey("habits_list")

    suspend fun onUserSignedIn(context: Context, uid: String): Boolean {
        val previous = readLastUid(context)
        val switched = previous != uid
        if (switched) {
            wipeLocalUserData(context)
        }
        context.dataStore.edit { it[LAST_ACCOUNT_UID] = uid }
        return switched
    }

    suspend fun onUserSignedOut(context: Context) {
        wipeLocalUserData(context)
        context.dataStore.edit { it.remove(LAST_ACCOUNT_UID) }
    }

    private suspend fun readLastUid(context: Context): String? =
        context.dataStore.data.first()[LAST_ACCOUNT_UID]

    suspend fun wipeLocalUserData(context: Context) {
        AppDataStore(context).clearAllData()
        ThemePreference(context).clearProfile()
        context.dataStore.edit { prefs ->
            prefs.remove(HABITS_KEY)
        }
        wipeRoomDatabase(context)
        WidgetSnapshotStore(context).clear()
        ReminderScheduler.scheduleAll(context.applicationContext)
    }

    private fun wipeRoomDatabase(context: Context) {
        AppDatabase.resetInstance()
        context.deleteDatabase("verdly_database")
    }
}
