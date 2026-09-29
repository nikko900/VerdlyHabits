package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.ui.screens.home.HabitItem

/**
 * Lightweight event bus so trophy tracking can fire from screens and view-models
 * that do not hold [com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel].
 */
object TrophyBus {
    @Volatile
    private var listener: ((TrophyEvent) -> Unit)? = null

    fun bind(block: (TrophyEvent) -> Unit) {
        listener = block
    }

    fun unbind() {
        listener = null
    }

    fun emit(event: TrophyEvent) {
        listener?.invoke(event)
    }
}

sealed class TrophyEvent {
    data class HabitsBound(val habits: List<HabitItem>) : TrophyEvent()
    data object HabitCreated : TrophyEvent()
    data class HabitCompleted(
        val habitId: String,
        val streak: Int,
        val previousStreak: Int,
        val isFirstCompletion: Boolean,
        val difficultyName: String,
        val withinCommitmentWindow: Boolean,
        val hasPhotoProof: Boolean,
        val reminderEnabled: Boolean,
        val reminderTime: String?,
        val plantedAt: Long,
        val habits: List<HabitItem>,
    ) : TrophyEvent()
    data object StatsOpened : TrophyEvent()
    data object AppOpened : TrophyEvent()
    data object LanguageChanged : TrophyEvent()
    data class FocusDone(val minutes: Int) : TrophyEvent()
    data object GoalCreated : TrophyEvent()
    data object GoalCompleted : TrophyEvent()
    data class FriendsCount(val count: Int) : TrophyEvent()
    data object ProfileViewedOther : TrophyEvent()
    data class ProfileViewsReceived(val count: Int) : TrophyEvent()
    data object Shared : TrophyEvent()
    data class DuoStreak(val days: Int) : TrophyEvent()
    data object ChallengeJoined : TrophyEvent()
    data class ChallengeWins(val count: Int) : TrophyEvent()
    data object GardenOpened : TrophyEvent()
    data object HallOpened : TrophyEvent()
    data class OathLocked(val amount: Int) : TrophyEvent()
    data class OathConverted(val amount: Int) : TrophyEvent()
    data class OathForfeited(val amount: Int) : TrophyEvent()
    data class CoinsGifted(val amount: Int) : TrophyEvent()
    data object CosmeticOwned : TrophyEvent()
}
