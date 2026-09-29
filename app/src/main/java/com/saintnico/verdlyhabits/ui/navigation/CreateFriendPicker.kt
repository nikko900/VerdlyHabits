package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

enum class CreatePickerMode { NUDGE, DUO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFriendPicker(
    mode: CreatePickerMode,
    friends: List<CreateNudgeFriend>,
    onPick: (CreateNudgeFriend) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                if (mode == CreatePickerMode.NUDGE) "Nudge someone" else "Start a duo",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (mode == CreatePickerMode.NUDGE) {
                    "They'll get a quiet poke."
                } else {
                    "Pick who you want to root with."
                },
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn {
                items(friends, key = { it.uid }) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(friend) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = friend.photoUrl,
                            contentDescription = friend.username,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (friend.username.isNotBlank()) "@${friend.username}" else "Friend",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}
