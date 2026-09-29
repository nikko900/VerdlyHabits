package com.saintnico.verdlyhabits.ui.screens.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.ui.components.social.rememberNudgeCooldownState
import com.saintnico.verdlyhabits.ui.screens.challenge.ChallengeScreen
import com.saintnico.verdlyhabits.ui.screens.duo.DuoScreen
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ChallengeViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState

enum class ArenaSegment { Duo, Challenges }

@Composable
fun ArenaScreen(
    initialSegment: ArenaSegment,
    duoState: DuoStreakState?,
    hasActiveChallenges: Boolean,
    challengeViewModel: ChallengeViewModel,
    billingViewModel: BillingViewModel,
    friendsViewModel: FriendsViewModel,
    coinViewModel: com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel,
    myUsername: String,
    myPhotoUrl: String?,
    onShowNotification: (String, Boolean) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onDeclineInvite: (String) -> Unit,
    onApplyGrace: ((String) -> Unit)?,
    onInviteBuddy: (uid: String, username: String, photoUrl: String?, onResult: (Boolean, String?) -> Unit) -> Unit,
    referralState: ReferralUiState,
    onOpenReferral: () -> Unit,
    openDuoNudge: Boolean = false,
    onGrantDuoSwap: ((((Boolean, String?) -> Unit) -> Unit))? = null,
) {
    val resolvedInitial = remember(initialSegment, hasActiveChallenges) {
        if (initialSegment == ArenaSegment.Challenges && !hasActiveChallenges) ArenaSegment.Duo else initialSegment
    }
    var segment by rememberSaveable { mutableIntStateOf(resolvedInitial.ordinal) }
    var requestBuddyPicker by remember { mutableStateOf(false) }
    var nudgeFromStrip by remember { mutableStateOf(openDuoNudge) }
    val buddyUid = duoState?.buddyUid.orEmpty()
    val nudgeCooldown = rememberNudgeCooldownState(listOfNotNull(buddyUid.takeIf { it.isNotBlank() }))
    val buddyCooling = buddyUid.isNotBlank() && nudgeCooldown.isCooling(buddyUid)

    Column(Modifier.fillMaxSize()) {
        ArenaDuoStrip(
            duoState = duoState,
            nudgeOnCooldown = buddyCooling,
            onNudge = {
                segment = ArenaSegment.Duo.ordinal
                nudgeFromStrip = true
            },
            onInvite = {
                segment = ArenaSegment.Duo.ordinal
                requestBuddyPicker = true
            },
        )

        ArenaSegmentControl(
            selected = ArenaSegment.entries[segment],
            onSelect = { segment = it.ordinal },
        )

        Box(Modifier.weight(1f)) {
            when (ArenaSegment.entries[segment]) {
                ArenaSegment.Challenges -> {
                    ChallengeScreen(
                        viewModel = challengeViewModel,
                        billingViewModel = billingViewModel,
                        friendsViewModel = friendsViewModel,
                        coinViewModel = coinViewModel,
                        onShowNotification = onShowNotification,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToMemberProfile = onNavigateToMemberProfile,
                    )
                }
                ArenaSegment.Duo -> {
                    DuoScreen(
                        duoState = duoState,
                        friendsViewModel = friendsViewModel,
                        myUsername = myUsername,
                        myPhotoUrl = myPhotoUrl,
                        hasDuoBuddy = duoState?.status == "active" || duoState?.status == "pending",
                        onAcceptInvite = onAcceptInvite,
                        onDeclineInvite = onDeclineInvite,
                        onApplyGrace = onApplyGrace,
                        onInviteBuddy = onInviteBuddy,
                        onNavigateToMemberProfile = onNavigateToMemberProfile,
                        onShowNotification = { msg, err -> onShowNotification(msg, err) },
                        referralState = referralState,
                        onOpenReferral = onOpenReferral,
                        embeddedInArena = true,
                        openBuddyPickerOnLaunch = requestBuddyPicker,
                        onBuddyPickerHandled = { requestBuddyPicker = false },
                        openNudgeOnLaunch = nudgeFromStrip && !buddyCooling,
                        onNudgeLaunchHandled = { nudgeFromStrip = false },
                        onGrantDuoSwap = onGrantDuoSwap,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArenaSegmentControl(
    selected: ArenaSegment,
    onSelect: (ArenaSegment) -> Unit,
) {
    val gold = Color(0xFFFFD54F)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.35f))
            .padding(4.dp),
    ) {
        ArenaSegment.entries.forEach { tab ->
            val active = tab == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(tab) },
                    ),
                color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when (tab) {
                            ArenaSegment.Duo -> "Duo"
                            ArenaSegment.Challenges -> "Challenges"
                        },
                        fontFamily = if (active) frauncesFamily else dmSansFamily,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (active) {
                            if (tab == ArenaSegment.Challenges) gold else Color(0xFF52B788)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(0.55f)
                        },
                    )
                }
            }
        }
    }
}
