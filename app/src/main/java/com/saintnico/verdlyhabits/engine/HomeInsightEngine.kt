package com.saintnico.verdlyhabits.engine

/**
 * Homepage brief. Same numbers as [StatsInsightEngine], trimmed so Today
 * stays quiet until someone asks for the full read.
 */
object HomeInsightEngine {

    data class TodayNote(
        val eyebrow: String,
        val headline: String,
        val support: String,
        val open: Open,
    )

    enum class Open { Week, Consistency, Habit, Signal }

    fun todayNote(
        report: StatsInsightEngine.Report,
    ): TodayNote {
        val today = report.week.days.lastOrNull()
        val leak = report.habits.firstOrNull { it.missStreak >= 2 }
        val win = report.signals.firstOrNull { it.kind == StatsInsightEngine.SignalKind.WIN }
        return when {
            today != null && today.missed.isNotEmpty() -> TodayNote(
                eyebrow = "STILL OPEN",
                headline = today.missed.first(),
                support = if (today.missed.size == 1) {
                    "Close this and the day is clean."
                } else {
                    "${today.missed.size} still due. Start with ${today.missed.first()}."
                },
                open = Open.Week,
            )
            leak != null -> TodayNote(
                eyebrow = "NEEDS YOU",
                headline = leak.habit.title,
                support = leak.nextMove,
                open = Open.Habit,
            )
            today != null && today.due > 0 && today.missed.isEmpty() -> TodayNote(
                eyebrow = "CLEAN",
                headline = "Every due habit is planted.",
                support = report.week.nextMove,
                open = Open.Week,
            )
            win != null -> TodayNote(
                eyebrow = "WIN",
                headline = win.title,
                support = win.body,
                open = Open.Signal,
            )
            else -> TodayNote(
                eyebrow = "TODAY",
                headline = report.score.headline,
                support = report.score.nextMove,
                open = Open.Consistency,
            )
        }
    }
}
