package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.ConfirmationState
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Quit streak integrity: days are earned by confirmation, never by the clock.
 * Relapse is never inferred. Silence freezes the number until the user answers.
 */
object QuitConfirmationEngine {

    const val RISK_HOUR = 20
    const val RISK_MINUTE = 30
    const val GRACE_UNTIL_HOUR = 4
    const val UNCONFIRMED_AFTER_SILENT_DAYS = 2

    data class Snapshot(
        val startDate: Long,
        val lastLapseAt: Long?,
        val lastConfirmedAt: Long?,
        val streakAnchorAt: Long,
        val isArchived: Boolean,
    )

    data class Evaluation(
        val state: ConfirmationState,
        val cleanStreakDays: Int,
        val silentDays: Int,
        val confirmedToday: Boolean,
        val confirmAppliesTo: LocalDate,
        val catchUpCopyTier: CatchUpCopyTier,
    )

    enum class CatchUpCopyTier { NONE, FEW_DAYS, OVER_A_WEEK, A_WHILE }

    fun snapshotOf(goal: GoalEntity): Snapshot = Snapshot(
        startDate = goal.startDate,
        lastLapseAt = goal.lastLapseAt,
        lastConfirmedAt = goal.lastConfirmedAt,
        streakAnchorAt = goal.streakAnchorAt,
        isArchived = goal.isArchived,
    )

    fun evaluate(
        goal: GoalEntity,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Evaluation = evaluate(snapshotOf(goal), now, zone)

    fun evaluate(
        snap: Snapshot,
        now: Instant,
        zone: ZoneId,
    ): Evaluation {
        val today = now.atZone(zone).toLocalDate()
        val localTime = now.atZone(zone).toLocalTime()
        val lastConfirmDate = snap.lastConfirmedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val heardFromDate = lastConfirmDate
            ?: snap.lastLapseAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: Instant.ofEpochMilli(anchorMillis(snap)).atZone(zone).toLocalDate()
        val silentDays = ChronoUnit.DAYS.between(heardFromDate, today).toInt().coerceAtLeast(0)
        val confirmedToday = lastConfirmDate == today
        val confirmAppliesTo = resolveConfirmDate(today, localTime, lastConfirmDate)
        val state = when {
            snap.isArchived -> ConfirmationState.STILL_CLEAR
            confirmedToday -> ConfirmationState.STILL_CLEAR
            silentDays >= UNCONFIRMED_AFTER_SILENT_DAYS -> ConfirmationState.UNCONFIRMED
            silentDays == 1 && !localTime.isBefore(LocalTime.of(RISK_HOUR, RISK_MINUTE)) ->
                ConfirmationState.AT_RISK
            else -> ConfirmationState.STILL_CLEAR
        }
        val days = cleanStreakDays(snap, zone)
        return Evaluation(
            state = state,
            cleanStreakDays = days,
            silentDays = silentDays,
            confirmedToday = confirmedToday,
            confirmAppliesTo = confirmAppliesTo,
            catchUpCopyTier = catchUpTier(silentDays, state),
        )
    }

    fun cleanStreakDays(goal: GoalEntity, zone: ZoneId = ZoneId.systemDefault()): Int =
        cleanStreakDays(snapshotOf(goal), zone)

    fun cleanStreakDays(snap: Snapshot, zone: ZoneId): Int {
        val lastConfirm = snap.lastConfirmedAt ?: return 0
        val through = Instant.ofEpochMilli(lastConfirm).atZone(zone).toLocalDate()
        val from = Instant.ofEpochMilli(anchorMillis(snap)).atZone(zone).toLocalDate()
        if (through.isBefore(from)) return 0
        return (ChronoUnit.DAYS.between(from, through) + 1).toInt().coerceAtLeast(0)
    }

    fun resolveConfirmDate(
        today: LocalDate,
        localTime: LocalTime,
        lastConfirmDate: LocalDate?,
    ): LocalDate {
        if (localTime.hour < GRACE_UNTIL_HOUR) {
            val yesterday = today.minusDays(1)
            if (lastConfirmDate == null || lastConfirmDate.isBefore(yesterday)) {
                return yesterday
            }
        }
        return today
    }

    fun catchUpCopy(goalTitle: String, silentDays: Int): Pair<String, String> {
        val title = goalTitle.ifBlank { "this" }
        return when {
            silentDays >= 14 ->
                "It's been a while" to
                    "If this still fits your life, let's pick it back up. However it went."
            silentDays >= 4 ->
                "It's been over a week" to
                    "However it went, we just want the truth. No judgment."
            else ->
                "It's been a few days" to
                    "It's been a few days since you checked in on $title. How'd it go?"
        }
    }

    fun firstSilentDay(snap: Snapshot, zone: ZoneId): LocalDate {
        val today = LocalDate.now(zone)
        val lastConfirm = snap.lastConfirmedAt?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }
        return (lastConfirm?.plusDays(1) ?: Instant.ofEpochMilli(anchorMillis(snap)).atZone(zone).toLocalDate())
            .coerceAtMost(today)
    }

    fun isQuit(goal: GoalEntity): Boolean = goal.goalType == GoalType.QUIT

    private fun anchorMillis(snap: Snapshot): Long =
        if (snap.streakAnchorAt > 0L) snap.streakAnchorAt else snap.startDate

    private fun catchUpTier(silentDays: Int, state: ConfirmationState): CatchUpCopyTier {
        if (state != ConfirmationState.UNCONFIRMED) return CatchUpCopyTier.NONE
        return when {
            silentDays >= 14 -> CatchUpCopyTier.A_WHILE
            silentDays >= 4 -> CatchUpCopyTier.OVER_A_WEEK
            else -> CatchUpCopyTier.FEW_DAYS
        }
    }
}
