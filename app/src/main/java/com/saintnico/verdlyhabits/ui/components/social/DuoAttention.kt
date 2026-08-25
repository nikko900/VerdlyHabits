package com.saintnico.verdlyhabits.ui.components.social

import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState

/** True when the duo hub deserves a glow / badge (invite, risk, or buddy finished first). */
fun DuoStreakState?.needsDuoAttention(): Boolean {
    if (this == null) return false
    return isIncomingInvite ||
        streakAtRisk ||
        (status == "active" && buddyDoneToday && !myDoneToday)
}

/** True when an active or pending duo pair already exists — blocks sending a new invite. */
fun DuoStreakState?.blocksNewDuoInvite(): Boolean =
    this != null && (status == "active" || status == "pending")
