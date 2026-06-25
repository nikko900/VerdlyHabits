package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalType {
    BUILD, REACH, QUIT, MAINTAIN
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val whyStatement: String,
    val goalType: GoalType,
    val targetValue: Float?,
    val currentValue: Float,
    val unit: String,
    val startDate: Long,
    val targetDate: Long,
    val completedDate: Long?,
    val linkedHabitIds: String, // comma-separated
    val colorHex: String,
    val energyLastUpdated: Long,
    val isArchived: Boolean
)
