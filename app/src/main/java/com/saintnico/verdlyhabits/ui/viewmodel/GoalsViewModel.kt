package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.goals.CheckInEntity
import com.saintnico.verdlyhabits.data.local.goals.CheckInStatus
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.data.repository.GoalsRepository
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.notifications.GoalNotificationScheduler
import com.saintnico.verdlyhabits.notifications.GoalReminderScheduler
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class GoalCelebration {
    data class Milestone(val goalId: String, val milestoneId: String) : GoalCelebration()
    data class Completed(val goalId: String) : GoalCelebration()
}

class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalsRepository
    val activeGoals: StateFlow<List<GoalEntity>>

    private val _activeGoalsState = MutableStateFlow<List<GoalEntity>>(emptyList())
    private val _celebration = MutableSharedFlow<GoalCelebration>(extraBufferCapacity = 1)
    val celebration = _celebration.asSharedFlow()

    private val _weeklyCheckInGoalId = MutableStateFlow<String?>(null)
    val weeklyCheckInGoalId: StateFlow<String?> = _weeklyCheckInGoalId.asStateFlow()

    init {
        val goalsDao = AppDatabase.getDatabase(application).goalsDao()
        repository = GoalsRepository(goalsDao)
        activeGoals = _activeGoalsState.asStateFlow()
        GoalNotificationScheduler.scheduleAll(application)

        viewModelScope.launch {
            repository.getActiveGoals().collect { goals ->
                _activeGoalsState.value = goals
            }
        }
    }

    fun saveGoal(
        title: String,
        whyStatement: String,
        goalType: com.saintnico.verdlyhabits.data.local.goals.GoalType,
        targetValue: Float?,
        unit: String,
        startDate: Long,
        targetDate: Long,
        linkedHabitIds: List<String>,
        colorHex: String,
    ) {
        viewModelScope.launch {
            val goalId = UUID.randomUUID().toString()
            val goal = GoalEntity(
                id = goalId,
                title = title,
                whyStatement = whyStatement,
                goalType = goalType,
                targetValue = targetValue,
                currentValue = 0f,
                unit = unit,
                startDate = startDate,
                targetDate = targetDate,
                completedDate = null,
                linkedHabitIds = linkedHabitIds.joinToString(","),
                colorHex = colorHex,
                energyLastUpdated = System.currentTimeMillis(),
                isArchived = false,
            )
            repository.insertGoal(goal)

            val milestones = (1..4).map { i ->
                val percent = i * 25f
                MilestoneEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    title = when (i) {
                        1 -> "First quarter"
                        2 -> "Halfway there"
                        3 -> "Home stretch"
                        else -> "The finish line"
                    },
                    targetPercent = percent,
                    completedDate = null,
                    isCompleted = false,
                )
            }
            repository.insertMilestones(milestones)
        }
    }

    /** Recompute progress from habits, complete milestones, emit celebrations. */
    fun syncWithHabits(habits: List<HabitItem>) {
        viewModelScope.launch {
            val goals = repository.getActiveGoalsOnce()
            goals.forEach { goal ->
                val fraction = GoalProgressEngine.progressFraction(goal, habits)
                val value = GoalProgressEngine.currentValue(goal, habits)
                if (value != goal.currentValue) {
                    repository.updateGoal(
                        goal.copy(
                            currentValue = value,
                            energyLastUpdated = System.currentTimeMillis(),
                        ),
                    )
                }
                checkMilestones(goal.id, fraction)
                if (fraction >= 1f && goal.completedDate == null) {
                    repository.updateGoal(
                        goal.copy(
                            currentValue = value,
                            energyLastUpdated = System.currentTimeMillis(),
                            completedDate = System.currentTimeMillis(),
                            isArchived = false,
                        ),
                    )
                    _celebration.emit(GoalCelebration.Completed(goal.id))
                }
            }
        }
    }

    private suspend fun checkMilestones(goalId: String, fraction: Float) {
        val milestones = repository.getMilestonesForGoalOnce(goalId)
        val percent = (fraction * 100f).coerceIn(0f, 100f)
        milestones.forEach { m ->
            if (!m.isCompleted && percent >= m.targetPercent) {
                repository.updateMilestone(
                    m.copy(
                        isCompleted = true,
                        completedDate = System.currentTimeMillis(),
                    ),
                )
                _celebration.emit(GoalCelebration.Milestone(goalId, m.id))
            }
        }
    }

    fun submitWeeklyCheckIn(goalId: String, status: CheckInStatus, note: String) {
        viewModelScope.launch {
            repository.insertCheckIn(
                CheckInEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    date = System.currentTimeMillis(),
                    status = status,
                    note = note.trim(),
                ),
            )
            _weeklyCheckInGoalId.value = null
        }
    }

    fun requestWeeklyCheckIn(goalId: String? = null) {
        viewModelScope.launch {
            val id = goalId ?: repository.getActiveGoalsOnce().firstOrNull()?.id
            _weeklyCheckInGoalId.value = id
        }
    }

    fun dismissWeeklyCheckIn() {
        _weeklyCheckInGoalId.value = null
    }

    fun archiveGoal(goalId: String) {
        viewModelScope.launch {
            val goal = repository.getGoalByIdOnce(goalId) ?: return@launch
            repository.updateGoal(goal.copy(isArchived = true))
        }
    }

    fun goalById(goalId: String): Flow<GoalEntity?> = repository.getGoalById(goalId)

    fun milestonesForGoal(goalId: String): Flow<List<MilestoneEntity>> =
        repository.getMilestonesForGoal(goalId)

    fun progressFor(goal: GoalEntity, habits: List<HabitItem>): Float =
        GoalProgressEngine.progressFraction(goal, habits)

    fun refreshGoalReminders() {
        GoalReminderScheduler.scheduleAll(getApplication())
    }
}
