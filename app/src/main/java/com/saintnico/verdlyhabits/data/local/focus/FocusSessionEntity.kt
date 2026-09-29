package com.saintnico.verdlyhabits.data.local.focus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val habitId: String?,
    val durationMinutes: Int,
    val completedAt: Long,
    val xpEarned: Int
)
