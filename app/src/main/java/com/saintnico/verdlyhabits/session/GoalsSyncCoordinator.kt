package com.saintnico.verdlyhabits.session

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.goals.ConfirmationState
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.remote.firestore.GoalsCloudRepository
import com.saintnico.verdlyhabits.data.remote.firestore.GoalsCloudSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps goals in sync with Firestore so they survive DB wipes, migrations, and account switches.
 * Habits already used this pattern via [com.saintnico.verdlyhabits.data.remote.firestore.UserRepository.saveHabits].
 */
object GoalsSyncCoordinator {

    private const val TAG = "GoalsSyncCoordinator"

    suspend fun backupNow(context: Context) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        withContext(Dispatchers.IO) {
            runCatching {
                val dao = AppDatabase.getDatabase(context).goalsDao()
                val goals = dao.getAllGoals()
                if (goals.isEmpty()) return@withContext
                val snapshot = GoalsCloudSnapshot(
                    goals = goals,
                    milestones = dao.getAllMilestones(),
                    checkIns = dao.getAllCheckIns(),
                    confirmations = dao.getAllConfirmations(),
                    silentGaps = dao.getAllSilentGaps(),
                    savedAt = System.currentTimeMillis(),
                )
                GoalsCloudRepository().saveSnapshot(snapshot)
                Log.i(TAG, "Backed up ${goals.size} goal(s) to Firestore")
            }.onFailure { e ->
                Log.w(TAG, "Goals backup failed", e)
            }
        }
    }

    /** Pull cloud goals into Room when local data is missing or stale. */
    suspend fun restoreNow(context: Context) {
        if (FirebaseAuth.getInstance().currentUser == null) return
        withContext(Dispatchers.IO) {
            runCatching {
                val cloud = GoalsCloudRepository().fetchSnapshot() ?: return@withContext
                if (cloud.goals.isEmpty()) return@withContext

                val dao = AppDatabase.getDatabase(context).goalsDao()
                val localGoals = dao.getAllGoals()
                val mergedGoals = mergeGoals(localGoals, cloud.goals)
                if (mergedGoals.isEmpty()) return@withContext

                val goalIds = mergedGoals.map { it.id }.toSet()
                mergedGoals.forEach { dao.insertGoal(it) }
                cloud.milestones
                    .filter { it.goalId in goalIds }
                    .forEach { dao.insertMilestones(listOf(it)) }
                cloud.checkIns
                    .filter { it.goalId in goalIds }
                    .forEach { dao.insertCheckIn(it) }
                cloud.confirmations.orEmpty()
                    .filter { it.goalId in goalIds }
                    .forEach { row ->
                        runCatching { dao.insertConfirmation(row) }
                    }
                cloud.silentGaps.orEmpty()
                    .filter { it.goalId in goalIds }
                    .forEach { dao.insertSilentGap(it) }

                Log.i(TAG, "Restored ${mergedGoals.size} goal(s) from Firestore")
            }.onFailure { e ->
                Log.w(TAG, "Goals restore failed", e)
            }
        }
    }

    private fun mergeGoals(local: List<GoalEntity>, cloud: List<GoalEntity>): List<GoalEntity> {
        if (local.isEmpty()) return cloud.map { backfillQuitConfirmation(it) }
        val byId = local.associateBy { it.id }.toMutableMap()
        cloud.forEach { remote ->
            val existing = byId[remote.id]
            byId[remote.id] = when {
                existing == null -> backfillQuitConfirmation(remote)
                remote.energyLastUpdated > existing.energyLastUpdated ->
                    backfillQuitConfirmation(remote)
                else -> backfillQuitConfirmation(existing)
            }
        }
        return byId.values.map { backfillQuitConfirmation(it) }.toList()
    }

    /** Old cloud snapshots have no lastConfirmedAt — treat them as confirmed now so restore isn't Unconfirmed. */
    private fun backfillQuitConfirmation(goal: GoalEntity): GoalEntity {
        if (goal.goalType != GoalType.QUIT) {
            return if (goal.streakAnchorAt == 0L) goal.copy(streakAnchorAt = goal.startDate) else goal
        }
        val now = System.currentTimeMillis()
        return goal.copy(
            lastConfirmedAt = goal.lastConfirmedAt ?: now,
            streakAnchorAt = if (goal.streakAnchorAt > 0L) goal.streakAnchorAt else goal.startDate,
            confirmationStateCache = goal.confirmationStateCache.ifBlank { ConfirmationState.STILL_CLEAR.name },
        )
    }
}
