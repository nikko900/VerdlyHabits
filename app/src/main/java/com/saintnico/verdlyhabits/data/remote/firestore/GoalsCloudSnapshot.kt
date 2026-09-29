package com.saintnico.verdlyhabits.data.remote.firestore

import com.saintnico.verdlyhabits.data.local.goals.CheckInEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalConfirmationEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalSilentGapEntity
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity

/** Full goals backup stored on the user document (mirrors habitsJson). */
data class GoalsCloudSnapshot(
    val goals: List<GoalEntity> = emptyList(),
    val milestones: List<MilestoneEntity> = emptyList(),
    val checkIns: List<CheckInEntity> = emptyList(),
    val confirmations: List<GoalConfirmationEntity>? = emptyList(),
    val silentGaps: List<GoalSilentGapEntity>? = emptyList(),
    val savedAt: Long = 0L,
)
