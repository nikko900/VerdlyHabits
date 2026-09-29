package com.saintnico.verdlyhabits.ui.components.profilepremium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.ui.components.ProfileAvatar
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.theme.jetBrainsMonoFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val SheetMint = Color(0xFF52B788)
private val SheetGold = Color(0xFFFFB300)
private val SheetCoral = Color(0xFFFF6B6B)
private val SheetPurple = Color(0xFF7B6CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileExploreHost(
    explore: ProfileExplore?,
    statsState: UserStatsUiState,
    career: ProfileSocialEngine.CareerInputs,
    snapshot: ProfileSocialEngine.ProfileSocialSnapshot,
    friends: List<FriendSummary>,
    duoState: DuoStreakState?,
    activeChallenges: List<Challenge>,
    allChallenges: List<Challenge>,
    userId: String,
    referralState: ReferralUiState,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenFriendsGrove: () -> Unit,
) {
    if (explore == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (explore) {
                ProfileExplore.Xp -> XpExplore(statsState, accent)
                ProfileExplore.Streak -> StreakExplore(statsState, career, duoState, accent)
                ProfileExplore.Challenges -> ChallengesExplore(
                    activeChallenges = activeChallenges,
                    career = career,
                    userId = userId,
                    accent = accent,
                    onOpenChallenges = {
                        onDismiss()
                        onOpenChallenges()
                    },
                )
                ProfileExplore.Completions -> CompletionsExplore(statsState, career, accent)
                ProfileExplore.Crew -> CrewExplore(
                    snapshot = snapshot,
                    friends = friends,
                    duoState = duoState,
                    accent = accent,
                    onOpenPerson = {
                        onDismiss()
                        onOpenProfile(it)
                    },
                    onOpenFriendsGrove = {
                        onDismiss()
                        onOpenFriendsGrove()
                    },
                )
                ProfileExplore.Connections -> ConnectionsExplore(
                    friends = friends,
                    accent = accent,
                    onOpenPerson = {
                        onDismiss()
                        onOpenProfile(it)
                    },
                    onOpenFriendsGrove = {
                        onDismiss()
                        onOpenFriendsGrove()
                    },
                )
                ProfileExplore.Motivated -> MotivatedExplore(
                    people = snapshot.motivatedPeople,
                    accent = accent,
                    onOpenPerson = {
                        onDismiss()
                        onOpenProfile(it)
                    },
                )
                ProfileExplore.Wins -> WinsExplore(
                    allChallenges = allChallenges,
                    userId = userId,
                    career = career,
                    accent = accent,
                    onOpenChallenges = {
                        onDismiss()
                        onOpenChallenges()
                    },
                )
                ProfileExplore.Duo -> DuoExplore(
                    duoState = duoState,
                    accent = accent,
                    onOpenBuddy = { uid ->
                        onDismiss()
                        onOpenProfile(uid)
                    },
                    onOpenFriendsGrove = {
                        onDismiss()
                        onOpenFriendsGrove()
                    },
                )
                ProfileExplore.Invites -> InvitesExplore(
                    referralState = referralState,
                    snapshot = snapshot,
                    accent = accent,
                    onOpenFriendsGrove = {
                        onDismiss()
                        onOpenFriendsGrove()
                    },
                )
            }
        }
    }
}

@Composable
private fun XpExplore(stats: UserStatsUiState, accent: Color) {
    val nextTitle = GamificationEngine.levelTitle(stats.level + 1)
    Eyebrow("EXPERIENCE")
    Text(
        "Level ${stats.level} · ${stats.levelTitle}",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Text(
        "${stats.totalXp}",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        color = accent,
    )
    Text("lifetime XP", fontFamily = dmSansFamily, fontSize = 13.sp, color = muted())
    LinearProgressIndicator(
        progress = { stats.progressToNextLevel.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        color = accent,
        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Lv ${stats.level}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
        Text("Lv ${stats.level + 1} · $nextTitle", fontSize = 11.sp, color = muted())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${stats.xpToNextLevel}", "to next level", accent)
        MiniStat("${stats.xpEarnedToday}", "earned today", SheetGold)
    }
    Story(
        "XP is the quiet ledger of showing up. Completions, streak bonuses, perfect days, " +
            "and on-time windows all add to this number. Nothing here is decorative.",
    )
    GlowNote(
        "Next title: $nextTitle. ${stats.xpToNextLevel} XP still to plant.",
        SheetGold,
        Icons.Rounded.AutoAwesome,
    )
    NextMove("Finish today's remaining habits. Streak bonus XP stacks as the days hold.")
}

@Composable
private fun StreakExplore(
    stats: UserStatsUiState,
    career: ProfileSocialEngine.CareerInputs,
    duoState: DuoStreakState?,
    accent: Color,
) {
    val best = maxOf(stats.longestStreakEver, career.bestStreak)
    Eyebrow("STREAK")
    Text(
        if (best > 0) "Your longest fire is $best days" else "The first day is the hardest",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Text(
        "${stats.longestStreakEver}",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        color = Color(0xFFFF7043),
    )
    Text("best personal streak", fontFamily = dmSansFamily, fontSize = 13.sp, color = muted())
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${career.bestStreak}", "arena best", SheetGold)
        MiniStat("${duoState?.streakDays ?: 0}", "duo days", SheetCoral)
        MiniStat("${stats.streakShields}", "shields", SheetPurple)
    }
    Story(
        if (best >= 7) {
            "A week is a rhythm. After that, streaks stop being luck and start being identity."
        } else {
            "Streaks here count real days you planted. Miss a day and the number resets. Shields can save one."
        },
    )
    if ((duoState?.streakDays ?: 0) > 0) {
        GlowNote(
            "Duo with @${duoState?.buddyUsername.orEmpty().ifBlank { "buddy" }} is ${duoState?.streakDays} days. Both of you have to lock in.",
            SheetCoral,
            Icons.Filled.LocalFireDepartment,
        )
    }
    NextMove(
        if ((duoState?.buddyDoneToday == true) && duoState?.myDoneToday != true) {
            "Your duo already planted today. Finish yours before midnight."
        } else {
            "Protect the current streak first. The record only moves if today holds."
        },
    )
}

@Composable
private fun ChallengesExplore(
    activeChallenges: List<Challenge>,
    career: ProfileSocialEngine.CareerInputs,
    userId: String,
    accent: Color,
    onOpenChallenges: () -> Unit,
) {
    Eyebrow("CHALLENGES")
    Text(
        if (activeChallenges.isEmpty()) "No live arenas right now"
        else "${activeChallenges.size} live arena${if (activeChallenges.size == 1) "" else "s"}",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${career.wins}", "wins", SheetGold)
        MiniStat("${career.podiums}", "podiums", accent)
        MiniStat("${career.totalShared}", "joined", SheetPurple)
    }
    if (activeChallenges.isEmpty()) {
        Story("Challenges are where a habit gets witnesses. Create one, or join a friend's arena.")
    } else {
        Story("Tap a card to open that arena. Rank, streak, and days left are live.")
        activeChallenges.forEach { ch ->
            val rank = ch.rankOf(userId).takeIf { it > 0 }
            val streak = ch.streakFor(userId)
            PersonRow(
                title = ch.habitName,
                subtitle = buildString {
                    if (rank != null) append("#$rank · ")
                    append("$streak day streak · ${ch.daysRemaining().toInt()}d left")
                },
                photoUrl = null,
                accent = accent,
                onClick = onOpenChallenges,
            )
        }
    }
    NextMove("Open Challenges to start a new arena or defend a live one.")
}

@Composable
private fun CompletionsExplore(
    stats: UserStatsUiState,
    career: ProfileSocialEngine.CareerInputs,
    accent: Color,
) {
    val since = Instant.ofEpochMilli(stats.memberSince)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val days = ChronoUnit.DAYS.between(since, java.time.LocalDate.now()).coerceAtLeast(1)
    val pace = stats.totalCompletions.toFloat() / days.toFloat()
    Eyebrow("COMPLETIONS")
    Text(
        "Marks on the ledger",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    )
    Text(
        "${stats.totalCompletions}",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        color = accent,
    )
    Text("habits finished all-time", fontFamily = dmSansFamily, fontSize = 13.sp, color = muted())
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat(String.format("%.1f", pace), "per day", accent)
        MiniStat("$days", "days in", SheetPurple)
        MiniStat("${career.totalShared}", "arenas", SheetGold)
    }
    Story(
        "Every completion is a day you chose the habit over the skip. " +
            "This number does not care about mood. It only counts the plant.",
    )
    GlowNote(
        "Building since ${since.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}.",
        accent,
        Icons.Rounded.AutoAwesome,
    )
    NextMove("One more completion today moves both this count and your streak.")
}

@Composable
private fun CrewExplore(
    snapshot: ProfileSocialEngine.ProfileSocialSnapshot,
    friends: List<FriendSummary>,
    duoState: DuoStreakState?,
    accent: Color,
    onOpenPerson: (String) -> Unit,
    onOpenFriendsGrove: () -> Unit,
) {
    val stats = snapshot.stats
    Eyebrow("YOUR CREW")
    Text(
        "People watching your rhythm",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${stats.friendsCount}", "friends", accent)
        MiniStat(if (stats.duoStreakDays > 0) "${stats.duoStreakDays}" else "—", "duo", SheetCoral)
        MiniStat("${stats.challengeWins}", "wins", SheetGold)
        MiniStat("${stats.referralsSent}", "invites", SheetPurple)
    }
    Story(
        if (snapshot.peopleMotivatedToday > 0) {
            "${snapshot.peopleMotivatedToday} people locked in today because you are in the same duo or arena."
        } else {
            "Your crew is friends, your duo, and anyone sharing an arena. When they plant, it shows here."
        },
    )
    if (snapshot.motivatedPeople.isNotEmpty()) {
        Text("Motivated today", fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        snapshot.motivatedPeople.forEach { person ->
            PersonRow(
                title = "@${person.username}",
                subtitle = person.reason,
                photoUrl = person.photoUrl,
                accent = accent,
                onClick = { onOpenPerson(person.uid) },
            )
        }
    }
    if (friends.isNotEmpty()) {
        Text("Grove", fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        friends.take(8).forEach { friend ->
            PersonRow(
                title = "@${friend.username.ifBlank { "rival" }}",
                subtitle = when {
                    (friend.plantedCountToday) > 0 -> "Planted today · ${friend.currentStreakPublic}d streak"
                    friend.currentStreakPublic > 0 -> "${friend.currentStreakPublic}-day streak"
                    else -> "In your grove"
                },
                photoUrl = friend.photoUrl,
                accent = accent,
                onClick = { onOpenPerson(friend.uid) },
            )
        }
    }
    if (duoState != null && duoState.buddyUid.isNotBlank()) {
        GlowNote(
            "Duo with @${duoState.buddyUsername.ifBlank { "buddy" }} · ${duoState.streakDays} days",
            SheetCoral,
            Icons.Filled.LocalFireDepartment,
        )
    }
    NextMove("Open the grove to add someone, or start a duo.")
    Text(
        "Open grove",
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable(onClick = onOpenFriendsGrove)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        fontWeight = FontWeight.SemiBold,
        color = accent,
    )
}

@Composable
private fun ConnectionsExplore(
    friends: List<FriendSummary>,
    accent: Color,
    onOpenPerson: (String) -> Unit,
    onOpenFriendsGrove: () -> Unit,
) {
    Eyebrow("CONNECTIONS")
    Text(
        if (friends.isEmpty()) "Your grove is still empty"
        else "${friends.size} ${if (friends.size == 1) "friend" else "friends"} in the grove",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Story(
        "These are people you can challenge, duo with, and see plant. Tap a name to open their card.",
    )
    if (friends.isEmpty()) {
        NextMove("Search a username in the grove and send a request.")
    } else {
        friends.forEach { friend ->
            PersonRow(
                title = "@${friend.username.ifBlank { "rival" }}",
                subtitle = buildString {
                    if (friend.equippedTitleLabel?.isNotBlank() == true) {
                        append(friend.equippedTitleLabel)
                        append(" · ")
                    }
                    append(
                        if (friend.plantedCountToday > 0) "Planted today"
                        else "${friend.currentStreakPublic}d streak",
                    )
                },
                photoUrl = friend.photoUrl,
                accent = accent,
                onClick = { onOpenPerson(friend.uid) },
            )
        }
    }
    Text(
        "Open Friends Grove",
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable(onClick = onOpenFriendsGrove)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        fontWeight = FontWeight.SemiBold,
        color = accent,
    )
}

@Composable
private fun MotivatedExplore(
    people: List<ProfileSocialEngine.MotivatedPerson>,
    accent: Color,
    onOpenPerson: (String) -> Unit,
) {
    Eyebrow("MOTIVATED TODAY")
    Text(
        if (people.isEmpty()) "Nobody locked in through you yet"
        else "${people.size} ${if (people.size == 1) "person" else "people"} you moved today",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Story(
        "This is not a vanity count. It is friends who finished today in a duo or arena you share.",
    )
    if (people.isEmpty()) {
        NextMove("Keep a duo or a live challenge going. When they plant, they land here.")
    } else {
        people.forEach { person ->
            PersonRow(
                title = "@${person.username}",
                subtitle = person.reason,
                photoUrl = person.photoUrl,
                accent = accent,
                onClick = { onOpenPerson(person.uid) },
            )
        }
        GlowNote("You are the reason they did not skip.", SheetCoral, Icons.Rounded.Favorite)
    }
}

@Composable
private fun WinsExplore(
    allChallenges: List<Challenge>,
    userId: String,
    career: ProfileSocialEngine.CareerInputs,
    accent: Color,
    onOpenChallenges: () -> Unit,
) {
    val wins = allChallenges.filter { !it.isActive && it.members.contains(userId) }
        .filter { ch ->
            val order = ch.resultSnapshot?.finalLeaderboard?.map { it.uid }?.takeIf { it.isNotEmpty() }
                ?: ch.finalStanding().map { it.first }
            order.indexOf(userId) == 0
        }
    Eyebrow("WINS")
    Text(
        if (career.wins == 0) "The first crown is still open"
        else "${career.wins} arena${if (career.wins == 1) "" else "s"} taken",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${career.wins}", "first place", SheetGold)
        MiniStat("${career.podiums}", "top 3", accent)
        MiniStat("${career.totalShared}", "played", SheetPurple)
    }
    Story("Wins count finished arenas where you stood first when the clock closed.")
    wins.take(8).forEach { ch ->
        PersonRow(
            title = ch.habitName,
            subtitle = ch.stake.ifBlank { "Arena closed" },
            photoUrl = null,
            accent = SheetGold,
            onClick = onOpenChallenges,
        )
    }
    NextMove("Open Challenges and start another arena while the form is hot.")
}

@Composable
private fun DuoExplore(
    duoState: DuoStreakState?,
    accent: Color,
    onOpenBuddy: (String) -> Unit,
    onOpenFriendsGrove: () -> Unit,
) {
    Eyebrow("DUO")
    if (duoState == null || duoState.buddyUid.isBlank()) {
        Text(
            "No accountability pair yet",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
        )
        Story("A duo streak only grows when both of you finish every habit on the same day.")
        NextMove("Pick a friend in the grove and send a duo invite.")
        Text(
            "Find a buddy",
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.12f))
                .clickable(onClick = onOpenFriendsGrove)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        return
    }
    Text(
        "${duoState.streakDays}-day duo with @${duoState.buddyUsername.ifBlank { "buddy" }}",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat(if (duoState.myDoneToday) "Done" else "Open", "you", accent)
        MiniStat(if (duoState.buddyDoneToday) "Done" else "Open", "them", SheetCoral)
        MiniStat("${duoState.streakDays}", "days", SheetGold)
    }
    PersonRow(
        title = "@${duoState.buddyUsername.ifBlank { "buddy" }}",
        subtitle = if (duoState.bothDoneToday) "Both locked in today" else duoState.buddyProgress,
        photoUrl = duoState.buddyPhotoUrl,
        accent = accent,
        onClick = { onOpenBuddy(duoState.buddyUid) },
    )
    if (duoState.streakAtRisk) {
        GlowNote("Streak at risk. Midnight is the line.", SheetCoral, Icons.Filled.LocalFireDepartment)
    }
    NextMove(
        if (!duoState.myDoneToday) "Finish your habits to keep the duo alive."
        else if (!duoState.buddyDoneToday) "You are done. Nudge them before the day closes."
        else "Both planted. Protect this tomorrow.",
    )
}

@Composable
private fun InvitesExplore(
    referralState: ReferralUiState,
    snapshot: ProfileSocialEngine.ProfileSocialSnapshot,
    accent: Color,
    onOpenFriendsGrove: () -> Unit,
) {
    Eyebrow("INVITES")
    Text(
        "${referralState.qualifiedCount} of ${referralState.friendsRequired} friends in",
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    LinearProgressIndicator(
        progress = {
            val need = referralState.friendsRequired.coerceAtLeast(1)
            (referralState.qualifiedCount.toFloat() / need).coerceIn(0f, 1f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        color = SheetGold,
        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${referralState.qualifiedCount}", "qualified", accent)
        MiniStat("${referralState.rewardDays}d", "premium reward", SheetGold)
        MiniStat("${snapshot.stats.referralsSent}", "sent", SheetPurple)
    }
    Story(
        "A qualified invite is a friend who joined with your link and started planting. " +
            "Bring ${referralState.friendsRequired} and the grove pays ${referralState.rewardDays} days of Premium.",
    )
    NextMove("Share your invite from Connect, or bring someone into the grove now.")
    Text(
        "Open Connect",
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable(onClick = onOpenFriendsGrove)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        fontWeight = FontWeight.SemiBold,
        color = accent,
    )
}

@Composable
private fun PersonRow(
    title: String,
    subtitle: String,
    photoUrl: String?,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            photoUri = photoUrl,
            size = 42.dp,
            fallbackTint = accent,
            fallbackBackground = accent.copy(alpha = 0.12f),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = muted(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        fontFamily = jetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.8.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
    )
}

@Composable
private fun Story(text: String) {
    Text(
        text,
        fontFamily = dmSansFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.82f),
    )
}

@Composable
private fun GlowNote(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.28f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, fontFamily = dmSansFamily, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun NextMove(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.TipsAndUpdates, null, tint = SheetPurple, modifier = Modifier.size(18.dp))
        Column {
            Text(
                "WHAT TO DO NEXT",
                fontFamily = jetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SheetPurple,
            )
            Spacer(Modifier.height(4.dp))
            Text(text, fontFamily = dmSansFamily, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = muted())
    }
}

@Composable
private fun muted() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
