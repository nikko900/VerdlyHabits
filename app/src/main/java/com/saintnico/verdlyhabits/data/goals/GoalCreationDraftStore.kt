package com.saintnico.verdlyhabits.data.goals

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.saintnico.verdlyhabits.preferences.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val GOAL_CREATION_DRAFT = stringPreferencesKey("goal_creation_draft_v1")

class GoalCreationDraftStore(private val context: Context) {

    val draftFlow: Flow<GoalCreationDraft?> = context.dataStore.data.map { prefs ->
        prefs[GOAL_CREATION_DRAFT]?.let { GoalCreationDraft.fromJson(it) }
    }

    suspend fun save(draft: GoalCreationDraft) {
        context.dataStore.edit { prefs ->
            prefs[GOAL_CREATION_DRAFT] = draft.copy(updatedAt = System.currentTimeMillis()).toJson()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(GOAL_CREATION_DRAFT) }
    }
}
