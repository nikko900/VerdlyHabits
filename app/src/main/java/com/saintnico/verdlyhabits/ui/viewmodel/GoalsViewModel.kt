package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.engagement.EngagementKind
import com.saintnico.verdlyhabits.data.engagement.SessionEngagementTracker
import com.saintnico.verdlyhabits.data.local.goals.CheckInEntity
import com.saintnico.verdlyhabits.data.local.goals.CheckInStatus
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.data.local.goals.PaceProfile
import com.saintnico.verdlyhabits.data.repository.GoalsRepository
import com.saintnico.verdlyhabits.engine.GoalMilestoneFactory
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.notifications.GoalNotificationScheduler
import com.saintnico.verdlyhabits.notifications.GoalReminderScheduler
import com.saintnico.verdlyhabits.data.goals.GoalCreationDraft
import com.saintnico.verdlyhabits.data.goals.GoalCreationDraftStore
import com.saintnico.verdlyhabits.ui.screens.goals.creation.GoalCreationState
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class GoalCelebration {
    data class Milestone(val goalId: String, val milestoneId: String) : GoalCelebration()
    data class Completed(val goalId: String) : GoalCelebration()
}

class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalsRepository
    private val draftStore = GoalCreationDraftStore(application)
    val activeGoals: StateFlow<List<GoalEntity>>
    val creationDraft: StateFlow<GoalCreationDraft?>

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
        creationDraft = draftStore.draftFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )
    }

    fun persistCreationDraft(state: GoalCreationState, step: Int, returnAfterAddHabit: Boolean = false) {
        viewModelScope.launch {
            val draft = GoalCreationDraft.fromState(state, step, returnAfterAddHabit)
            if (draft.hasProgress || returnAfterAddHabit) {
                draftStore.save(draft)
            } else {
                draftStore.clear()
            }
        }
    }

    fun clearReturnAfterAddHabit() {
        viewModelScope.launch {
            val current = creationDraft.value ?: return@launch
            if (current.returnAfterAddHabit) {
                draftStore.save(current.copy(returnAfterAddHabit = false))
            }
        }
    }

    fun clearCreationDraft() {
        viewModelScope.launch { draftStore.clear() }
    }

    fun saveGoal(
        title: String,
        whyStatement: String,
        goalType: GoalType,
        targetValue: Float?,
        unit: String,
        startDate: Long,
        targetDate: Long,
        linkedHabitIds: List<String>,
        colorHex: String,
        tinyVersionText: String? = null,
        anchorCue: String? = null,
        softReviewDate: Long? = null,
        paceProfile: PaceProfile? = null,
        triggerText: String? = null,
        replacementText: String? = null,
        costPerOccurrence: Float? = null,
        costUnit: String? = null,
        floorCount: Int? = null,
        floorPeriodDays: Int? = null,
        promotedFromGoalId: String? = null,
    ) {
        viewModelScope.launch {
            val goalId = UUID.randomUUID().toString()
            val resolvedTargetDate = when (goalType) {
                GoalType.REACH -> targetDate
                GoalType.BUILD -> softReviewDate ?: (startDate + 56L * 24 * 60 * 60 * 1000)
                GoalType.QUIT, GoalType.MAINTAIN -> startDate + 365L * 24 * 60 * 60 * 1000
            }
            val goal = GoalEntity(
                id = goalId,
                title = title,
                whyStatement = whyStatement,
                goalType = goalType,
                targetValue = if (goalType == GoalType.REACH) targetValue else null,
                currentValue = 0f,
                unit = if (goalType == GoalType.REACH) unit else "",
                startDate = startDate,
                targetDate = resolvedTargetDate,
                completedDate = null,
                linkedHabitIds = linkedHabitIds.joinToString(","),
                colorHex = colorHex,
                energyLastUpdated = System.currentTimeMillis(),
                isArchived = false,
                tinyVersionText = tinyVersionText?.trim()?.ifEmpty { null },
                anchorCue = anchorCue?.trim()?.ifEmpty { null },
                softReviewDate = softReviewDate,
                paceProfile = paceProfile?.name,
                lastLapseAt = null,
                longestCleanStreakDays = 0,
                triggerText = triggerText?.trim()?.ifEmpty { null },
                replacementText = replacementText?.trim()?.ifEmpty { null },
                costPerOccurrence = costPerOccurrence,
                costUnit = costUnit?.trim()?.ifEmpty { null },
                floorCount = floorCount,
                floorPeriodDays = floorPeriodDays,
                consecutiveWeeksHeld = 0,
                lifetimeWeeksHeld = 0,
                promotedFromGoalId = promotedFromGoalId,
            )
            repository.insertGoal(goal)
            repository.insertMilestones(GoalMilestoneFactory.createFor(goal))
            clearCreationDraft()
        }
    }

    /** Recompute progress from habits, complete milestones, emit celebrations. */
    fun syncWithHabits(habits: List<HabitItem>) {
        viewModelScope.launch {
            val goals = repository.getActiveGoalsOnce()
            goals.forEach { goal ->
                val metrics = GoalProgressEngine.metrics(goal, habits)
                val value = GoalProgressEngine.currentValue(goal, habits)
                var updated = goal

                if (goal.goalType == GoalType.QUIT) {
                    val clean = metrics.cleanStreakDays
                    val longest = maxOf(goal.longestCleanStreakDays, clean)
                    if (longest != goal.longestCleanStreakDays || value != goal.currentValue) {
                        updated = goal.copy(
                            currentValue = value,
                            longestCleanStreakDays = longest,
                            energyLastUpdated = System.currentTimeMillis(),
                        )
                    }
                } else if (goal.goalType == GoalType.MAINTAIN) {
                    val (consecutive, lifetime) = GoalProgressEngine.recomputeWeeksHeld(goal, habits)
                    if (consecutive != goal.consecutiveWeeksHeld ||
                        lifetime != goal.lifetimeWeeksHeld ||
                        value != goal.currentValue
                    ) {
                        updated = goal.copy(
                            currentValue = value,
                            consecutiveWeeksHeld = consecutive,
                            lifetimeWeeksHeld = lifetime,
                            energyLastUpdated = System.currentTimeMillis(),
                        )
                    }
                } else if (value != goal.currentValue) {
                    updated = goal.copy(
                        currentValue = value,
                        energyLastUpdated = System.currentTimeMillis(),
                    )
                }

                if (updated != goal) {
                    repository.updateGoal(updated)
                }

                checkMilestones(updated, habits)

                if (metrics.canAutoComplete && updated.completedDate == null) {
                    repository.updateGoal(
                        updated.copy(
                            completedDate = System.currentTimeMillis(),
                            energyLastUpdated = System.currentTimeMillis(),
                        ),
                    )
                    _celebration.emit(GoalCelebration.Completed(updated.id))
                }
            }
        }
    }

    private suspend fun checkMilestones(goal: GoalEntity, habits: List<HabitItem>) {
        val milestones = repository.getMilestonesForGoalOnce(goal.id)
        val progress = GoalMilestoneFactory.progressForMilestones(goal, habits)
        milestones.forEach { m ->
            if (!m.isCompleted && progress >= m.targetPercent) {
                repository.updateMilestone(
                    m.copy(
                        isCompleted = true,
                        completedDate = System.currentTimeMillis(),
                    ),
                )
                _celebration.emit(GoalCelebration.Milestone(goal.id, m.id))
            }
        }
    }

    /** Explicit Quit lapse — never inferred from a missed check-in. */
    fun logQuitLapse(goalId: String) {
        viewModelScope.launch {
            val goal = repository.getGoalByIdOnce(goalId) ?: return@launch
            if (goal.goalType != GoalType.QUIT) return@launch
            val clean = GoalProgressEngine.cleanStreakDays(goal)
            val longest = maxOf(goal.longestCleanStreakDays, clean)
            repository.updateGoal(
                goal.copy(
                    lastLapseAt = System.currentTimeMillis(),
                    longestCleanStreakDays = longest,
                    currentValue = 0f,
                    energyLastUpdated = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun promoteToMaintain(
        goalId: String,
        floorCount: Int = 3,
        floorPeriodDays: Int = 7,
    ) {
        viewModelScope.launch {
            val goal = repository.getGoalByIdOnce(goalId) ?: return@launch
            if (goal.goalType != GoalType.BUILD && goal.goalType != GoalType.REACH) return@launch
            repository.updateGoal(
                goal.copy(
                    goalType = GoalType.MAINTAIN,
                    floorCount = floorCount,
                    floorPeriodDays = floorPeriodDays,
                    targetValue = null,
                    unit = "",
                    completedDate = null,
                    promotedFromGoalId = goalId,
                    consecutiveWeeksHeld = 0,
                    lifetimeWeeksHeld = 0,
                    energyLastUpdated = System.currentTimeMillis(),
                ),
            )
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
            SessionEngagementTracker.recordEngagement(getApplication(), EngagementKind.GOAL_CHECKIN)
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

    /** Archive with a reflective note — used when the user chooses to step back. */
    fun endGoalWithReason(goalId: String, reason: String) {
        viewModelScope.launch {
            val trimmed = reason.trim()
            if (trimmed.isNotEmpty()) {
                repository.insertCheckIn(
                    CheckInEntity(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        date = System.currentTimeMillis(),
                        status = CheckInStatus.FELL_BEHIND,
                        note = "Ended: $trimmed",
                    ),
                )
            }
            val goal = repository.getGoalByIdOnce(goalId) ?: return@launch
            repository.updateGoal(goal.copy(isArchived = true))
        }
    }

    fun goalById(goalId: String): Flow<GoalEntity?> = repository.getGoalById(goalId)

    fun milestonesForGoal(goalId: String): Flow<List<MilestoneEntity>> =
        repository.getMilestonesForGoal(goalId)

    fun progressFor(goal: GoalEntity, habits: List<HabitItem>): Float =
        GoalProgressEngine.progressFraction(goal, habits)

    fun metricsFor(goal: GoalEntity, habits: List<HabitItem>) =
        GoalProgressEngine.metrics(goal, habits)

    fun refreshGoalReminders() {
        GoalReminderScheduler.scheduleAll(getApplication())
    }
}
