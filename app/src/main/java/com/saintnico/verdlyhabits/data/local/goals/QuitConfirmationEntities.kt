package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ConfirmationState {
    STILL_CLEAR,
    AT_RISK,
    UNCONFIRMED,
}

enum class ConfirmationType {
    CONFIRMED,
    SLIP,
    SELF_REPORTED_RETROACTIVE,
    CONFIRMATION_UNCERTAIN,
    SOFT_RESET,
    GOAL_CREATED,
}

enum class ConfirmationSource {
    DAILY_TAP,
    NOTIFICATION_ACTION,
    CATCHUP_FLOW,
    GOAL_CREATED,
}

enum class SilentGapResolution {
    PENDING,
    YES_CLEAR,
    NO_SLIPPED,
    UNSURE_KEEP,
    UNSURE_RESET,
}

@Entity(
    tableName = "goal_confirmations",
    indices = [Index("goalId"), Index(value = ["goalId", "date"])],
)
data class GoalConfirmationEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    /** ISO `yyyy-MM-dd` of the calendar day this row applies to. */
    val date: String,
    val createdAt: Long,
    val type: String,
    val source: String,
)

@Entity(
    tableName = "goal_silent_gaps",
    indices = [Index("goalId"), Index("resolution")],
)
data class GoalSilentGapEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val gapStartDate: String,
    val gapEndDate: String?,
    val resolution: String,
    val resolvedAt: Long?,
)
