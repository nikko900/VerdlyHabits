package com.saintnico.verdlyhabits.data.model

/** Where a nudge was sent from — shapes which preset floats to the top and where the push deep-links. */
enum class NudgeSurface(val wireValue: String) {
    PROFILE("profile"),
    DUO("duo"),
    CHALLENGE("challenge"),
    GROVE("grove"),
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

/**
 * Fixed set of nudge messages. Short on purpose: each send overwrites one
 * pair document, and the optional handwritten note is capped in the sheet.
 */
object NudgeMessages {

    const val NOTE_MAX_CHARS = 80
    const val MESSAGE_MAX_CHARS = 160

    val emojiPalette: List<String> = listOf(
        "🔥", "🌱", "💚", "⚡", "🏆", "👀", "🫶", "💪",
        "✨", "☕", "🌿", "🌟", "🤝", "🙌", "😎", "🎯",
    )

    val presets: List<NudgePreset> = listOf(
        NudgePreset("streak", "Don't break our streak. Jump in. 🔥", "flame"),
        NudgePreset("waiting", "I'm done. Your move. ⏳", "waiting"),
        NudgePreset("catchup", "You're falling behind. Catch up. ⚡", "bolt"),
        NudgePreset("overtaken", "I just overtook you. Come get your spot back. 🏆", "trophy"),
        NudgePreset("comeback", "Missed you today. Let's go. 👋", "wave"),
        NudgePreset("checkin", "Quick check in. How's it going? ✨", "star"),
        NudgePreset("proud", "Proud of you. Keep it up. 💚", "heart"),
        NudgePreset("grove", "The grove is waiting on you. 🌱", "wave"),
        NudgePreset("tiny", "One tiny plant. That's all. 🌿", "bolt"),
        NudgePreset("believe", "I believe in you. Show up. 💪", "heart"),
        NudgePreset("coffee", "Coffee's on. Habits too. ☕", "star"),
        NudgePreset("soft", "Soft nudge. You've got this. 🫶", "heart"),
        NudgePreset("week", "Let's finish the week together. 🤝", "trophy"),
        NudgePreset("future", "Your future self is watching. 🌟", "star"),
        NudgePreset("eyes", "Don't ghost the grove. 👀", "waiting"),
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

    fun compose(preset: String, note: String): String {
        val base = preset.trim()
        val extra = note.trim().replace('\n', ' ').take(NOTE_MAX_CHARS)
        val joined = if (extra.isBlank()) base else "$base $extra"
        return joined.take(MESSAGE_MAX_CHARS)
    }
}
