package com.saintnico.verdlyhabits.data.local.goals

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalsDao {
    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY startDate DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun getGoalById(goalId: String): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoal(goal: GoalEntity)

    @Update
    fun updateGoal(goal: GoalEntity)

    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY targetPercent ASC")
    fun getMilestonesForGoal(goalId: String): Flow<List<MilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMilestones(milestones: List<MilestoneEntity>)

    @Update
    fun updateMilestone(milestone: MilestoneEntity)

    @Query("SELECT * FROM check_ins WHERE goalId = :goalId ORDER BY date DESC")
    fun getCheckInsForGoal(goalId: String): Flow<List<CheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCheckIn(checkIn: CheckInEntity)
}
