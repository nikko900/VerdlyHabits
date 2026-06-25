package com.saintnico.verdlyhabits.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.preferences.dataStore
import com.saintnico.verdlyhabits.ui.utils.calculateStreak
import com.saintnico.verdlyhabits.ui.viewmodel.HabitEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WidgetSnapshotWriter {
    private val gson = Gson()
    private val habitsKey = stringPreferencesKey("habits_list")
    private val todayFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun refreshHabitsFromLocalStore(context: Context) {
        val json = context.dataStore.data.first()[habitsKey] ?: ""
        if (json.isBlank()) {
            WidgetSnapshotStore(context).saveHabits(HabitsWidgetSnapshot())
            return
        }
        val type = object : TypeToken<List<HabitEntity>>() {}.type
        val entities: List<HabitEntity> = runCatching { gson.fromJson<List<HabitEntity>>(json, type) }
            .getOrDefault(emptyList())
        updateHabits(context, entities)
    }

    suspend fun updateHabits(context: Context, entities: List<HabitEntity>) {
        val today = LocalDate.now().format(todayFmt)
        val active = entities.filter { !it.isArchived }
        val rows = active
            .sortedWith(
                compareByDescending<HabitEntity> { it.isFavoriteFocus }
                    .thenBy { it.completedDates.contains(today) }
                    .thenByDescending { calculateStreak(it.completedDates) }
                    .thenBy { it.title.lowercase() },
            )
            .take(6)
            .map { entity ->
                val doneToday = entity.completedDates.contains(today)
                HabitWidgetRow(
                    id = entity.id,
                    title = entity.title,
                    streak = calculateStreak(entity.completedDates),
                    isCompletedToday = doneToday,
                    colorArgb = if (entity.color != 0L) entity.color else 0xFF4CAF50,
                    isPaused = entity.isPaused,
                )
            }
        val done = active.count { it.completedDates.contains(today) }
        WidgetSnapshotStore(context).saveHabits(
            HabitsWidgetSnapshot(
                habits = rows,
                doneToday = done,
                totalToday = active.size,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateLeaderboard(context: Context, challenges: List<Challenge>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val active = challenges.filter { it.isActive && !it.isArchived }
        val pick = active.maxByOrNull { it.startDate }
        if (pick == null || uid.isBlank()) {
            WidgetSnapshotStore(context).saveLeaderboard(LeaderboardWidgetSnapshot())
            return
        }
        val today = LocalDate.now().format(todayFmt)
        val board = pick.leaderboard()
        val rows = board.take(4).mapIndexed { index, (memberUid, score) ->
            LeaderboardWidgetRow(
                uid = memberUid,
                name = pick.memberNames[memberUid]?.ifBlank { "Rival" } ?: "Rival",
                streak = pick.streakFor(memberUid),
                score = score,
                scoreLabel = pick.scoreLabel(),
                reactionPoints = pick.reactionPointsFor(memberUid),
                rank = index + 1,
                isMe = memberUid == uid,
                doneToday = pick.hasCompletedToday(memberUid, today),
            )
        }
        WidgetSnapshotStore(context).saveLeaderboard(
            LeaderboardWidgetSnapshot(
                challengeId = pick.id,
                challengeTitle = pick.habitName.ifBlank { "Arena" },
                myRank = pick.rankOf(uid).takeIf { it > 0 } ?: 0,
                myUid = uid,
                rows = rows,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun refreshAll(context: Context, challenges: List<Challenge> = emptyList()) {
        refreshHabitsFromLocalStore(context)
        if (challenges.isNotEmpty()) {
            updateLeaderboard(context, challenges)
        }
        WidgetRefresh.updateAll(context)
    }
}
