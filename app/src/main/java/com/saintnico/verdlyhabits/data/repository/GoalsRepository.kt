package com.saintnico.verdlyhabits.data.repository

import com.saintnico.verdlyhabits.data.local.goals.CheckInEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalsDao
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class GoalsRepository(private val goalsDao: GoalsDao) {

    fun getActiveGoals(): Flow<List<GoalEntity>> = goalsDao.getActiveGoals()

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
}
