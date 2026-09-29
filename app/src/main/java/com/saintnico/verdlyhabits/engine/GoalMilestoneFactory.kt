package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.util.UUID

/**
 * Type-specific milestone seeds. [MilestoneEntity.targetPercent] is overloaded per room:
 * - REACH: true percent of target (25/50/75/100)
 * - QUIT: clean-day thresholds (1/3/7/14/30)
 * - MAINTAIN: weeks held (4/13/26/52)
 * - BUILD: stage index as 25/50/75/100 mapped to stages 1–4
 */
object GoalMilestoneFactory {

    fun createFor(goal: GoalEntity): List<MilestoneEntity> = when (goal.goalType) {
        GoalType.BUILD -> listOf(
            milestone(goal.id, "First Bloom", 25f),
            milestone(goal.id, "Taking Root", 50f),
            milestone(goal.id, "Steady Rhythm", 75f),
            milestone(goal.id, "Second Nature", 100f),
        )
        GoalType.REACH -> listOf(
            milestone(goal.id, "Quarter mark", 25f),
            milestone(goal.id, "The midpoint", 50f),
            milestone(goal.id, "Home stretch", 75f),
            milestone(goal.id, "The finish line", 100f),
        )
        GoalType.QUIT -> listOf(
            milestone(goal.id, "First day clear", 1f),
            milestone(goal.id, "Three days clear", 3f),
            milestone(goal.id, "One week clear", 7f),
            milestone(goal.id, "Two weeks clear", 14f),
            milestone(goal.id, "One month clear", 30f),
        )
        GoalType.MAINTAIN -> listOf(
            milestone(goal.id, "One month held", 4f),
            milestone(goal.id, "One quarter held", 13f),
            milestone(goal.id, "Half a year held", 26f),
            milestone(goal.id, "One year held", 52f),
        )
    }

    /** Key moments that deserve a full ritual screen — not just konfetti. */
    fun isRitualMilestone(goal: GoalEntity, milestone: MilestoneEntity): Boolean =
        when (goal.goalType) {
            GoalType.REACH -> milestone.targetPercent == 50f
            GoalType.BUILD -> milestone.targetPercent == 50f
            GoalType.QUIT -> milestone.targetPercent == 7f || milestone.targetPercent == 14f
            GoalType.MAINTAIN -> milestone.targetPercent == 4f || milestone.targetPercent == 13f
        }

    /**
     * Returns the threshold value to compare against [MilestoneEntity.targetPercent]
     * for completion checks.
     */
    fun progressForMilestones(goal: GoalEntity, habits: List<HabitItem>): Float {
        val m = GoalProgressEngine.metrics(goal, habits)
        return when (goal.goalType) {
            GoalType.BUILD -> when (m.buildStage) {
                0 -> 0f
                1 -> 25f
                2 -> 50f
                3 -> 75f
                else -> 100f
            }
            GoalType.REACH -> m.progressFraction * 100f
            GoalType.QUIT -> m.cleanStreakDays.toFloat()
            GoalType.MAINTAIN -> m.weeksHeld.toFloat()
        }
    }

    fun reachMilestoneTitle(
        baseTitle: String,
        targetPercent: Float,
        paceDeltaLabel: String?,
    ): String {
        if (targetPercent != 50f) {
            val pace = paceDeltaLabel ?: return baseTitle
            return when {
                pace.contains("ahead", true) -> "$baseTitle · ahead of pace"
                pace.contains("behind", true) -> "$baseTitle · behind pace"
                pace.contains("On pace", true) -> "$baseTitle · on pace"
                else -> baseTitle
            }
        }
        return baseTitle
    }

    private fun milestone(goalId: String, title: String, target: Float) = MilestoneEntity(
        id = UUID.randomUUID().toString(),
        goalId = goalId,
        title = title,
        targetPercent = target,
        completedDate = null,
        isCompleted = false,
    )
}
