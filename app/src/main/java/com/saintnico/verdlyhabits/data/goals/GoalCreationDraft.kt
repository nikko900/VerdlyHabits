package com.saintnico.verdlyhabits.data.goals

import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.PaceProfile
import com.saintnico.verdlyhabits.ui.screens.goals.creation.GoalCreationState
import org.json.JSONObject

/** In-progress goal wizard — survives navigation to Add Habit and app restarts. */
data class GoalCreationDraft(
    val currentStep: Int = 1,
    val goalType: GoalType? = null,
    val title: String = "",
    val whyStatement: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val targetValue: String = "",
    val unit: String = "",
    val linkedHabitIds: List<String> = emptyList(),
    val colorHex: String = "#4CAF50",
    val iconName: String = "Rounded.TrackChanges",
    val tinyVersionText: String = "",
    val anchorCue: String = "",
    val softReviewEnabled: Boolean = false,
    val paceProfile: PaceProfile = PaceProfile.STEADY,
    val triggerText: String = "",
    val replacementText: String = "",
    val trackCost: Boolean = false,
    val costPerOccurrence: String = "",
    val costUnit: String = "money",
    val floorCount: String = "3",
    val floorPeriodDays: String = "7",
    val returnAfterAddHabit: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val hasProgress: Boolean
        get() = goalType != null && (currentStep > 1 || title.isNotBlank() || whyStatement.isNotBlank())

    fun continueLabel(): String = when (goalType) {
        GoalType.BUILD -> "Continue building"
        GoalType.REACH -> "Continue reaching"
        GoalType.QUIT -> "Continue quitting"
        GoalType.MAINTAIN -> "Continue maintaining"
        null -> "Continue your goal"
    }

    fun toJson(): String = JSONObject().apply {
        put("currentStep", currentStep)
        put("goalType", goalType?.name)
        put("title", title)
        put("whyStatement", whyStatement)
        put("startDate", startDate)
        put("targetDate", targetDate)
        put("targetValue", targetValue)
        put("unit", unit)
        put("linkedHabitIds", linkedHabitIds.joinToString(","))
        put("colorHex", colorHex)
        put("iconName", iconName)
        put("tinyVersionText", tinyVersionText)
        put("anchorCue", anchorCue)
        put("softReviewEnabled", softReviewEnabled)
        put("paceProfile", paceProfile.name)
        put("triggerText", triggerText)
        put("replacementText", replacementText)
        put("trackCost", trackCost)
        put("costPerOccurrence", costPerOccurrence)
        put("costUnit", costUnit)
        put("floorCount", floorCount)
        put("floorPeriodDays", floorPeriodDays)
        put("returnAfterAddHabit", returnAfterAddHabit)
        put("updatedAt", updatedAt)
    }.toString()

    companion object {
        fun fromState(
            state: GoalCreationState,
            step: Int,
            returnAfterAddHabit: Boolean = false,
        ): GoalCreationDraft = GoalCreationDraft(
            currentStep = step,
            goalType = state.goalType,
            title = state.title,
            whyStatement = state.whyStatement,
            startDate = state.startDate,
            targetDate = state.targetDate,
            targetValue = state.targetValue,
            unit = state.unit,
            linkedHabitIds = state.linkedHabitIds.toList(),
            colorHex = state.colorHex,
            iconName = state.iconName,
            tinyVersionText = state.tinyVersionText,
            anchorCue = state.anchorCue,
            softReviewEnabled = state.softReviewEnabled,
            paceProfile = state.paceProfile,
            triggerText = state.triggerText,
            replacementText = state.replacementText,
            trackCost = state.trackCost,
            costPerOccurrence = state.costPerOccurrence,
            costUnit = state.costUnit,
            floorCount = state.floorCount,
            floorPeriodDays = state.floorPeriodDays,
            returnAfterAddHabit = returnAfterAddHabit,
        )

        fun fromJson(raw: String): GoalCreationDraft? = runCatching {
            val o = JSONObject(raw)
            GoalCreationDraft(
                currentStep = o.optInt("currentStep", 1),
                goalType = o.optString("goalType").takeIf { it.isNotBlank() }?.let { GoalType.valueOf(it) },
                title = o.optString("title", ""),
                whyStatement = o.optString("whyStatement", ""),
                startDate = o.optLong("startDate", System.currentTimeMillis()),
                targetDate = o.optLong("targetDate", System.currentTimeMillis()),
                targetValue = o.optString("targetValue", ""),
                unit = o.optString("unit", ""),
                linkedHabitIds = o.optString("linkedHabitIds", "")
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() },
                colorHex = o.optString("colorHex", "#4CAF50"),
                iconName = o.optString("iconName", "Rounded.TrackChanges"),
                tinyVersionText = o.optString("tinyVersionText", ""),
                anchorCue = o.optString("anchorCue", ""),
                softReviewEnabled = o.optBoolean("softReviewEnabled", false),
                paceProfile = runCatching {
                    PaceProfile.valueOf(o.optString("paceProfile", PaceProfile.STEADY.name))
                }.getOrDefault(PaceProfile.STEADY),
                triggerText = o.optString("triggerText", ""),
                replacementText = o.optString("replacementText", ""),
                trackCost = o.optBoolean("trackCost", false),
                costPerOccurrence = o.optString("costPerOccurrence", ""),
                costUnit = o.optString("costUnit", "money"),
                floorCount = o.optString("floorCount", "3"),
                floorPeriodDays = o.optString("floorPeriodDays", "7"),
                returnAfterAddHabit = o.optBoolean("returnAfterAddHabit", false),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            )
        }.getOrNull()
    }
}
