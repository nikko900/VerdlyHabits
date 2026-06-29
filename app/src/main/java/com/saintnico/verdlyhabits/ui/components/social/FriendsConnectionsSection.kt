package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeJoinRequestDoc
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRelationship
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.IncomingFriendRequestUi
import com.saintnico.verdlyhabits.ui.viewmodel.UsernameSearchResult
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import com.saintnico.verdlyhabits.ui.components.social.ReportUserSheet

import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import com.saintnico.verdlyhabits.ui.components.referral.ReferralPromoBanner

/** High-contrast tokens for the always-dark connections shell (readable in light + dark app themes). */
private object ConnectionsShellColors {
    val accent = Color(0xFF74C69D)
    val accentBright = Color(0xFF95D5B2)
    val actionFill = Color(0xFF40916C)
    val onShell = Color.White
    val onShellMuted = Color(0xB3FFFFFF)
    val border = Color(0x45FFFFFF)
    val disabledFill = Color(0x24FFFFFF)
    val disabledLabel = Color(0x66FFFFFF)
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

    val shellGradient = Brush.linearGradient(
        listOf(
            Color(0xFF0F1A14),
            Color(0xFF132A1F),
            Color(0xFF0A120E),
        ),
    )

    Column(modifier) {
        Text(
            "CONNECTIONS",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            letterSpacing = 0.5.sp,
            color = primary,
        )
        Text(
            "Friends, requests, and arena invites",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = onBg.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(12.dp))

        if (referralState.showBanner) {
            ReferralPromoBanner(
                qualifiedCount = referralState.qualifiedCount,
                friendsRequired = referralState.friendsRequired,
                rewardDays = referralState.rewardDays,
                onOpen = onOpenReferral,
                onDismiss = {}, // We don't dismiss it here, keep it compact
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, primary.copy(alpha = 0.22f), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(shellGradient)
                    .padding(16.dp),
            ) {
                Column {
                    SectionLabel("Find by username", Icons.Default.Search, ConnectionsShellColors.accentBright)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            friendsViewModel.clearUsernameSearch()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("@rivalname", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
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
                            onSearch = {
                                friendsViewModel.searchByUsername(searchQuery)
                            },
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ConnectionsShellColors.onShell,
                            unfocusedTextColor = ConnectionsShellColors.onShell.copy(alpha = 0.92f),
                            focusedBorderColor = ConnectionsShellColors.accent,
                            unfocusedBorderColor = ConnectionsShellColors.border,
                            cursorColor = ConnectionsShellColors.accentBright,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { friendsViewModel.searchByUsername(searchQuery) },
                        enabled = searchQuery.trim().length >= 2 && !searchLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConnectionsShellColors.actionFill,
                            contentColor = ConnectionsShellColors.onShell,
                            disabledContainerColor = ConnectionsShellColors.disabledFill,
                            disabledContentColor = ConnectionsShellColors.disabledLabel,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Search", fontWeight = FontWeight.Bold)
                    }
                    searchError?.let { err ->
                        Spacer(Modifier.height(6.dp))
                        Text(err, fontSize = 12.sp, color = Color(0xFFFFB4A2))
                    }
                    searchResult?.let { result ->
                        Spacer(Modifier.height(10.dp))
                        UsernameSearchResultRow(
                            result = result,
                            primary = primary,
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
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))

                    if (joinRequests.isNotEmpty()) {
                        SectionLabel("Arena invites", Icons.Default.SportsScore, primary)
                        Spacer(Modifier.height(8.dp))
                        joinRequests.forEach { req ->
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
                            Spacer(Modifier.height(8.dp))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(Modifier.height(12.dp))
                    }

                    if (incoming.isNotEmpty()) {
                        SectionLabel("Friend requests", Icons.Default.Person, primary)
                        Spacer(Modifier.height(8.dp))
                        incoming.forEach { item ->
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
                            Spacer(Modifier.height(8.dp))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(Modifier.height(12.dp))
                    }

                    SectionLabel("Your circle", Icons.Default.Groups, primary)
                    Spacer(Modifier.height(10.dp))
                    if (friends.isEmpty()) {
                        Text(
                            "Search above by @username, or connect from a challenge leaderboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.55f),
                            lineHeight = 18.sp,
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(friends, key = { it.uid }) { f ->
                                FriendAvatarChip(
                                    friend = f,
                                    primary = primary,
                                    onClick = { onNavigateToMemberProfile(f.uid) },
                                )
                            }
                        }
                        if (!hasDuoBuddy && onInviteAccountabilityBuddy != null && friends.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showBuddyPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ConnectionsShellColors.accent),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ConnectionsShellColors.accentBright,
                                    containerColor = ConnectionsShellColors.actionFill.copy(alpha = 0.22f),
                                ),
                            ) {
                                Icon(
                                    Icons.Default.SportsScore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = ConnectionsShellColors.accentBright,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Start duo streak with a friend",
                                    fontFamily = frauncesFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = ConnectionsShellColors.onShell,
                                )
                            }
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBuddyPicker = false },
            title = {
                Text("Pick your accountability buddy", fontFamily = frauncesFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    friends.forEach { f ->
                        OutlinedButton(
                            onClick = {
                                onInviteAccountabilityBuddy?.invoke(f.uid, f.username, f.photoUrl) { ok, err ->
                                    if (ok) {
                                        onShowNotification("Duo streak invite sent to @${f.username}", false)
                                        showBuddyPicker = false
                                    } else {
                                        onShowNotification(err ?: "Couldn't send duo invite", true)
                                    }
                                } ?: run {
                                    showBuddyPicker = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("@${f.username}", fontFamily = frauncesFamily)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBuddyPicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun UsernameSearchResultRow(
    result: UsernameSearchResult,
    primary: Color,
    onConnect: () -> Unit,
    onViewProfile: () -> Unit,
    onReport: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewProfile),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        AvatarCircle(result.photoUrl, result.username, primary, 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "@${result.username}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when (result.relationship) {
                    FriendRelationship.Friends -> "Already connected"
                    FriendRelationship.OutgoingPending -> "Request pending"
                    FriendRelationship.IncomingPending -> "Wants to connect — check requests"
                    FriendRelationship.Self -> "You"
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
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Text("Connect", fontSize = 12.sp)
                }
            }
            FriendRelationship.Friends -> {
                Text("Friends", fontSize = 12.sp, color = Color(0xFF95D5B2), fontWeight = FontWeight.SemiBold)
            }
            FriendRelationship.OutgoingPending -> {
                Text("Pending", fontSize = 12.sp, color = Color.White.copy(0.5f))
            }
            else -> Unit
        }
        }
        if (result.relationship != FriendRelationship.Self) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Report user",
                    modifier = Modifier
                        .clickable(onClick = onReport)
                        .padding(vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.38f),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.88f),
        )
    }
}

@Composable
private fun IncomingRequestRow(
    item: IncomingFriendRequestUi,
    primary: Color,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(item.photoUrl, item.username, primary, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "@${item.username}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Wants to connect", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConnectionsShellColors.actionFill,
                    contentColor = ConnectionsShellColors.onShell,
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text("Accept", fontSize = 12.sp)
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

@Composable
private fun ChallengeJoinRequestRow(
    request: ChallengeJoinRequestDoc,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
    ) {
        Text(
            "@${request.fromUsername} wants in",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White,
        )
        Text(
            request.challengeTitle,
            fontSize = 12.sp,
            color = Color(0xFF95D5B2),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C)),
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

@Composable
private fun FriendAvatarChip(
    friend: FriendSummary,
    primary: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
    ) {
        AvatarCircle(friend.photoUrl, friend.username, primary, 52.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            "@${friend.username}".take(14),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AvatarCircle(photoUrl: String?, label: String, primary: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(primary.copy(alpha = 0.15f))
            .border(1.dp, primary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                label.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = primary,
                fontSize = (size.value * 0.32f).sp,
            )
        }
    }
}

