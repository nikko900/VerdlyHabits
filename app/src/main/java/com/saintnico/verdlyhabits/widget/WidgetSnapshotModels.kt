package com.saintnico.verdlyhabits.widget

data class HabitWidgetRow(
    val id: String,
    val title: String,
    val streak: Int,
    val isCompletedToday: Boolean,
    val colorArgb: Long,
    val isPaused: Boolean,
)

data class HabitsWidgetSnapshot(
    val habits: List<HabitWidgetRow> = emptyList(),
    val doneToday: Int = 0,
    val totalToday: Int = 0,
    val updatedAtMillis: Long = 0L,
)

data class LeaderboardWidgetRow(
    val uid: String,
    val name: String,
    val streak: Int,
    val score: Int = streak,
    val scoreLabel: String = "streak",
    val reactionPoints: Int = 0,
    val rank: Int,
    val isMe: Boolean,
    val doneToday: Boolean,
)

data class LeaderboardWidgetSnapshot(
    val challengeId: String = "",
    val challengeTitle: String = "",
    val myRank: Int = 0,
    val myUid: String = "",
    val rows: List<LeaderboardWidgetRow> = emptyList(),
    val updatedAtMillis: Long = 0L,
)
