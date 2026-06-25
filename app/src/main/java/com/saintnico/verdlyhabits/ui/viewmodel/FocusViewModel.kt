package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.local.AppDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppDataStore(application)

    val sessions: StateFlow<List<AppDataStore.FocusSession>> = store.focusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusMinutes: StateFlow<Int> = store.totalFocusMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveSession(habitId: String?, durationMinutes: Int, completed: Boolean, xpEarned: Int) {
        viewModelScope.launch {
            store.saveFocusSession(
                AppDataStore.FocusSession(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    startedAt = System.currentTimeMillis(),
                    durationMinutes = durationMinutes,
                    completed = completed,
                    xpEarned = xpEarned
                )
            )
        }
    }
}
