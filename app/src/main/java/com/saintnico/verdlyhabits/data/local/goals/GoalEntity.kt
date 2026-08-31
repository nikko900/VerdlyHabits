package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalType {
    BUILD, REACH, QUIT, MAINTAIN
}

/** Reach pacing preference — seeds mid-goal support, not a hard schedule. */
enum class PaceProfile {
    FRONT_LOAD, STEADY, BACK_LOAD
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Shared private line: identity / stakes / cost / standard — label depends on [goalType]. */
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
    val isArchived: Boolean,
    // ── BUILD ──────────────────────────────────────────────────────────────
    val tinyVersionText: String? = null,
    val anchorCue: String? = null,
    /** Soft review horizon (not a hard deadline). Null = no calendar pressure. */
    val softReviewDate: Long? = null,
    // ── REACH ──────────────────────────────────────────────────────────────
    val paceProfile: String? = null,
    // ── QUIT ───────────────────────────────────────────────────────────────
    val lastLapseAt: Long? = null,
    val longestCleanStreakDays: Int = 0,
    val triggerText: String? = null,
    val replacementText: String? = null,
    val costPerOccurrence: Float? = null,
    val costUnit: String? = null,
    // ── MAINTAIN ───────────────────────────────────────────────────────────
    /** Minimum completions required inside [floorPeriodDays]. */
    val floorCount: Int? = null,
    /** Window length in days for the floor (typically 7). */
    val floorPeriodDays: Int? = null,
    val consecutiveWeeksHeld: Int = 0,
    val lifetimeWeeksHeld: Int = 0,
    val promotedFromGoalId: String? = null,
)
