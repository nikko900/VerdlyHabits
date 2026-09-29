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

    @Query("SELECT * FROM goals WHERE isArchived = 1 ORDER BY startDate DESC")
    fun getArchivedGoals(): Flow<List<GoalEntity>>

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

    @Query("DELETE FROM milestones WHERE goalId = :goalId")
    fun deleteMilestonesForGoal(goalId: String)

    @Query("SELECT COUNT(*) FROM check_ins WHERE goalId = :goalId")
    fun countCheckInsForGoal(goalId: String): Int

    @Query("SELECT * FROM goals")
    fun getAllGoals(): List<GoalEntity>

    @Query("SELECT * FROM milestones")
    fun getAllMilestones(): List<MilestoneEntity>

    @Query("SELECT * FROM check_ins")
    fun getAllCheckIns(): List<CheckInEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertConfirmation(row: GoalConfirmationEntity)

    @Query("SELECT * FROM goal_confirmations WHERE goalId = :goalId ORDER BY date ASC, createdAt ASC")
    fun getConfirmationsForGoal(goalId: String): List<GoalConfirmationEntity>

    @Query("SELECT * FROM goal_confirmations")
    fun getAllConfirmations(): List<GoalConfirmationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSilentGap(row: GoalSilentGapEntity)

    @Update
    fun updateSilentGap(row: GoalSilentGapEntity)

    @Query("SELECT * FROM goal_silent_gaps WHERE goalId = :goalId ORDER BY gapStartDate DESC")
    fun getSilentGapsForGoal(goalId: String): List<GoalSilentGapEntity>

    @Query("SELECT * FROM goal_silent_gaps WHERE resolution = :resolution")
    fun getSilentGapsByResolution(resolution: String): List<GoalSilentGapEntity>

    @Query("SELECT * FROM goal_silent_gaps")
    fun getAllSilentGaps(): List<GoalSilentGapEntity>
}
