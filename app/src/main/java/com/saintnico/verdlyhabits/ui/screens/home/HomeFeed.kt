package com.saintnico.verdlyhabits.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.engine.StatsEngine
import com.saintnico.verdlyhabits.engine.home.HomeFeedComposer
import com.saintnico.verdlyhabits.engine.home.HomeFeedPost
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember
import com.saintnico.verdlyhabits.ui.viewmodel.GrovePulse
import java.time.LocalDate

@Composable
fun rememberRankedHomeFeed(
    meUid: String,
    members: List<GroveMember>,
    pulse: GrovePulse,
    duo: DuoStreakState?,
    challenges: List<Challenge>,
    blockedUids: Set<String>,
    today: LocalDate = LocalDate.now(),
): List<HomeFeedPost> {
    val quote = remember(today.dayOfYear) { StatsEngine.dailyQuote(today.dayOfYear) }
    return remember(meUid, members, pulse, duo, challenges, blockedUids, today, quote) {
        HomeFeedComposer.compose(
            meUid = meUid,
            members = members,
            pulse = pulse,
            duo = duo,
            challenges = challenges,
            blockedUids = blockedUids,
            quote = quote,
            today = today,
        )
    }
}
