package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestones")
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val title: String,
    val targetPercent: Float,
    val completedDate: Long?,
    val isCompleted: Boolean
)
