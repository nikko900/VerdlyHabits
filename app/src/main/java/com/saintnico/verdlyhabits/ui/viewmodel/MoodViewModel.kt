package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.local.AppDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class MoodViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppDataStore(application)

    val moods: StateFlow<List<AppDataStore.MoodEntry>> = store.moods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasLoggedToday: StateFlow<Boolean> = moods.map { list ->
        val today = LocalDate.now()
        list.any { entry ->
            Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun logMood(mood: Int, tags: List<String>, note: String?) {
        viewModelScope.launch {
            store.saveMood(
                AppDataStore.MoodEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    mood = mood,
                    tags = tags,
                    note = note
                )
            )
        }
    }
}
