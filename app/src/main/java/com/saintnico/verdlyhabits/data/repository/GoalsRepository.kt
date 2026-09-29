package com.saintnico.verdlyhabits.data.repository

import com.saintnico.verdlyhabits.data.local.goals.CheckInEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalConfirmationEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalSilentGapEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalsDao
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.data.local.goals.SilentGapResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class GoalsRepository(private val goalsDao: GoalsDao) {

    fun getActiveGoals(): Flow<List<GoalEntity>> = goalsDao.getActiveGoals()

    fun getArchivedGoals(): Flow<List<GoalEntity>> = goalsDao.getArchivedGoals()

    fun getGoalById(goalId: String): Flow<GoalEntity?> = goalsDao.getGoalById(goalId)

    suspend fun insertGoal(goal: GoalEntity) = withContext(Dispatchers.IO) { goalsDao.insertGoal(goal) }

    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) { goalsDao.updateGoal(goal) }

    fun getMilestonesForGoal(goalId: String): Flow<List<MilestoneEntity>> = goalsDao.getMilestonesForGoal(goalId)

    suspend fun getMilestonesForGoalOnce(goalId: String): List<MilestoneEntity> =
        withContext(Dispatchers.IO) { goalsDao.getMilestonesForGoal(goalId).first() }

    suspend fun getActiveGoalsOnce(): List<GoalEntity> =
        withContext(Dispatchers.IO) { goalsDao.getActiveGoals().first() }

    suspend fun getGoalByIdOnce(goalId: String): GoalEntity? =
        withContext(Dispatchers.IO) { goalsDao.getGoalById(goalId).first() }

    suspend fun insertMilestones(milestones: List<MilestoneEntity>) = withContext(Dispatchers.IO) { goalsDao.insertMilestones(milestones) }

    suspend fun updateMilestone(milestone: MilestoneEntity) = withContext(Dispatchers.IO) { goalsDao.updateMilestone(milestone) }

    fun getCheckInsForGoal(goalId: String): Flow<List<CheckInEntity>> = goalsDao.getCheckInsForGoal(goalId)

    suspend fun insertCheckIn(checkIn: CheckInEntity) = withContext(Dispatchers.IO) { goalsDao.insertCheckIn(checkIn) }

    suspend fun deleteMilestonesForGoal(goalId: String) =
        withContext(Dispatchers.IO) { goalsDao.deleteMilestonesForGoal(goalId) }

    suspend fun countCheckInsForGoal(goalId: String): Int =
        withContext(Dispatchers.IO) { goalsDao.countCheckInsForGoal(goalId) }

    suspend fun insertConfirmation(row: GoalConfirmationEntity) =
        withContext(Dispatchers.IO) { goalsDao.insertConfirmation(row) }

    suspend fun confirmationsForGoal(goalId: String): List<GoalConfirmationEntity> =
        withContext(Dispatchers.IO) { goalsDao.getConfirmationsForGoal(goalId) }

    suspend fun allConfirmations(): List<GoalConfirmationEntity> =
        withContext(Dispatchers.IO) { goalsDao.getAllConfirmations() }

    suspend fun insertSilentGap(row: GoalSilentGapEntity) =
        withContext(Dispatchers.IO) { goalsDao.insertSilentGap(row) }

    suspend fun updateSilentGap(row: GoalSilentGapEntity) =
        withContext(Dispatchers.IO) { goalsDao.updateSilentGap(row) }

    suspend fun pendingSilentGaps(): List<GoalSilentGapEntity> =
        withContext(Dispatchers.IO) {
            goalsDao.getSilentGapsByResolution(SilentGapResolution.PENDING.name)
        }

    suspend fun silentGapsForGoal(goalId: String): List<GoalSilentGapEntity> =
        withContext(Dispatchers.IO) { goalsDao.getSilentGapsForGoal(goalId) }

    suspend fun allSilentGaps(): List<GoalSilentGapEntity> =
        withContext(Dispatchers.IO) { goalsDao.getAllSilentGaps() }
}
