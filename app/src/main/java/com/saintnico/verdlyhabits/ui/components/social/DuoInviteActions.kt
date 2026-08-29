package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary

private val Mint = Color(0xFF52B788)
private val ActionGreen = Color(0xFF40916C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoBuddyPickerSheet(
    friends: List<FriendSummary>,
    accent: Color,
    onDismiss: () -> Unit,
    onPickFriend: (FriendSummary) -> Unit,
    onFindFriends: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Start a duo streak",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Text(
                        "Pick a friend — when you both finish every habit on the same day, your streak grows.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (friends.isEmpty()) {
                Text(
                    "Add friends first by searching name or username below, or from a challenge leaderboard.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    lineHeight = 18.sp,
                )
                if (onFindFriends != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onFindFriends()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Find friends", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                for (friend in friends) {
                    OutlinedButton(
                        onClick = { onPickFriend(friend) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Mint.copy(0.35f)),
                    ) {
                        ConnectionsAvatar(friend.photoUrl, friend.username, accent, 32.dp, friend.membershipTier)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "@${friend.username}",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
