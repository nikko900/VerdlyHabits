package com.saintnico.verdlyhabits.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.screens.grove.GroveAvatarStack
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GrovePulse

private val Mint = Color(0xFF52B788)

@Composable
fun HomeGrovePulse(
    pulse: GrovePulse,
    onOpenGrove: () -> Unit,
    onOpenConnect: () -> Unit,
    viewerSharesLastActive: Boolean = true,
) {
    val emptyFriends = pulse.friendCount == 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Mint.copy(alpha = 0.10f))
            .clickable(onClick = if (emptyFriends) onOpenConnect else onOpenGrove)
            .padding(14.dp),
    ) {
        Text(
            "PLANTED TODAY",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
            color = Mint,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pulse.plantedFriends.isNotEmpty()) {
                GroveAvatarStack(pulse.plantedFriends, viewerSharesLastActive = viewerSharesLastActive)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    pulse.copy,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                pulse.rival?.let { rival ->
                    Text(
                        if (rival.plantedToday) "@${rival.username} already planted"
                        else "@${rival.username} is still growing",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Mint)
        }
    }
}

@Composable
fun HomeGroveStrip(
    groveViewModel: com.saintnico.verdlyhabits.ui.viewmodel.GroveViewModel,
    friendsViewModel: com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel,
    onOpenGrove: () -> Unit,
    onOpenConnect: () -> Unit,
    onShareFirst: () -> Unit,
    onEarnedLead: () -> Unit = {},
) {
    val friends by friendsViewModel.friendSummaries.collectAsState()
    val me by groveViewModel.myPresence.collectAsState()
    val first by groveViewModel.firstInMoment.collectAsState()
    val viewerSharesLastActive by groveViewModel.shareLastActive.collectAsState()
    val members = remember(friends, me) { groveViewModel.membersFrom(friends) }
    val pulse = remember(members) { groveViewModel.pulse(members) }

    androidx.compose.runtime.LaunchedEffect(Unit) { groveViewModel.refreshMe() }
    androidx.compose.runtime.LaunchedEffect(members) {
        if (members.any { it.isSelf && it.groveLead }) onEarnedLead()
    }

    Column {
        HomeGrovePulse(
            pulse = pulse,
            onOpenGrove = onOpenGrove,
            onOpenConnect = onOpenConnect,
            viewerSharesLastActive = viewerSharesLastActive,
        )
        if (first && pulse.firstInGrove) {
            Spacer(Modifier.height(8.dp))
            Text(
                "You opened the grove today.",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onShareFirst()
                        groveViewModel.consumeFirstInMoment()
                    }
                    .padding(10.dp),
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Mint,
            )
        }
    }
}
