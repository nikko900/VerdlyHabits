package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.analytics.AnalyticsTracker
import com.saintnico.verdlyhabits.data.engagement.ComebackCopyBank
import com.saintnico.verdlyhabits.data.engagement.ComebackDestination
import com.saintnico.verdlyhabits.data.engagement.EngagementGapTier
import com.saintnico.verdlyhabits.data.engagement.SessionEngagementSnapshot
import com.saintnico.verdlyhabits.data.engagement.SessionEngagementTracker
import com.saintnico.verdlyhabits.data.streak.StreakRepository
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WelcomeBackUiState(
    val visible: Boolean = false,
    val gapTier: EngagementGapTier? = null,
    val headline: String = "",
    val body: String = "",
    val contextLine: String? = null,
    val primaryLabel: String = "",
    val destination: ComebackDestination = ComebackDestination.HOME_BROWSE,
    val suggestedHabit: HabitItem? = null,
    val absenceEpoch: Long = 0L,
    val analyticsLogged: Boolean = false,
)

class ReEngagementViewModel(application: Application) : AndroidViewModel(application) {

    private val engagementStore = SessionEngagementTracker.get(application)
    private val streakRepo = StreakRepository.get(application)

    private val _state = MutableStateFlow(WelcomeBackUiState())
    val state: StateFlow<WelcomeBackUiState> = _state.asStateFlow()

    /** Pending tab navigation for MainShell to consume after Welcome Back dismisses. */
    private val _pendingDestination = MutableStateFlow<ComebackDestination?>(null)
    val pendingDestination: StateFlow<ComebackDestination?> = _pendingDestination.asStateFlow()

    private var lastEvaluatedAbsence: Long = 0L

    fun evaluateOnForeground(habits: List<HabitItem>, hasActiveGoals: Boolean) {
        viewModelScope.launch {
            val snapshot = SessionEngagementTracker.onForeground(getApplication())
            buildWelcomeBackState(snapshot, habits, hasActiveGoals)
        }
    }

    private fun buildWelcomeBackState(
        snapshot: SessionEngagementSnapshot,
        habits: List<HabitItem>,
        hasActiveGoals: Boolean,
    ) {
        if (!snapshot.shouldShowWelcomeBack || snapshot.gapTier == null) {
            _state.update { it.copy(visible = false) }
            return
        }
        if (snapshot.absenceEpoch == lastEvaluatedAbsence && _state.value.visible) return
        lastEvaluatedAbsence = snapshot.absenceEpoch

        val today = LocalDate.now()
        val visibleHabits = habits.filter { !it.isArchived && !it.isPaused }
        val dueToday = visibleHabits.filter {
            HabitScheduling.isDueOn(it.frequency, it.customDaysMask, today)
        }
        val incompleteToday = dueToday.filter { !it.isCompleted }
        val suggested = incompleteToday.minWithOrNull(compareBy { it.difficulty.xp })
        val hasRepairable = streakRepo.snapshot.value.repairable.isNotEmpty()

        val copy = ComebackCopyBank.pick(
            tier = snapshot.gapTier,
            lastKind = snapshot.lastEngagementKind,
            hasIncompleteHabitToday = suggested != null,
            hasActiveGoals = hasActiveGoals,
            hasRepairableStreak = hasRepairable,
            seed = snapshot.absenceEpoch,
        )

        val destination = when (copy.destination) {
            ComebackDestination.HOME_COMPLETE_HABIT ->
                if (suggested != null) ComebackDestination.HOME_COMPLETE_HABIT
                else ComebackDestination.HOME_BROWSE
            else -> copy.destination
        }

        _state.update {
            WelcomeBackUiState(
                visible = true,
                gapTier = snapshot.gapTier,
                headline = copy.headline,
                body = copy.body,
                contextLine = copy.contextLine,
                primaryLabel = copy.primaryLabel,
                destination = destination,
                suggestedHabit = suggested,
                absenceEpoch = snapshot.absenceEpoch,
                analyticsLogged = false,
            )
        }
    }

    fun onWelcomeBackShown() {
        val current = _state.value
        if (current.analyticsLogged || !current.visible) return
        AnalyticsTracker.log(
            getApplication(),
            AnalyticsTracker.ReEngagement.SHOWN,
            mapOf(
                "gap_tier" to current.gapTier?.name,
                "destination" to current.destination.name,
            ),
        )
        _state.update { it.copy(analyticsLogged = true) }
    }

    fun onPrimaryAction() {
        val current = _state.value
        viewModelScope.launch {
            SessionEngagementTracker.acknowledgeWelcomeBack(
                getApplication(),
                current.absenceEpoch,
            )
            AnalyticsTracker.log(
                getApplication(),
                AnalyticsTracker.ReEngagement.COMPLETED,
                mapOf(
                    "gap_tier" to current.gapTier?.name,
                    "destination" to current.destination.name,
                    "habit_id" to current.suggestedHabit?.id,
                ),
            )
            _pendingDestination.value = current.destination
            _state.update { it.copy(visible = false) }
        }
    }

    fun onDismiss() {
        val current = _state.value
        viewModelScope.launch {
            SessionEngagementTracker.acknowledgeWelcomeBack(
                getApplication(),
                current.absenceEpoch,
            )
            AnalyticsTracker.log(
                getApplication(),
                AnalyticsTracker.ReEngagement.DISMISSED,
                mapOf("gap_tier" to current.gapTier?.name),
            )
            _state.update { it.copy(visible = false) }
        }
    }

    fun consumePendingDestination(): ComebackDestination? {
        val dest = _pendingDestination.value
        _pendingDestination.value = null
        return dest
    }
}
