package com.saintnico.verdlyhabits.data.goals

import android.content.Context
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.goals.ConfirmationSource
import com.saintnico.verdlyhabits.data.local.goals.ConfirmationState
import com.saintnico.verdlyhabits.data.local.goals.ConfirmationType
import com.saintnico.verdlyhabits.data.local.goals.GoalConfirmationEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalSilentGapEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.SilentGapResolution
import com.saintnico.verdlyhabits.engine.QuitConfirmationEngine
import com.saintnico.verdlyhabits.session.GoalsSyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Single writer for Quit confirmation events. Used by the ViewModel and by
 * notification action receivers so a tap never requires the app UI.
 */
object QuitConfirmationWriter {

    suspend fun confirmStillClear(
        context: Context,
        goalId: String,
        source: ConfirmationSource,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).goalsDao()
        val goal = dao.getGoalById(goalId).first() ?: return@withContext
        if (goal.goalType != GoalType.QUIT) return@withContext
        val eval = QuitConfirmationEngine.evaluate(goal, now, zone)
        if (eval.confirmedToday) return@withContext
        val applyDate = eval.confirmAppliesTo
        append(
            dao,
            goal,
            applyDate,
            now,
            ConfirmationType.CONFIRMED,
            source,
            lastConfirmedAt = now.toEpochMilli(),
            streakAnchorAt = anchorOf(goal),
            lastLapseAt = goal.lastLapseAt,
        )
        GoalsSyncCoordinator.backupNow(context)
    }

    suspend fun logSlip(
        context: Context,
        goalId: String,
        slipDate: LocalDate? = null,
        source: ConfirmationSource = ConfirmationSource.DAILY_TAP,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).goalsDao()
        val goal = dao.getGoalById(goalId).first() ?: return@withContext
        if (goal.goalType != GoalType.QUIT) return@withContext
        val today = now.atZone(zone).toLocalDate()
        val lapseDay = (slipDate ?: today).coerceAtMost(today)
        val lapseInstant = lapseDay.atTime(now.atZone(zone).toLocalTime())
            .atZone(zone)
            .toInstant()
            .coerceAtMost(now)
        val clean = QuitConfirmationEngine.cleanStreakDays(goal, zone)
        val longest = maxOf(goal.longestCleanStreakDays, clean)
        val nextAnchor = lapseDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        append(
            dao,
            goal.copy(longestCleanStreakDays = longest),
            lapseDay,
            now,
            ConfirmationType.SLIP,
            source,
            lastConfirmedAt = null,
            streakAnchorAt = nextAnchor,
            lastLapseAt = lapseInstant.toEpochMilli(),
            extraGoal = { it.copy(currentValue = 0f, longestCleanStreakDays = longest) },
        )
        closePendingGap(dao, goal.id, lapseDay.toString(), SilentGapResolution.NO_SLIPPED, now)
        GoalsSyncCoordinator.backupNow(context)
    }

    suspend fun resolveCatchUp(
        context: Context,
        goalId: String,
        resolution: SilentGapResolution,
        slipDate: LocalDate? = null,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context).goalsDao()
        val goal = dao.getGoalById(goalId).first() ?: return@withContext
        if (goal.goalType != GoalType.QUIT) return@withContext
        val today = now.atZone(zone).toLocalDate()
        val firstSilent = QuitConfirmationEngine.firstSilentDay(QuitConfirmationEngine.snapshotOf(goal), zone)
        when (resolution) {
            SilentGapResolution.YES_CLEAR, SilentGapResolution.UNSURE_KEEP -> {
                val type = if (resolution == SilentGapResolution.YES_CLEAR) {
                    ConfirmationType.SELF_REPORTED_RETROACTIVE
                } else {
                    ConfirmationType.CONFIRMATION_UNCERTAIN
                }
                var day = firstSilent
                while (!day.isAfter(today)) {
                    dao.insertConfirmation(
                        GoalConfirmationEntity(
                            id = UUID.randomUUID().toString(),
                            goalId = goal.id,
                            date = day.toString(),
                            createdAt = now.toEpochMilli(),
                            type = type.name,
                            source = ConfirmationSource.CATCHUP_FLOW.name,
                        ),
                    )
                    day = day.plusDays(1)
                }
                val evalAfter = goal.copy(lastConfirmedAt = now.toEpochMilli())
                dao.updateGoal(
                    evalAfter.copy(
                        confirmationStateCache = ConfirmationState.STILL_CLEAR.name,
                        energyLastUpdated = now.toEpochMilli(),
                    ),
                )
                closePendingGap(dao, goal.id, today.toString(), resolution, now)
            }
            SilentGapResolution.UNSURE_RESET -> {
                dao.insertConfirmation(
                    GoalConfirmationEntity(
                        id = UUID.randomUUID().toString(),
                        goalId = goal.id,
                        date = today.toString(),
                        createdAt = now.toEpochMilli(),
                        type = ConfirmationType.SOFT_RESET.name,
                        source = ConfirmationSource.CATCHUP_FLOW.name,
                    ),
                )
                dao.updateGoal(
                    goal.copy(
                        lastConfirmedAt = null,
                        streakAnchorAt = today.atStartOfDay(zone).toInstant().toEpochMilli(),
                        confirmationStateCache = ConfirmationState.STILL_CLEAR.name,
                        currentValue = 0f,
                        energyLastUpdated = now.toEpochMilli(),
                    ),
                )
                closePendingGap(dao, goal.id, today.toString(), resolution, now)
            }
            SilentGapResolution.NO_SLIPPED -> {
                logSlip(context, goalId, slipDate ?: firstSilent, ConfirmationSource.CATCHUP_FLOW, now, zone)
                return@withContext
            }
            SilentGapResolution.PENDING -> Unit
        }
        GoalsSyncCoordinator.backupNow(context)
    }

    suspend fun ensurePendingGap(context: Context, goal: GoalEntity) = withContext(Dispatchers.IO) {
        if (goal.goalType != GoalType.QUIT || goal.isArchived) return@withContext
        val eval = QuitConfirmationEngine.evaluate(goal)
        if (eval.state != ConfirmationState.UNCONFIRMED) return@withContext
        val dao = AppDatabase.getDatabase(context).goalsDao()
        val pending = dao.getSilentGapsForGoal(goal.id)
            .any { it.resolution == SilentGapResolution.PENDING.name }
        if (pending) return@withContext
        val start = QuitConfirmationEngine.firstSilentDay(
            QuitConfirmationEngine.snapshotOf(goal),
            ZoneId.systemDefault(),
        )
        dao.insertSilentGap(
            GoalSilentGapEntity(
                id = UUID.randomUUID().toString(),
                goalId = goal.id,
                gapStartDate = start.toString(),
                gapEndDate = null,
                resolution = SilentGapResolution.PENDING.name,
                resolvedAt = null,
            ),
        )
    }

    suspend fun seedCreated(context: Context, goal: GoalEntity, now: Instant = Instant.now()) =
        withContext(Dispatchers.IO) {
            if (goal.goalType != GoalType.QUIT) return@withContext
            val dao = AppDatabase.getDatabase(context).goalsDao()
            val date = now.atZone(ZoneId.systemDefault()).toLocalDate().toString()
            dao.insertConfirmation(
                GoalConfirmationEntity(
                    id = UUID.randomUUID().toString(),
                    goalId = goal.id,
                    date = date,
                    createdAt = now.toEpochMilli(),
                    type = ConfirmationType.GOAL_CREATED.name,
                    source = ConfirmationSource.GOAL_CREATED.name,
                ),
            )
        }

    private fun anchorOf(goal: GoalEntity): Long =
        if (goal.streakAnchorAt > 0L) goal.streakAnchorAt else goal.startDate

    private fun append(
        dao: com.saintnico.verdlyhabits.data.local.goals.GoalsDao,
        goal: GoalEntity,
        applyDate: LocalDate,
        now: Instant,
        type: ConfirmationType,
        source: ConfirmationSource,
        lastConfirmedAt: Long?,
        streakAnchorAt: Long,
        lastLapseAt: Long?,
        extraGoal: (GoalEntity) -> GoalEntity = { it },
    ) {
        dao.insertConfirmation(
            GoalConfirmationEntity(
                id = UUID.randomUUID().toString(),
                goalId = goal.id,
                date = applyDate.toString(),
                createdAt = now.toEpochMilli(),
                type = type.name,
                source = source.name,
            ),
        )
        val state = QuitConfirmationEngine.evaluate(
            QuitConfirmationEngine.Snapshot(
                startDate = goal.startDate,
                lastLapseAt = lastLapseAt,
                lastConfirmedAt = lastConfirmedAt,
                streakAnchorAt = streakAnchorAt,
                isArchived = goal.isArchived,
            ),
            now,
            ZoneId.systemDefault(),
        )
        dao.updateGoal(
            extraGoal(
                goal.copy(
                    lastConfirmedAt = lastConfirmedAt,
                    streakAnchorAt = streakAnchorAt,
                    lastLapseAt = lastLapseAt,
                    confirmationStateCache = state.state.name,
                    energyLastUpdated = now.toEpochMilli(),
                ),
            ),
        )
    }

    private fun closePendingGap(
        dao: com.saintnico.verdlyhabits.data.local.goals.GoalsDao,
        goalId: String,
        endDate: String,
        resolution: SilentGapResolution,
        now: Instant,
    ) {
        dao.getSilentGapsForGoal(goalId)
            .filter { it.resolution == SilentGapResolution.PENDING.name }
            .forEach { gap ->
                dao.updateSilentGap(
                    gap.copy(
                        gapEndDate = endDate,
                        resolution = resolution.name,
                        resolvedAt = now.toEpochMilli(),
                    ),
                )
            }
    }
}
