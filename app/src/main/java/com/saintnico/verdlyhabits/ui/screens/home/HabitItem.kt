package com.saintnico.verdlyhabits.ui.screens.home

import androidx.compose.ui.graphics.vector.ImageVector
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.ReminderWindow

data class HabitItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val streak: Int = 0,
    var isCompleted: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    /** Optional second daily alarm (same day), e.g. evening nudge. */
    val reminderTime2: String? = null,
    val completedDates: Set<String> = emptySet(),
    val color: Long = 0xFF4CAF50, // default green
    val plantedAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val completionProofs: Map<String, String> = emptyMap(), // Date string -> Photo URL
    // ── Phase 1 overhaul fields ───────────────────────────────────────────
    val difficulty: Difficulty = Difficulty.EASY,
    val category: HabitCategory = HabitCategory.OTHER,
    val reminderWindow: ReminderWindow = ReminderWindow.EXACT,
    val linkedHabitId: String? = null, // habit stacking — fire after this one completes
    val isFavoriteFocus: Boolean = false, // user-pinned habit for the dashboard "Today's Focus" card
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    /** Mon..Sun, seven `0`/`1` chars when [frequency] is CUSTOM. */
    val customDaysMask: String? = null,
)
