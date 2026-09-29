package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.remote.firestore.BlockedUserDoc
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersSheet(
    friendsViewModel: FriendsViewModel,
    onDismiss: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit = { _, _ -> },
) {
    val snapshot by friendsViewModel.blockedList.collectAsState()
    var pendingUnblock by remember { mutableStateOf<BlockedUserDoc?>(null) }
    var unblocking by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Blocked users",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", fontFamily = dmSansFamily)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "People you’ve blocked stay hidden from your friends list, search, and requests. They are not banned from Verdly. Only hidden for you.",
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(16.dp))

            when {
                !snapshot.loaded && snapshot.error == null -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    VerdlyLoadingIndicator(size = 28.dp)
                }
                snapshot.error != null -> Column(Modifier.padding(vertical = 12.dp)) {
                    Text(
                        snapshot.error.orEmpty(),
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { friendsViewModel.retryBlockedList() }) {
                        Text("Retry", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
                snapshot.users.isEmpty() -> Text(
                    "No one blocked yet.",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(vertical = 20.dp),
                )
                else -> Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    snapshot.users.forEach { user ->
                        BlockedUserRow(
                            user = user,
                            onUnblock = { pendingUnblock = user },
                        )
                    }
                }
            }
        }
    }

    val target = pendingUnblock
    if (target != null) {
        val handle = target.username.ifBlank { target.displayName }.ifBlank { "Rival" }
        AlertDialog(
            onDismissRequest = { if (!unblocking) pendingUnblock = null },
            title = { Text("Unblock @$handle?") },
            text = {
                Text("They can appear in search and requests again. You can block them from their profile anytime.")
            },
            confirmButton = {
                TextButton(
                    enabled = !unblocking,
                    onClick = {
                        unblocking = true
                        friendsViewModel.unblockUser(target.uid) { ok, err ->
                            unblocking = false
                            pendingUnblock = null
                            when {
                                ok && err != null -> onShowNotification(err, true)
                                ok -> onShowNotification(
                                    "Unblocked @$handle. They're back in Connections.",
                                    false,
                                )
                                else -> onShowNotification(err ?: "Couldn't unblock. Try again.", true)
                            }
                        }
                    },
                ) {
                    Text("Unblock")
                }
            },
            dismissButton = {
                TextButton(enabled = !unblocking, onClick = { pendingUnblock = null }) {
                    Text("Keep blocked")
                }
            },
        )
    }
}

@Composable
private fun BlockedUserRow(
    user: BlockedUserDoc,
    onUnblock: () -> Unit,
) {
    val handle = user.username.ifBlank { user.displayName }.ifBlank { "Rival" }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!user.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ) {}
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "@$handle",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (user.displayName.isNotBlank() && user.displayName != handle) {
                    Text(
                        user.displayName,
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onUnblock) {
                Text("Unblock", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
