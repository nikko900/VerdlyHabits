package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.ConfirmationState
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.PaceProfile
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Live metrics + presentation copy for the four goal rooms.
 * Progress math and what the user *sees* are deliberately type-specific.
 */
object GoalProgressEngine {

    private val dayMs = 24 * 60 * 60 * 1000L
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val zone: ZoneId get() = ZoneId.systemDefault()

    data class GoalMetrics(
        val progressFraction: Float,
        val heroPrimary: String,
        val heroSecondary: String? = null,
        val listStatus: String,
        val narrative: String,
        val showProgressBar: Boolean,
        /** Only Reach should auto-complete on fraction >= 1. */
        val canAutoComplete: Boolean,
        val cleanStreakDays: Int = 0,
        val longestCleanStreakDays: Int = 0,
        val weeksHeld: Int = 0,
        val trailingConsistency: Float = 0f,
        val buildStage: Int = 0,
        val remainingLabel: String? = null,
        val projectedFinishLabel: String? = null,
        val paceDeltaLabel: String? = null,
        val floorMetThisWeek: Boolean = false,
        val consecutiveMissDays: Int = 0,
        val costSavedLabel: String? = null,
        val costMetrics: CostSavedMetrics? = null,
        val shouldNudgeMaintain: Boolean = false,
        val confirmationState: ConfirmationState = ConfirmationState.STILL_CLEAR,
        val silentDays: Int = 0,
        val confirmedToday: Boolean = false,
        val recentUrges: Boolean = false,
    )

    data class CostSavedMetrics(
        val perOccurrence: Float,
        val unit: String,
        val cleanDays: Int,
        val totalSaved: Float,
        val monthlyProjection: Float?,
        val isMoney: Boolean,
    )

    fun linkedIds(goal: GoalEntity): List<String> =
        goal.linkedHabitIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun linkedHabits(goal: GoalEntity, habits: List<HabitItem>): List<HabitItem> {
        val ids = linkedIds(goal).toSet()
        return habits.filter { it.id in ids && !it.isArchived }
    }

    fun metrics(goal: GoalEntity, habits: List<HabitItem>): GoalMetrics = when (goal.goalType) {
        GoalType.BUILD -> buildMetrics(goal, habits)
        GoalType.REACH -> reachMetrics(goal, habits)
        GoalType.QUIT -> quitMetrics(goal)
        GoalType.MAINTAIN -> maintainMetrics(goal, habits)
    }

    /** Progress fraction 0..1 for milestone sync + list momentum. */
    fun progressFraction(goal: GoalEntity, habits: List<HabitItem>): Float =
        metrics(goal, habits).progressFraction

    fun currentValue(goal: GoalEntity, habits: List<HabitItem>): Float {
        val m = metrics(goal, habits)
        return when (goal.goalType) {
            GoalType.REACH -> linkedHabits(goal, habits).sumOf { it.completedDates.size }.toFloat()
                .let { if (it <= 0f && goal.currentValue > 0f) goal.currentValue else it }
            GoalType.QUIT -> m.cleanStreakDays.toFloat()
            GoalType.MAINTAIN -> m.weeksHeld.toFloat()
            GoalType.BUILD -> m.trailingConsistency * 100f
        }
    }

    fun goalDurationDays(goal: GoalEntity): Int =
        ((goal.targetDate - goal.startDate) / dayMs).toInt().coerceAtLeast(1)

    fun missedYesterday(goal: GoalEntity, habits: List<HabitItem>): Boolean {
        val linked = linkedHabits(goal, habits)
        if (linked.isEmpty()) return false
        val yesterday = LocalDate.now(zone).minusDays(1).format(dateFmt)
        return linked.none { it.completedDates.contains(yesterday) }
    }

    fun parsePace(goal: GoalEntity): PaceProfile =
        runCatching { PaceProfile.valueOf(goal.paceProfile ?: PaceProfile.STEADY.name) }
            .getOrDefault(PaceProfile.STEADY)

    // ── BUILD ────────────────────────────────────────────────────────────────

    private fun buildMetrics(goal: GoalEntity, habits: List<HabitItem>): GoalMetrics {
        val linked = linkedHabits(goal, habits)
        val consistency28 = trailingConsistency(goal, linked, windowDays = 28)
        val consistency14 = trailingConsistency(goal, linked, windowDays = 14)
        val stage = buildStage(goal, linked, consistency14, consistency28)
        val streak = linked.maxOfOrNull { it.streak } ?: 0
        val daysSinceMiss = daysSinceLastMiss(goal, linked)
        val hero = when {
            linked.isEmpty() -> "Link a habit"
            else -> "${(consistency28 * 100).roundToInt()}% rhythm"
        }
        val secondary = buildList {
            add("trailing 28 days")
            if (streak > 0) add("$streak-day streak")
            if (daysSinceMiss != null) add("$daysSinceMiss since a miss")
        }.joinToString(" · ").ifBlank { null }

        val narrative = GoalCelebrationCopy.liveNarrative(
            goal,
            GoalMetrics(
                progressFraction = consistency28.coerceIn(0f, 1f),
                heroPrimary = hero,
                heroSecondary = secondary,
                listStatus = hero,
                narrative = "",
                showProgressBar = false,
                canAutoComplete = false,
                trailingConsistency = consistency28,
                buildStage = stage,
            ),
        )

        return GoalMetrics(
            progressFraction = consistency28.coerceIn(0f, 1f),
            heroPrimary = hero,
            heroSecondary = secondary,
            listStatus = hero,
            narrative = narrative,
            showProgressBar = false,
            canAutoComplete = false,
            trailingConsistency = consistency28,
            buildStage = stage,
        )
    }

    private fun buildStage(
        goal: GoalEntity,
        linked: List<HabitItem>,
        c14: Float,
        c28: Float,
    ): Int {
        if (linked.isEmpty()) return 0
        val start = Instant.ofEpochMilli(goal.startDate).atZone(zone).toLocalDate()
        val weekEnd = start.plusDays(6)
        val firstWeekCount = countCompletionsInRange(linked, start, weekEnd)
        val daysAlive = TimeUnit.MILLISECONDS
            .toDays(System.currentTimeMillis() - goal.startDate)
            .toInt()
            .coerceAtLeast(0)
        return when {
            daysAlive >= 60 && missedWeeksSince(goal, linked) <= 1 -> 4
            c28 >= 0.80f -> 3
            c14 >= 0.70f -> 2
            firstWeekCount >= 3 -> 1
            else -> 0
        }
    }

    // ── REACH ────────────────────────────────────────────────────────────────

    private fun reachMetrics(goal: GoalEntity, habits: List<HabitItem>): GoalMetrics {
        val linked = linkedHabits(goal, habits)
        val target = goal.targetValue?.takeIf { it > 0f }
        val current = when {
            linked.isNotEmpty() -> linked.sumOf { it.completedDates.size }.toFloat()
            else -> goal.currentValue
        }
        val fraction = if (target != null) (current / target).coerceIn(0f, 1f) else timeProgress(goal)
        val remaining = if (target != null) (target - current).coerceAtLeast(0f) else null
        val unit = goal.unit.trim()
        val unitSuffix = if (unit.isNotEmpty()) " $unit" else ""

        val hero = if (target != null) {
            "${formatNum(current)} of ${formatNum(target)}$unitSuffix"
        } else {
            "${(fraction * 100).roundToInt()}% of the way"
        }
        val remainingLabel = remaining?.let {
            "${formatNum(it)}$unitSuffix to go"
        }

        val projected = projectedFinishDate(goal, current, target)
        val projectedLabel = projected?.let {
            "finishing ~${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
        }
        val paceDelta = paceDeltaLabel(goal, current, target)

        val narrative = GoalCelebrationCopy.liveNarrative(
            goal,
            GoalMetrics(
                progressFraction = fraction,
                heroPrimary = hero,
                heroSecondary = listOfNotNull(remainingLabel, projectedLabel).joinToString(" · ").ifBlank { null },
                listStatus = listOfNotNull(hero, projectedLabel).joinToString(" · "),
                narrative = "",
                showProgressBar = true,
                canAutoComplete = target != null && current >= target,
                remainingLabel = remainingLabel,
                projectedFinishLabel = projectedLabel,
                paceDeltaLabel = paceDelta,
            ),
        )

        return GoalMetrics(
            progressFraction = fraction,
            heroPrimary = hero,
            heroSecondary = listOfNotNull(remainingLabel, projectedLabel).joinToString(" · ").ifBlank { null },
            listStatus = listOfNotNull(hero, projectedLabel).joinToString(" · "),
            narrative = narrative,
            showProgressBar = true,
            canAutoComplete = target != null && current >= target,
            remainingLabel = remainingLabel,
            projectedFinishLabel = projectedLabel,
            paceDeltaLabel = paceDelta,
        )
    }

    // ── QUIT ─────────────────────────────────────────────────────────────────

    private fun quitMetrics(goal: GoalEntity): GoalMetrics {
        val eval = QuitConfirmationEngine.evaluate(goal)
        val clean = eval.cleanStreakDays
        val longest = max(goal.longestCleanStreakDays, clean)
        val costSaved = costSavedLabel(goal, clean)
        val costMetrics = computeCostSaved(goal, clean)
        val recentUrges = goal.recentUrgesUntil?.let { it > System.currentTimeMillis() } == true

        val fraction = (clean / 30f).coerceIn(0f, 1f)
        val hero = when {
            eval.state == ConfirmationState.UNCONFIRMED ->
                if (clean == 1) "1 day paused" else "$clean days paused"
            clean == 1 -> "1 day clear"
            else -> "$clean days clear"
        }
        val status = when (eval.state) {
            ConfirmationState.AT_RISK -> "Still clear today?"
            ConfirmationState.UNCONFIRMED -> "Check in to keep counting"
            ConfirmationState.STILL_CLEAR -> hero
        }

        val narrative = GoalCelebrationCopy.liveNarrative(
            goal,
            GoalMetrics(
                progressFraction = fraction,
                heroPrimary = hero,
                heroSecondary = null,
                listStatus = status,
                narrative = "",
                showProgressBar = false,
                canAutoComplete = false,
                cleanStreakDays = clean,
                longestCleanStreakDays = longest,
                costSavedLabel = costSaved,
                costMetrics = costMetrics,
                confirmationState = eval.state,
                silentDays = eval.silentDays,
                confirmedToday = eval.confirmedToday,
                recentUrges = recentUrges,
            ),
        )

        return GoalMetrics(
            progressFraction = fraction,
            heroPrimary = hero,
            heroSecondary = buildList {
                add("Longest: $longest")
                costSaved?.let { add(it) }
                if (eval.state == ConfirmationState.AT_RISK) add("Waiting on today")
                if (eval.state == ConfirmationState.UNCONFIRMED) add("Paused. Tell us how it went")
                if (recentUrges) add("Urges recently")
            }.joinToString(" · "),
            listStatus = status,
            narrative = narrative,
            showProgressBar = false,
            canAutoComplete = false,
            cleanStreakDays = clean,
            longestCleanStreakDays = longest,
            costSavedLabel = costSaved,
            costMetrics = costMetrics,
            confirmationState = eval.state,
            silentDays = eval.silentDays,
            confirmedToday = eval.confirmedToday,
            recentUrges = recentUrges,
        )
    }

    fun computeCostSaved(goal: GoalEntity, cleanDays: Int = cleanStreakDays(goal)): CostSavedMetrics? {
        val per = goal.costPerOccurrence ?: return null
        if (per <= 0f) return null
        val unit = goal.costUnit?.trim().orEmpty().ifBlank { "units" }
        val isMoney = unit.equals("money", true) || unit == "$" || unit.contains("£") || unit.contains("€")
        val days = cleanDays.coerceAtLeast(0)
        val total = per * days
        val monthly = if (days > 0) (total / days) * 30f else per * 30f
        return CostSavedMetrics(
            perOccurrence = per,
            unit = unit,
            cleanDays = days,
            totalSaved = total,
            monthlyProjection = monthly,
            isMoney = isMoney,
        )
    }

    fun cleanStreakDays(goal: GoalEntity): Int {
        if (goal.goalType != GoalType.QUIT) {
            val start = Instant.ofEpochMilli(goal.startDate).atZone(zone).toLocalDate()
            val today = LocalDate.now(zone)
            val lapseDay = goal.lastLapseAt?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            }
            val from = when {
                lapseDay == null -> start
                lapseDay.isAfter(today) -> today
                else -> lapseDay.plusDays(1)
            }
            if (from.isAfter(today)) return 0
            return (java.time.temporal.ChronoUnit.DAYS.between(from, today) + 1).toInt().coerceAtLeast(0)
        }
        return QuitConfirmationEngine.cleanStreakDays(goal)
    }

    // ── MAINTAIN ─────────────────────────────────────────────────────────────

    private fun maintainMetrics(goal: GoalEntity, habits: List<HabitItem>): GoalMetrics {
        val linked = linkedHabits(goal, habits)
        val floorCount = goal.floorCount ?: 3
        val floorDays = goal.floorPeriodDays ?: 7
        val met = floorMetThisPeriod(linked, floorCount, floorDays)
        val weeks = if (met) {
            // Prefer stored consecutive weeks; bump visually if this week is held and stored is stale.
            max(goal.consecutiveWeeksHeld, if (goal.consecutiveWeeksHeld == 0 && met) 1 else goal.consecutiveWeeksHeld)
        } else {
            goal.consecutiveWeeksHeld
        }
        val missStreak = consecutiveMissDays(linked)
        val fraction = (weeks / 52f).coerceIn(0f, 1f)

        val narrative = GoalCelebrationCopy.liveNarrative(
            goal,
            GoalMetrics(
                progressFraction = fraction,
                heroPrimary = if (weeks == 1) "1 week held" else "$weeks weeks held",
                heroSecondary = "Floor: $floorCount / ${floorDays}d" +
                    if (met) " · held this week" else " · under floor",
                listStatus = if (weeks == 1) "1 week held" else "$weeks weeks held",
                narrative = "",
                showProgressBar = false,
                canAutoComplete = false,
                weeksHeld = weeks,
                floorMetThisWeek = met,
                consecutiveMissDays = missStreak,
                shouldNudgeMaintain = missStreak >= 2,
            ),
        )

        return GoalMetrics(
            progressFraction = fraction,
            heroPrimary = if (weeks == 1) "1 week held" else "$weeks weeks held",
            heroSecondary = "Floor: $floorCount / ${floorDays}d" +
                if (met) " · held this week" else " · under floor",
            listStatus = if (weeks == 1) "1 week held" else "$weeks weeks held",
            narrative = narrative,
            showProgressBar = false,
            canAutoComplete = false,
            weeksHeld = weeks,
            floorMetThisWeek = met,
            consecutiveMissDays = missStreak,
            shouldNudgeMaintain = missStreak >= 2,
        )
    }

    fun floorMetThisPeriod(
        linked: List<HabitItem>,
        floorCount: Int,
        floorPeriodDays: Int,
    ): Boolean {
        if (linked.isEmpty()) return false
        val today = LocalDate.now(zone)
        val start = today.minusDays((floorPeriodDays - 1).toLong())
        val completions = countCompletionsInRange(linked, start, today)
        return completions >= floorCount
    }

    fun recomputeWeeksHeld(goal: GoalEntity, habits: List<HabitItem>): Pair<Int, Int> {
        val linked = linkedHabits(goal, habits)
        val floorCount = goal.floorCount ?: return goal.consecutiveWeeksHeld to goal.lifetimeWeeksHeld
        val floorDays = goal.floorPeriodDays ?: 7
        if (linked.isEmpty()) return 0 to goal.lifetimeWeeksHeld

        val today = LocalDate.now(zone)
        val start = Instant.ofEpochMilli(goal.startDate).atZone(zone).toLocalDate()
        var consecutive = 0
        var lifetime = 0
        var weekEnd = today
        while (!weekEnd.isBefore(start)) {
            val weekStart = weekEnd.minusDays((floorDays - 1).toLong())
            val count = countCompletionsInRange(linked, weekStart.coerceAtLeast(start), weekEnd)
            if (count >= floorCount) {
                lifetime++
                if (weekEnd == today || consecutive > 0) consecutive++
                else if (lifetime == 1 && weekEnd == today) consecutive = 1
            } else if (weekEnd == today) {
                // Current week still open — don't break consecutive yet unless previous weeks failed.
            } else {
                break
            }
            weekEnd = weekEnd.minusDays(floorDays.toLong())
        }
        // Simpler consecutive: walk back week by week from current complete periods
        consecutive = 0
        weekEnd = today
        var first = true
        while (!weekEnd.isBefore(start)) {
            val weekStart = weekEnd.minusDays((floorDays - 1).toLong())
            val count = countCompletionsInRange(linked, weekStart.coerceAtLeast(start), weekEnd)
            val met = count >= floorCount
            if (met) {
                consecutive++
                first = false
            } else if (first && weekEnd == today) {
                // incomplete current week — skip without breaking
                first = false
            } else {
                break
            }
            weekEnd = weekEnd.minusDays(floorDays.toLong())
        }
        lifetime = max(goal.lifetimeWeeksHeld, consecutive)
        return consecutive to lifetime
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun trailingConsistency(
        goal: GoalEntity,
        linked: List<HabitItem>,
        windowDays: Int,
    ): Float {
        if (linked.isEmpty()) return 0f
        val today = LocalDate.now(zone)
        val goalStart = Instant.ofEpochMilli(goal.startDate).atZone(zone).toLocalDate()
        val windowStart = today.minusDays((windowDays - 1).toLong()).coerceAtLeast(goalStart)
        val total = java.time.temporal.ChronoUnit.DAYS.between(windowStart, today).toInt() + 1
        if (total <= 0) return 0f
        var perfect = 0
        var d = windowStart
        while (!d.isAfter(today)) {
            val key = d.format(dateFmt)
            if (linked.all { it.completedDates.contains(key) }) perfect++
            d = d.plusDays(1)
        }
        return (perfect.toFloat() / total).coerceIn(0f, 1f)
    }

    private fun daysSinceLastMiss(goal: GoalEntity, linked: List<HabitItem>): Int? {
        if (linked.isEmpty()) return null
        val today = LocalDate.now(zone)
        val start = Instant.ofEpochMilli(goal.startDate).atZone(zone).toLocalDate()
        var d = today
        while (!d.isBefore(start)) {
            val key = d.format(dateFmt)
            if (linked.any { !it.completedDates.contains(key) }) {
                return java.time.temporal.ChronoUnit.DAYS.between(d, today).toInt()
            }
            d = d.minusDays(1)
        }
        return java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
    }

    private fun missedWeeksSince(goal: GoalEntity, linked: List<HabitItem>): Int {
        val start = Instant.ofEpochMilli(goal.startDate).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        var misses = 0
        var weekStart = start
        while (!weekStart.isAfter(today)) {
            val weekEnd = weekStart.plusDays(6).coerceAtMost(today)
            if (countCompletionsInRange(linked, weekStart, weekEnd) == 0) misses++
            weekStart = weekStart.plusDays(7)
        }
        return misses
    }

    private fun countCompletionsInRange(
        linked: List<HabitItem>,
        start: LocalDate,
        end: LocalDate,
    ): Int {
        var count = 0
        var d = start
        while (!d.isAfter(end)) {
            val key = d.format(dateFmt)
            count += linked.count { it.completedDates.contains(key) }
            d = d.plusDays(1)
        }
        return count
    }

    private fun consecutiveMissDays(linked: List<HabitItem>): Int {
        if (linked.isEmpty()) return 0
        val today = LocalDate.now(zone)
        var miss = 0
        var d = today
        repeat(14) {
            val key = d.format(dateFmt)
            if (linked.none { it.completedDates.contains(key) }) {
                miss++
                d = d.minusDays(1)
            } else return miss
        }
        return miss
    }

    private fun timeProgress(goal: GoalEntity): Float {
        val span = (goal.targetDate - goal.startDate).coerceAtLeast(1L)
        return ((System.currentTimeMillis() - goal.startDate).toFloat() / span).coerceIn(0f, 1f)
    }

    private fun projectedFinishDate(
        goal: GoalEntity,
        current: Float,
        target: Float?,
    ): LocalDate? {
        if (target == null || target <= 0f || current <= 0f) return null
        if (current >= target) return LocalDate.now(zone)
        val elapsed = TimeUnit.MILLISECONDS
            .toDays(System.currentTimeMillis() - goal.startDate)
            .coerceAtLeast(1)
        val rate = current / elapsed.toFloat()
        if (rate <= 0f) return null
        val daysLeft = ((target - current) / rate).roundToInt().coerceAtLeast(0)
        return LocalDate.now(zone).plusDays(daysLeft.toLong())
    }

    private fun paceDeltaLabel(goal: GoalEntity, current: Float, target: Float?): String? {
        if (target == null || target <= 0f) return null
        val span = (goal.targetDate - goal.startDate).coerceAtLeast(1L)
        val elapsed = (System.currentTimeMillis() - goal.startDate).toFloat().coerceAtLeast(0f)
        val expected = target * (elapsed / span).coerceIn(0f, 1f)
        val delta = current - expected
        val unit = goal.unit.trim()
        val u = if (unit.isNotEmpty()) " $unit" else ""
        return when {
            delta > 0.5f -> "${formatNum(delta)}$u ahead of pace"
            delta < -0.5f -> "${formatNum(-delta)}$u behind pace"
            else -> "On pace"
        }
    }

    private fun costSavedLabel(goal: GoalEntity, cleanDays: Int): String? {
        val metrics = computeCostSaved(goal, cleanDays) ?: return null
        return formatCostSaved(metrics)
    }

    fun formatCostSaved(metrics: CostSavedMetrics): String {
        if (metrics.cleanDays <= 0) {
            return if (metrics.isMoney) {
                "~${formatNum(metrics.perOccurrence)} reclaimed per clear day"
            } else {
                "~${formatNum(metrics.perOccurrence)} ${metrics.unit} per clear day"
            }
        }
        return when {
            metrics.isMoney -> "Saved ~${formatNum(metrics.totalSaved)}"
            metrics.unit.isNotEmpty() -> "Reclaimed ~${formatNum(metrics.totalSaved)} ${metrics.unit}"
            else -> "Reclaimed ~${formatNum(metrics.totalSaved)}"
        }
    }

    private fun formatNum(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString() else String.format("%.1f", v)
}
