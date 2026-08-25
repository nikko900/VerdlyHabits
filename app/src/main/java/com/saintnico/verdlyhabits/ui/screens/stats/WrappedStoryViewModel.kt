package com.saintnico.verdlyhabits.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.engine.WrappedInsightsEngine
import com.saintnico.verdlyhabits.engine.WrappedSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WrappedStoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<WrappedSnapshot?>(null)
    val uiState: StateFlow<WrappedSnapshot?> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val habits = runCatching { HabitStoreReader.loadHabits(context) }.getOrDefault(emptyList())
            _uiState.value = runCatching {
                WrappedInsightsEngine.computeAndPersist(habits)
            }.getOrElse {
                WrappedInsightsEngine.compute(habits)
            }
        }
    }
}
