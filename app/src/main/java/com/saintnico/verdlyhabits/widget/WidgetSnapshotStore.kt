package com.saintnico.verdlyhabits.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore by preferencesDataStore(name = "widget_cache")

class WidgetSnapshotStore(private val context: Context) {
    private val gson = Gson()

    suspend fun saveHabits(snapshot: HabitsWidgetSnapshot) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_HABITS] = gson.toJson(snapshot)
        }
    }

    suspend fun saveLeaderboard(snapshot: LeaderboardWidgetSnapshot) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_LEADERBOARD] = gson.toJson(snapshot)
        }
    }

    suspend fun readHabits(): HabitsWidgetSnapshot {
        val json = context.widgetDataStore.data.first()[KEY_HABITS] ?: return HabitsWidgetSnapshot()
        return runCatching { gson.fromJson(json, HabitsWidgetSnapshot::class.java) }
            .getOrDefault(HabitsWidgetSnapshot())
    }

    suspend fun readLeaderboard(): LeaderboardWidgetSnapshot {
        val json = context.widgetDataStore.data.first()[KEY_LEADERBOARD] ?: return LeaderboardWidgetSnapshot()
        return runCatching { gson.fromJson(json, LeaderboardWidgetSnapshot::class.java) }
            .getOrDefault(LeaderboardWidgetSnapshot())
    }

    suspend fun clear() {
        context.widgetDataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_HABITS = stringPreferencesKey("habits_widget_snapshot")
        private val KEY_LEADERBOARD = stringPreferencesKey("leaderboard_widget_snapshot")
    }
}
