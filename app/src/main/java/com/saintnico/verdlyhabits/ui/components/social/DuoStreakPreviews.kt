package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saintnico.verdlyhabits.data.remote.firestore.DuoMilestoneReached
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState

private val PreviewBg = Color(0xFF0A120E)

@Preview(name = "Duo — Active", showBackground = true, backgroundColor = 0xFF0A120E)
@Composable
private fun PreviewDuoActive() {
    Column(Modifier.background(PreviewBg).padding(16.dp)) {
        DuoStreakCard(
            state = DuoStreakState(
                status = "active",
                buddyUsername = "Alex",
                streakDays = 7,
                myProgress = "3/5",
                buddyProgress = "5/5",
                buddyDoneToday = true,
                countdownHours = 4,
                countdownMinutes = 12,
            ),
            onAccept = {},
            onDecline = {},
        )
    }
}

@Preview(name = "Duo — At risk", showBackground = true, backgroundColor = 0xFF0A120E)
@Composable
private fun PreviewDuoAtRisk() {
    Column(Modifier.background(PreviewBg).padding(16.dp)) {
        DuoStreakCard(
            state = DuoStreakState(
                status = "active",
                buddyUsername = "Alex",
                streakDays = 12,
                myProgress = "1/5",
                buddyProgress = "2/5",
                streakAtRisk = true,
                countdownHours = 2,
                countdownMinutes = 18,
                graceAvailable = true,
            ),
            onAccept = {},
            onDecline = {},
            onApplyGrace = {},
        )
    }
}

@Preview(name = "Duo — Both done", showBackground = true, backgroundColor = 0xFF0A120E)
@Composable
private fun PreviewDuoBothDone() {
    Column(Modifier.background(PreviewBg).padding(16.dp)) {
        DuoStreakCard(
            state = DuoStreakState(
                status = "active",
                buddyUsername = "Alex",
                streakDays = 14,
                myProgress = "5/5",
                buddyProgress = "5/5",
                myDoneToday = true,
                buddyDoneToday = true,
                bothDoneToday = true,
            ),
            onAccept = {},
            onDecline = {},
        )
    }
}

@Preview(name = "Duo — Streak broken", showBackground = true, backgroundColor = 0xFF0A120E)
@Composable
private fun PreviewDuoBroken() {
    Column(Modifier.background(PreviewBg).padding(16.dp).fillMaxWidth()) {
        DuoStreakBrokenBanner(previousStreak = 12, buddyName = "Alex", onDismiss = {})
    }
}

@Preview(name = "Duo — Milestone 7d", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDuoMilestone() {
    DuoMilestoneCelebration(
        milestone = DuoMilestoneReached(7, "Alex", "preview"),
        onDismiss = {},
        onShare = {},
    )
}
