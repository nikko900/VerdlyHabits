package com.saintnico.verdlyhabits.ui.screens.duo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.NudgeSituation
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.engine.DuoStreakEngine
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.components.social.DuoStreakCard
import com.saintnico.verdlyhabits.ui.components.social.FriendsConnectionsSection
import com.saintnico.verdlyhabits.ui.components.social.NudgeSheetHost
import com.saintnico.verdlyhabits.ui.components.social.NudgeTarget
import com.saintnico.verdlyhabits.ui.components.social.blocksNewDuoInvite
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState

private val Mint = Color(0xFF52B788)
private val Gold = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoScreen(
    duoState: DuoStreakState?,
    friendsViewModel: FriendsViewModel,
    myUsername: String,
    myPhotoUrl: String?,
    hasDuoBuddy: Boolean,
    onAcceptInvite: (String) -> Unit,
    onDeclineInvite: (String) -> Unit,
    onApplyGrace: ((String) -> Unit)?,
    onInviteBuddy: (uid: String, username: String, photoUrl: String?, onResult: (Boolean, String?) -> Unit) -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
    referralState: ReferralUiState = ReferralUiState(),
    onOpenReferral: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    embeddedInArena: Boolean = false,
    openBuddyPickerOnLaunch: Boolean = false,
    onBuddyPickerHandled: () -> Unit = {},
    openNudgeOnLaunch: Boolean = false,
    onNudgeLaunchHandled: () -> Unit = {},
) {
    val friends by friendsViewModel.friendSummaries.collectAsState()
    var requestBuddyPicker by remember { mutableStateOf(openBuddyPickerOnLaunch) }
    var nudgeTarget by remember { mutableStateOf<NudgeTarget?>(null) }
    val canInviteDuo = !duoState.blocksNewDuoInvite()

    LaunchedEffect(openBuddyPickerOnLaunch) {
        if (openBuddyPickerOnLaunch) {
            requestBuddyPicker = true
            onBuddyPickerHandled()
        }
    }

    LaunchedEffect(openNudgeOnLaunch, duoState) {
        if (openNudgeOnLaunch && duoState?.buddyUid?.isNotBlank() == true) {
            nudgeTarget = NudgeTarget(
                uid = duoState.buddyUid,
                username = duoState.buddyUsername.ifBlank { "your buddy" },
                photoUrl = duoState.buddyPhotoUrl,
                surface = NudgeSurface.DUO,
                situation = if (duoState.streakAtRisk) NudgeSituation.DUO_AT_RISK else NudgeSituation.DUO_WAITING_ON_THEM,
                contextId = duoState.pairId,
            )
            onNudgeLaunchHandled()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!embeddedInArena) {
                TopAppBar(
                title = {
                    Column {
                        Text(
                            "Duo & connections",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                        Text(
                            duoSubtitle(duoState),
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (embeddedInArena) PaddingValues(0.dp) else padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (duoState != null) {
                DuoStreakCard(
                    state = duoState,
                    onAccept = { onAcceptInvite(duoState.pairId) },
                    onDecline = { onDeclineInvite(duoState.pairId) },
                    onApplyGrace = onApplyGrace?.let { cb -> { cb(duoState.pairId) } },
                    onNudgeBuddy = if (duoState.buddyUid.isNotBlank()) {
                        {
                            nudgeTarget = NudgeTarget(
                                uid = duoState.buddyUid,
                                username = duoState.buddyUsername.ifBlank { "your buddy" },
                                photoUrl = duoState.buddyPhotoUrl,
                                surface = NudgeSurface.DUO,
                                situation = if (duoState.streakAtRisk) {
                                    NudgeSituation.DUO_AT_RISK
                                } else {
                                    NudgeSituation.DUO_WAITING_ON_THEM
                                },
                                contextId = duoState.pairId,
                            )
                        }
                    } else null,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(10.dp))
                DuoMilestoneStrip(streakDays = duoState.streakDays)
                Spacer(Modifier.height(16.dp))
            } else {
                DuoEmptyHero(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    friendCount = friends.size,
                    onPickBuddy = if (canInviteDuo) {{ requestBuddyPicker = true }} else null,
                )
                Spacer(Modifier.height(12.dp))
            }

            FriendsConnectionsSection(
                friendsViewModel = friendsViewModel,
                primary = MaterialTheme.colorScheme.primary,
                onBg = MaterialTheme.colorScheme.onBackground,
                onNavigateToMemberProfile = onNavigateToMemberProfile,
                onShowNotification = onShowNotification,
                hasDuoBuddy = duoState.blocksNewDuoInvite(),
                myUsername = myUsername,
                myPhotoUrl = myPhotoUrl,
                onInviteAccountabilityBuddy = if (canInviteDuo) onInviteBuddy else null,
                referralState = referralState,
                onOpenReferral = onOpenReferral,
                modifier = Modifier.fillMaxWidth(),
                requestOpenBuddyPicker = requestBuddyPicker,
                onRequestOpenBuddyPickerHandled = { requestBuddyPicker = false },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    NudgeSheetHost(
        target = nudgeTarget,
        onDismiss = { nudgeTarget = null },
        onSent = { onShowNotification("Nudge sent", false) },
        onError = { message -> onShowNotification(message, true) },
    )
}

@Composable
private fun DuoActiveHero(duoState: DuoStreakState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Mint.copy(0.35f), Gold.copy(0.15f))),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Mint.copy(0.14f), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Mint.copy(0.12f))
                        .border(1.dp, Mint.copy(0.22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    StreakFlame(streak = duoState.streakDays.coerceAtLeast(1), size = 28.dp)
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        "${duoState.streakDays}-day duo streak",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        "with @${duoState.buddyUsername}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DuoEmptyHero(
    modifier: Modifier = Modifier,
    friendCount: Int = 0,
    onPickBuddy: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Mint.copy(0.12f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Start a duo streak",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Text(
                if (friendCount > 0) {
                    "You have $friendCount ${if (friendCount == 1) "friend" else "friends"} ready — pick one to start a duo streak. When you both finish every habit on the same day, your streak grows."
                } else {
                    "Add a friend first (search below), then invite them to a duo. When you both finish every habit on the same day, your streak grows — miss a day and it resets."
                },
                fontFamily = dmSansFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
            )
            if (onPickBuddy != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPickBuddy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold.copy(alpha = 0.92f), contentColor = Color(0xFF1A1208)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (friendCount > 0) "Choose duo partner" else "Add friends & start duo",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DuoMilestoneStrip(streakDays: Int) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Milestones",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (day in DuoStreakEngine.CELEBRATION_MILESTONES) {
                val reached = streakDays >= day
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (reached) Mint.copy(0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                            )
                            .border(
                                1.dp,
                                if (reached) Mint.copy(0.35f) else MaterialTheme.colorScheme.outline.copy(0.12f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${day}d",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (reached) Mint else MaterialTheme.colorScheme.onBackground.copy(0.35f),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (day) {
                            3 -> "Spark"
                            7 -> "Week"
                            14 -> "Fortnight"
                            21 -> "Habit"
                            30 -> "Legend"
                            else -> ""
                        },
                        fontSize = 9.sp,
                        fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (reached) MaterialTheme.colorScheme.onBackground.copy(0.75f)
                        else MaterialTheme.colorScheme.onBackground.copy(0.3f),
                    )
                }
            }
        }
    }
}

private fun duoSubtitle(state: DuoStreakState?): String = when {
    state == null -> "Invite a friend to start"
    state.isIncomingInvite -> "${state.buddyUsername} invited you"
    state.status == "pending" -> "Waiting for ${state.buddyUsername}"
    state.bothDoneToday -> "Both locked in today"
    state.streakAtRisk -> "Finish before midnight"
    state.streakDays > 0 -> "${state.streakDays}-day streak with ${state.buddyUsername}"
    else -> "Complete habits together daily"
}
