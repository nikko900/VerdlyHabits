package com.saintnico.verdlyhabits.data.model

/** Where a nudge was sent from — shapes which preset floats to the top and where the push deep-links. */
enum class NudgeSurface(val wireValue: String) {
    PROFILE("profile"),
    DUO("duo"),
    CHALLENGE("challenge"),
}

/** Situational hint used to reorder presets so the most relevant nudge is pre-selected. */
enum class NudgeSituation {
    GENERAL,
    DUO_AT_RISK,
    DUO_WAITING_ON_THEM,
    CHALLENGE_BEHIND,
    CHALLENGE_OVERTAKEN,
}

data class NudgePreset(val id: String, val message: String, val iconKey: String)

/** Fixed set of nudge messages — TikTok-style pokes, but with real words attached. */
object NudgeMessages {

    val presets: List<NudgePreset> = listOf(
        NudgePreset("streak", "Don't break our streak — jump in.", "flame"),
        NudgePreset("waiting", "I'm done. Your move.", "waiting"),
        NudgePreset("catchup", "You're falling behind — catch up.", "bolt"),
        NudgePreset("overtaken", "I just overtook you. Come get your spot back.", "trophy"),
        NudgePreset("comeback", "Missed you today. Let's go.", "wave"),
        NudgePreset("checkin", "Quick check-in — how's it going?", "star"),
        NudgePreset("proud", "Proud of you. Keep it up.", "heart"),
    )

    /** Presets reordered so the most contextually relevant nudge appears first. */
    fun forSituation(situation: NudgeSituation): List<NudgePreset> {
        val priorityId = when (situation) {
            NudgeSituation.DUO_AT_RISK -> "streak"
            NudgeSituation.DUO_WAITING_ON_THEM -> "waiting"
            NudgeSituation.CHALLENGE_BEHIND -> "catchup"
            NudgeSituation.CHALLENGE_OVERTAKEN -> "overtaken"
            NudgeSituation.GENERAL -> return presets
        }
        val priority = presets.firstOrNull { it.id == priorityId } ?: return presets
        return listOf(priority) + presets.filter { it.id != priorityId }
    }
}
