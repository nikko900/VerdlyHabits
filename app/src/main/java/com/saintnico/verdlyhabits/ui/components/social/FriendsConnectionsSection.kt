package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.saintnico.verdlyhabits.ui.components.VerdlyActionButtonContent
import com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeJoinRequestDoc
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRelationship
import com.saintnico.verdlyhabits.ui.components.referral.ReferralPromoBanner
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.IncomingFriendRequestUi
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UsernameSearchResult
import androidx.lifecycle.viewmodel.compose.viewModel

private object ConnectionsShellColors {
    val accent = Color(0xFF74C69D)
    val accentBright = Color(0xFF95D5B2)
    val actionFill = Color(0xFF40916C)
    val onShell = Color.White
    val onShellMuted = Color(0xB3FFFFFF)
    val border = Color(0x45FFFFFF)
    val disabledFill = Color(0x24FFFFFF)
    val disabledLabel = Color(0x66FFFFFF)
    val gold = Color(0xFFFFB300)
}

@Composable
fun FriendsConnectionsSection(
    friendsViewModel: FriendsViewModel,
    primary: Color,
    onBg: Color,
    onNavigateToMemberProfile: (String) -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hasDuoBuddy: Boolean = false,
    myUsername: String = "",
    myPhotoUrl: String? = null,
    onInviteAccountabilityBuddy: ((uid: String, username: String, photoUrl: String?, onResult: (Boolean, String?) -> Unit) -> Unit)? = null,
    referralState: ReferralUiState = ReferralUiState(),
    onOpenReferral: () -> Unit = {},
    focusFindFriends: Boolean = false,
    onFocusFindFriendsHandled: () -> Unit = {},
    requestOpenBuddyPicker: Boolean = false,
    onRequestOpenBuddyPickerHandled: () -> Unit = {},
) {
    val incoming by friendsViewModel.incomingRequestsUi.collectAsState()
    val joinRequests by friendsViewModel.pendingChallengeJoinRequests.collectAsState()
    val friends by friendsViewModel.friendSummaries.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel()
    val viewerSharesLastActive by settingsViewModel.shareLastActive.collectAsState()
    val searchResults by friendsViewModel.usernameSearchResults.collectAsState()
    val searchError by friendsViewModel.usernameSearchError.collectAsState()
    val searchLoading by friendsViewModel.usernameSearchLoading.collectAsState()
    val discoverSuggestions by friendsViewModel.discoverSuggestions.collectAsState()
    val suggestionsLoading by friendsViewModel.suggestionsLoading.collectAsState()
    var showBuddyPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<UsernameSearchResult?>(null) }
    var connectingUid by remember { mutableStateOf<String?>(null) }
    var connectPhase by remember { mutableStateOf(VerdlyActionPhase.Idle) }
    val connectScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        friendsViewModel.onSearchQueryChanged(searchQuery)
    }

    LaunchedEffect(Unit) {
        friendsViewModel.loadDiscoverSuggestions()
    }

    LaunchedEffect(focusFindFriends) {
        if (focusFindFriends) {
            onFocusFindFriendsHandled()
        }
    }

    LaunchedEffect(requestOpenBuddyPicker) {
        if (requestOpenBuddyPicker) {
            showBuddyPicker = true
            onRequestOpenBuddyPickerHandled()
        }
    }

    fun sendDuoInvite(friend: FriendSummary) {
        onInviteAccountabilityBuddy?.invoke(friend.uid, friend.username, friend.photoUrl) { ok, err ->
            if (ok) {
                onShowNotification("Duo streak invite sent to @${friend.username}", false)
                showBuddyPicker = false
            } else {
                onShowNotification(err ?: "Couldn't send duo invite", true)
            }
        }
    }

    Column(modifier) {
        ConnectionsPageHeader(
            friendCount = friends.size,
            pendingRequests = incoming.size,
            arenaInvites = joinRequests.size,
            accent = primary,
        )

        Spacer(Modifier.height(14.dp))

        if (referralState.showBanner) {
            ReferralPromoBanner(
                qualifiedCount = referralState.qualifiedCount,
                friendsRequired = referralState.friendsRequired,
                rewardDays = referralState.rewardDays,
                onOpen = onOpenReferral,
                onDismiss = {},
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
            )
        }

        ConnectionsPremiumShell(accent = primary) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ConnectionsSectionHeader(
                    title = "Find someone",
                    icon = Icons.Default.Search,
                    tint = ConnectionsShellColors.accentBright,
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search name or username",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        com.saintnico.verdlyhabits.ui.components.VerdlyActionLottie(
                            rawRes = com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog.SearchLottie,
                            size = 22.dp,
                        )
                    },
                    trailingIcon = {
                        if (searchLoading) {
                            VerdlyLoadingIndicator(size = 20.dp)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { friendsViewModel.searchByUsername(searchQuery) },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ConnectionsShellColors.onShell,
                        unfocusedTextColor = ConnectionsShellColors.onShell.copy(alpha = 0.92f),
                        focusedBorderColor = ConnectionsShellColors.accent,
                        unfocusedBorderColor = ConnectionsShellColors.border,
                        cursorColor = ConnectionsShellColors.accentBright,
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                if (searchQuery.trim().isNotEmpty()) {
                    Text(
                        "Results update as you type",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                }
                searchError?.let { err ->
                    Text(err, fontSize = 12.sp, color = Color(0xFFFFB4A2))
                }
                searchResults.forEach { result ->
                    UsernameSearchResultRow(
                        result = result,
                        primary = primary,
                        canInviteDuo = !hasDuoBuddy && result.relationship == FriendRelationship.Friends,
                        connectPhase = if (connectingUid == result.uid) connectPhase else VerdlyActionPhase.Idle,
                        onConnect = {
                            if (connectingUid != null) return@UsernameSearchResultRow
                            connectingUid = result.uid
                            connectPhase = VerdlyActionPhase.Loading
                            friendsViewModel.sendFriendRequest(result.uid) { ok, err ->
                                if (ok) {
                                    connectPhase = VerdlyActionPhase.Success
                                    onShowNotification("Request sent to @${result.username}", false)
                                    connectScope.launch {
                                        delay(900)
                                        friendsViewModel.clearUsernameSearch()
                                        searchQuery = ""
                                        connectingUid = null
                                        connectPhase = VerdlyActionPhase.Idle
                                    }
                                } else {
                                    connectPhase = VerdlyActionPhase.Error
                                    onShowNotification(err ?: "Could not connect", true)
                                    connectingUid = null
                                    connectPhase = VerdlyActionPhase.Idle
                                }
                            }
                        },
                        onViewProfile = { onNavigateToMemberProfile(result.uid) },
                        onReport = { reportTarget = result },
                        onInviteDuo = if (!hasDuoBuddy && onInviteAccountabilityBuddy != null) {
                            {
                                sendDuoInvite(
                                    FriendSummary(
                                        uid = result.uid,
                                        username = result.username,
                                        photoUrl = result.photoUrl,
                                        membershipTier = result.membershipTier,
                                    ),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }

                if (searchQuery.trim().isEmpty()) {
                    DiscoverSuggestionsBlock(
                        suggestions = discoverSuggestions,
                        loading = suggestionsLoading,
                        primary = primary,
                        connectingUid = connectingUid,
                        connectPhase = connectPhase,
                        onConnect = { uid, username ->
                            if (connectingUid != null) return@DiscoverSuggestionsBlock
                            connectingUid = uid
                            connectPhase = VerdlyActionPhase.Loading
                            friendsViewModel.sendFriendRequest(uid) { ok, err ->
                                if (ok) {
                                    connectPhase = VerdlyActionPhase.Success
                                    onShowNotification("Request sent to @$username", false)
                                    connectScope.launch {
                                        delay(900)
                                        connectingUid = null
                                        connectPhase = VerdlyActionPhase.Idle
                                        friendsViewModel.loadDiscoverSuggestions(force = true)
                                    }
                                } else {
                                    connectPhase = VerdlyActionPhase.Error
                                    onShowNotification(err ?: "Could not connect", true)
                                    connectingUid = null
                                    connectPhase = VerdlyActionPhase.Idle
                                }
                            }
                        },
                        onViewProfile = onNavigateToMemberProfile,
                        onReport = { reportTarget = it },
                    )
                }

                if (joinRequests.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    ConnectionsSectionHeader(
                        title = "Arena invites",
                        icon = Icons.Default.SportsScore,
                        tint = ConnectionsShellColors.gold,
                        badge = joinRequests.size.toString(),
                    )
                    for (req in joinRequests) {
                        ChallengeJoinRequestRow(
                            request = req,
                            onApprove = {
                                friendsViewModel.approveChallengeJoinRequest(
                                    requestId = req.id,
                                    challengeId = req.challengeId,
                                    requesterUid = req.fromUid,
                                ) { ok, err ->
                                    if (ok) {
                                        onShowNotification("${req.fromUsername} joined ${req.challengeTitle}", false)
                                    } else {
                                        onShowNotification(err ?: "Could not approve", true)
                                    }
                                }
                            },
                            onDecline = {
                                friendsViewModel.declineChallengeJoinRequest(req.id) { ok, _ ->
                                    if (ok) onShowNotification("Declined", false)
                                }
                            },
                        )
                    }
                }

                if (incoming.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    ConnectionsSectionHeader(
                        title = "Friend requests",
                        icon = Icons.Default.Person,
                        tint = ConnectionsShellColors.accentBright,
                        badge = incoming.size.toString(),
                    )
                    for (item in incoming) {
                        IncomingRequestRow(
                            item = item,
                            primary = primary,
                            onAccept = {
                                friendsViewModel.acceptRequest(item.request.id) { ok, err ->
                                    if (ok) {
                                        onShowNotification("You're now connected with @${item.username}", false)
                                    } else {
                                        onShowNotification(err ?: "Accept failed", true)
                                    }
                                }
                            },
                            onDecline = {
                                friendsViewModel.declineRequest(item.request.id) { _, _ -> }
                            },
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                ConnectionsSectionHeader(
                    title = "Your circle",
                    icon = Icons.Default.Groups,
                    tint = ConnectionsShellColors.accent,
                    badge = if (friends.isNotEmpty()) "${friends.size}" else null,
                )

                if (friends.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "No connections yet",
                                fontFamily = frauncesFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.88f),
                            )
                            Text(
                                "Search by name or username above, tap someone on a challenge leaderboard, or open their profile and tap Connect.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.55f),
                                lineHeight = 17.sp,
                            )
                            if (!hasDuoBuddy && onInviteAccountabilityBuddy != null) {
                                OutlinedButton(
                                    onClick = { showBuddyPicker = true },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ConnectionsShellColors.gold.copy(0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ConnectionsShellColors.gold,
                                    ),
                                ) {
                                    Icon(Icons.Default.SportsScore, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Start duo after you connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    if (!hasDuoBuddy && onInviteAccountabilityBuddy != null) {
                        Button(
                            onClick = { showBuddyPicker = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConnectionsShellColors.gold.copy(alpha = 0.22f),
                                contentColor = ConnectionsShellColors.gold,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.SportsScore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Invite a friend to duo",
                                fontFamily = frauncesFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (f in friends) {
                            ConnectionsFriendCard(
                                friend = f,
                                accent = primary,
                                onClick = { onNavigateToMemberProfile(f.uid) },
                                showDuoInvite = !hasDuoBuddy && onInviteAccountabilityBuddy != null,
                                onInviteDuo = if (!hasDuoBuddy && onInviteAccountabilityBuddy != null) {
                                    { sendDuoInvite(f) }
                                } else {
                                    null
                                },
                                viewerSharesLastActive = viewerSharesLastActive,
                            )
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { target ->
        ReportUserSheet(
            reportedUid = target.uid,
            reportedUsername = target.username,
            reportedDisplayName = target.displayName,
            source = "friend_search",
            onDismiss = { reportTarget = null },
            onSubmitted = {
                onShowNotification("Report received. Our team will investigate.", false)
            },
            onBlockUser = {
                friendsViewModel.blockUser(
                    uid = target.uid,
                    username = target.username,
                    displayName = target.displayName,
                    photoUrl = target.photoUrl,
                    reason = "after_report",
                ) { ok, err ->
                    if (ok) {
                        onShowNotification("Blocked. They’ll stay hidden from your social feed.", false)
                        friendsViewModel.clearUsernameSearch()
                    } else {
                        onShowNotification(err ?: "Couldn't block", true)
                    }
                }
                reportTarget = null
            },
        )
    }

    if (showBuddyPicker) {
        DuoBuddyPickerSheet(
            friends = friends,
            accent = primary,
            onDismiss = { showBuddyPicker = false },
            onPickFriend = { sendDuoInvite(it) },
        )
    }
}

@Composable
private fun UsernameSearchResultRow(
    result: UsernameSearchResult,
    primary: Color,
    canInviteDuo: Boolean = false,
    connectPhase: VerdlyActionPhase = VerdlyActionPhase.Idle,
    onConnect: () -> Unit,
    onViewProfile: () -> Unit,
    onReport: () -> Unit,
    onInviteDuo: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewProfile),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConnectionsAvatar(
                    result.photoUrl,
                    result.username,
                    primary,
                    48.dp,
                    result.membershipTier,
                    userId = result.uid,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "@${result.username}",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val tag = result.suggestionTag
                    Text(
                        when {
                            !tag.isNullOrBlank() && result.relationship == FriendRelationship.None -> tag
                            else -> when (result.relationship) {
                                FriendRelationship.Friends -> "Already connected"
                                FriendRelationship.OutgoingPending -> "Request pending"
                                FriendRelationship.IncomingPending -> "Wants to connect"
                                FriendRelationship.Self -> "That's you"
                                FriendRelationship.None -> result.displayName.ifBlank { "Rival" }
                            }
                        },
                        fontSize = 11.sp,
                        color = if (!tag.isNullOrBlank() && result.relationship == FriendRelationship.None) {
                            ConnectionsShellColors.accentBright
                        } else {
                            Color.White.copy(alpha = 0.55f)
                        },
                        fontWeight = if (!tag.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                when (result.relationship) {
                    FriendRelationship.None -> {
                        Button(
                            onClick = onConnect,
                            enabled = connectPhase == VerdlyActionPhase.Idle || connectPhase == VerdlyActionPhase.Error,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConnectionsShellColors.actionFill,
                                contentColor = ConnectionsShellColors.onShell,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            VerdlyActionButtonContent(
                                phase = connectPhase,
                                successLabel = "Request Sent",
                                contentColor = ConnectionsShellColors.onShell,
                            ) {
                                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    FriendRelationship.Friends -> {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Friends ✓", fontSize = 12.sp, color = ConnectionsShellColors.accentBright, fontWeight = FontWeight.SemiBold)
                            if (canInviteDuo && onInviteDuo != null) {
                                Button(
                                    onClick = onInviteDuo,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ConnectionsShellColors.gold.copy(alpha = 0.85f),
                                        contentColor = Color(0xFF1A1208),
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Default.SportsScore, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Duo invite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    FriendRelationship.OutgoingPending -> {
                        Text("Pending", fontSize = 12.sp, color = Color.White.copy(0.5f))
                    }
                    else -> Unit
                }
            }
            if (result.relationship != FriendRelationship.Self) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Report user",
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onReport)
                        .padding(vertical = 4.dp),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.38f),
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestRow(
    item: IncomingFriendRequestUi,
    primary: Color,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, ConnectionsShellColors.accent.copy(alpha = 0.2f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectionsAvatar(
                item.photoUrl,
                item.username,
                primary,
                44.dp,
                item.membershipTier,
                userId = item.request.fromUid,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "@${item.username}",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Wants to connect", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConnectionsShellColors.actionFill,
                        contentColor = ConnectionsShellColors.onShell,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ConnectionsShellColors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ConnectionsShellColors.onShellMuted,
                    ),
                ) {
                    Text("Decline", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DiscoverSuggestionsBlock(
    suggestions: List<UsernameSearchResult>,
    loading: Boolean,
    primary: Color,
    connectingUid: String?,
    connectPhase: VerdlyActionPhase,
    onConnect: (uid: String, username: String) -> Unit,
    onViewProfile: (String) -> Unit,
    onReport: (UsernameSearchResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.airbnb.lottie.compose.LottieAnimation(
                composition = com.airbnb.lottie.compose.rememberLottieComposition(
                    com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(
                        com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog.LottieStarDynamic,
                    ),
                ).value,
                iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Suggested for you",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Text(
                    "New on Verdly · people you might vibe with",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        when {
            loading && suggestions.isEmpty() -> {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    VerdlyLoadingIndicator(size = 28.dp)
                }
            }
            suggestions.isEmpty() -> {
                Text(
                    "Search a name or @username to find someone.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
            else -> {
                suggestions.forEach { result ->
                    UsernameSearchResultRow(
                        result = result,
                        primary = primary,
                        connectPhase = if (connectingUid == result.uid) connectPhase else VerdlyActionPhase.Idle,
                        onConnect = { onConnect(result.uid, result.username) },
                        onViewProfile = { onViewProfile(result.uid) },
                        onReport = { onReport(result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeJoinRequestRow(
    request: ChallengeJoinRequestDoc,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, ConnectionsShellColors.gold.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "@${request.fromUsername} wants in",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White,
            )
            Text(
                request.challengeTitle,
                fontSize = 12.sp,
                color = ConnectionsShellColors.gold.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectionsShellColors.actionFill),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Let them in", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ConnectionsShellColors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ConnectionsShellColors.onShellMuted,
                    ),
                ) {
                    Text("Decline", fontSize = 12.sp)
                }
            }
        }
    }
}
