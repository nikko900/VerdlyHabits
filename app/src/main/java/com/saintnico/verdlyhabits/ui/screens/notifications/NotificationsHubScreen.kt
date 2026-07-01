package com.saintnico.verdlyhabits.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.model.NotificationCategory
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.NotificationsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsHubScreen(
    notificationsViewModel: NotificationsViewModel,
    friendsViewModel: FriendsViewModel,
    accountabilityViewModel: com.saintnico.verdlyhabits.ui.viewmodel.AccountabilityViewModel,
    onBack: () -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onOpenChallenge: (String) -> Unit,
    onOpenRoute: (String) -> Unit = {},
    onShowToast: (String, Boolean) -> Unit,
) {
    val state by notificationsViewModel.state.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Activity",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                        )
                        Text(
                            "Requests & updates",
                            style = MaterialTheme.typography.bodySmall,
                            color = onBg.copy(alpha = 0.55f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        Text(
                            "Mark all read",
                            modifier = Modifier
                                .clickable { notificationsViewModel.markAllRead() }
                                .padding(end = 16.dp),
                            color = primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
            )
        },
        containerColor = bg,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CategoryChipRow(
                selected = state.selectedCategory,
                onSelect = notificationsViewModel::selectCategory,
                primary = primary,
                onBg = onBg,
            )
            Spacer(Modifier.height(8.dp))
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primary)
                    }
                }
                state.filtered.isEmpty() -> EmptyNotificationsState(onBg = onBg, primary = primary)
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.filtered, key = { it.id }) { item ->
                            NotificationCard(
                                item = item,
                                primary = primary,
                                onBg = onBg,
                                onOpenProfile = {
                                    notificationsViewModel.markRead(item.id)
                                    if (item.actorUid.isNotBlank()) onNavigateToMemberProfile(item.actorUid)
                                },
                                onAcceptFriend = {
                                    friendsViewModel.acceptRequest(item.referenceId) { ok, err ->
                                        if (ok) {
                                            notificationsViewModel.resolveAction(item.id, "accepted")
                                            onShowToast("You're now connected", false)
                                        } else {
                                            onShowToast(err ?: "Could not accept", true)
                                        }
                                    }
                                },
                                onDeclineFriend = {
                                    friendsViewModel.declineRequest(item.referenceId) { _, _ ->
                                        notificationsViewModel.resolveAction(item.id, "declined")
                                    }
                                },
                                onAcceptDuo = {
                                    accountabilityViewModel.acceptInvite(item.referenceId)
                                    notificationsViewModel.resolveAction(item.id, "accepted")
                                    onShowToast("Duo streak started!", false)
                                },
                                onDeclineDuo = {
                                    accountabilityViewModel.declineInvite(item.referenceId)
                                    notificationsViewModel.resolveAction(item.id, "declined")
                                },
                                onApproveArena = {
                                    val parts = item.referenceId.split("_")
                                    val challengeId = item.challengeId ?: parts.firstOrNull().orEmpty()
                                    friendsViewModel.approveChallengeJoinRequest(
                                        requestId = item.referenceId,
                                        challengeId = challengeId,
                                        requesterUid = item.actorUid,
                                    ) { ok, err ->
                                        if (ok) {
                                            notificationsViewModel.resolveAction(item.id, "accepted")
                                            onShowToast("Rival added to your arena", false)
                                        } else {
                                            onShowToast(err ?: "Could not approve", true)
                                        }
                                    }
                                },
                                onDeclineArena = {
                                    friendsViewModel.declineChallengeJoinRequest(item.referenceId) { ok, _ ->
                                        if (ok) notificationsViewModel.resolveAction(item.id, "declined")
                                    }
                                },
                                onOpenChallenge = {
                                    notificationsViewModel.markRead(item.id)
                                    item.challengeId?.let(onOpenChallenge)
                                },
                                onOpenRoute = { route ->
                                    notificationsViewModel.markRead(item.id)
                                    onOpenRoute(route)
                                },
                                onMarkRead = { notificationsViewModel.markRead(item.id) },
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    selected: NotificationCategory,
    onSelect: (NotificationCategory) -> Unit,
    primary: Color,
    onBg: Color,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(NotificationCategory.entries) { cat ->
            val isSelected = cat == selected
            Surface(
                onClick = { onSelect(cat) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) primary.copy(alpha = 0.14f) else onBg.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) primary.copy(alpha = 0.5f) else onBg.copy(alpha = 0.08f),
                ),
            ) {
                Text(
                    cat.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isSelected) primary else onBg.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: InboxNotification,
    primary: Color,
    onBg: Color,
    onOpenProfile: () -> Unit,
    onAcceptFriend: () -> Unit,
    onDeclineFriend: () -> Unit,
    onAcceptDuo: () -> Unit,
    onDeclineDuo: () -> Unit,
    onApproveArena: () -> Unit,
    onDeclineArena: () -> Unit,
    onOpenChallenge: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onMarkRead: () -> Unit,
) {
    val showActions = item.actionState == "pending" && !item.read || item.actionState == "pending"
    val icon = iconForType(item.type)
    val accent = accentForType(item.type)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (item.read) onBg.copy(alpha = 0.03f) else primary.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.read) onBg.copy(alpha = 0.06f) else primary.copy(alpha = 0.18f),
        ),
        onClick = {
            when (item.type) {
                InboxNotificationType.CHALLENGE_UPDATE -> onOpenChallenge()
                InboxNotificationType.SYSTEM -> {
                    val route = item.route?.takeIf { it.isNotBlank() }
                    if (route != null) onOpenRoute(route) else onMarkRead()
                }
                else -> onOpenProfile()
            }
        },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!item.actorPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.actorPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = onBg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!item.read) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE85D4C)),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.body,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = onBg.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatRelativeTime(item.createdAtMillis),
                        fontSize = 11.sp,
                        color = onBg.copy(alpha = 0.4f),
                    )
                }
            }
            AnimatedVisibility(
                visible = showActions && item.type != InboxNotificationType.CHALLENGE_UPDATE &&
                    item.type != InboxNotificationType.SYSTEM,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = onBg.copy(alpha = 0.06f))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (item.type) {
                            InboxNotificationType.FRIEND_REQUEST -> {
                                Button(
                                    onClick = onAcceptFriend,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onDeclineFriend,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Decline", fontSize = 12.sp)
                                }
                            }
                            InboxNotificationType.DUO_INVITE -> {
                                Button(
                                    onClick = onAcceptDuo,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C)),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Start duo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onDeclineDuo,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Not now", fontSize = 12.sp)
                                }
                            }
                            InboxNotificationType.ARENA_INVITE -> {
                                Button(
                                    onClick = onApproveArena,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C)),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Let them in", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onDeclineArena,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Decline", fontSize = 12.sp)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState(onBg: Color, primary: Color) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.2f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.NotificationsNone,
                contentDescription = null,
                tint = primary.copy(alpha = 0.7f),
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "All caught up",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = onBg,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Friend requests, duo invites, arena updates, and live pulse activity show up here.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = onBg.copy(alpha = 0.55f),
            lineHeight = 20.sp,
            fontSize = 14.sp,
        )
    }
}

private fun iconForType(type: InboxNotificationType): ImageVector = when (type) {
    InboxNotificationType.FRIEND_REQUEST -> Icons.Default.PersonAdd
    InboxNotificationType.DUO_INVITE -> Icons.Default.SportsScore
    InboxNotificationType.ARENA_INVITE -> Icons.Default.Groups
    InboxNotificationType.CHALLENGE_UPDATE -> Icons.Rounded.Celebration
    InboxNotificationType.SYSTEM -> Icons.Rounded.NotificationsNone
}

private fun accentForType(type: InboxNotificationType): Color = when (type) {
    InboxNotificationType.FRIEND_REQUEST -> Color(0xFF74C69D)
    InboxNotificationType.DUO_INVITE -> Color(0xFF52B788)
    InboxNotificationType.ARENA_INVITE -> Color(0xFF90CAF9)
    InboxNotificationType.CHALLENGE_UPDATE -> Color(0xFFFFB74D)
    InboxNotificationType.SYSTEM -> Color(0xFF95D5B2)
}

private fun formatRelativeTime(millis: Long): String {
    if (millis <= 0L) return "Just now"
    val instant = Instant.ofEpochMilli(millis)
    val now = Instant.now()
    val minutes = ChronoUnit.MINUTES.between(instant, now)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        ChronoUnit.HOURS.between(instant, now) < 24 -> "${ChronoUnit.HOURS.between(instant, now)}h ago"
        ChronoUnit.DAYS.between(instant, now) < 7 -> "${ChronoUnit.DAYS.between(instant, now)}d ago"
        else -> instant.atZone(ZoneId.systemDefault()).toLocalDate().let { date ->
            if (date.year == LocalDate.now().year) {
                date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
            } else {
                date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }
    }
}
