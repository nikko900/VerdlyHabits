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
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
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
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.IncomingFriendRequestUi
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import com.saintnico.verdlyhabits.ui.viewmodel.UsernameSearchResult

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
    val searchResult by friendsViewModel.usernameSearchResult.collectAsState()
    val searchError by friendsViewModel.usernameSearchError.collectAsState()
    val searchLoading by friendsViewModel.usernameSearchLoading.collectAsState()
    var showBuddyPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<UsernameSearchResult?>(null) }

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
                    onValueChange = {
                        searchQuery = it
                        friendsViewModel.clearUsernameSearch()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("@username", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = ConnectionsShellColors.accentBright)
                    },
                    trailingIcon = {
                        if (searchLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = primary,
                            )
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
                Button(
                    onClick = { friendsViewModel.searchByUsername(searchQuery) },
                    enabled = searchQuery.trim().length >= 2 && !searchLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConnectionsShellColors.actionFill,
                        contentColor = ConnectionsShellColors.onShell,
                        disabledContainerColor = ConnectionsShellColors.disabledFill,
                        disabledContentColor = ConnectionsShellColors.disabledLabel,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Search & connect", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                searchError?.let { err ->
                    Text(err, fontSize = 12.sp, color = Color(0xFFFFB4A2))
                }
                searchResult?.let { result ->
                    UsernameSearchResultRow(
                        result = result,
                        primary = primary,
                        canInviteDuo = !hasDuoBuddy && result.relationship == FriendRelationship.Friends,
                        onConnect = {
                            friendsViewModel.sendFriendRequest(result.uid) { ok, err ->
                                if (ok) {
                                    onShowNotification("Request sent to @${result.username}", false)
                                    friendsViewModel.clearUsernameSearch()
                                    searchQuery = ""
                                } else {
                                    onShowNotification(err ?: "Could not connect", true)
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
                                    ),
                                )
                            }
                        } else {
                            null
                        },
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
                                "Search by @username above, tap someone on a challenge leaderboard, or open their profile and tap Connect.",
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
            onDismiss = { reportTarget = null },
            onSubmitted = {
                onShowNotification("Report submitted. Thank you.", false)
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
                ConnectionsAvatar(result.photoUrl, result.username, primary, 48.dp)
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
                    Text(
                        when (result.relationship) {
                            FriendRelationship.Friends -> "Already connected"
                            FriendRelationship.OutgoingPending -> "Request pending"
                            FriendRelationship.IncomingPending -> "Wants to connect"
                            FriendRelationship.Self -> "That's you"
                            FriendRelationship.None -> result.displayName.ifBlank { "Rival" }
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }
                when (result.relationship) {
                    FriendRelationship.None -> {
                        Button(
                            onClick = onConnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConnectionsShellColors.actionFill,
                                contentColor = ConnectionsShellColors.onShell,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            ConnectionsAvatar(item.photoUrl, item.username, primary, 44.dp)
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
