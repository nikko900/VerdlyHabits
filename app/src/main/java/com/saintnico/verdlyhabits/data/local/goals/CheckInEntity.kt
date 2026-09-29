package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CheckInStatus {
    ON_TRACK, FELL_BEHIND, CRUSHED_IT
}

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val date: Long,
    val status: CheckInStatus,
    val note: String
)
