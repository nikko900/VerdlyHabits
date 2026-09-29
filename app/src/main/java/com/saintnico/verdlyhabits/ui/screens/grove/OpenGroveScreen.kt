package com.saintnico.verdlyhabits.ui.screens.grove

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember
import com.saintnico.verdlyhabits.ui.viewmodel.GroveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenGroveScreen(
    groveViewModel: GroveViewModel,
    isPremium: Boolean,
    onBack: () -> Unit,
    onOpenMember: (String) -> Unit,
    onRequestPremium: () -> Unit,
) {
    val look by groveViewModel.lookAtOpenGrove.collectAsState()
    val me by groveViewModel.myPresence.collectAsState()
    val board by groveViewModel.openBoard.collectAsState()
    val history by groveViewModel.openGroveHistory.collectAsState()
    val stand = me?.standInOpenGrove == true

    LaunchedEffect(look, isPremium) {
        if (look) groveViewModel.refreshOpenGrove()
        if (isPremium) groveViewModel.refreshOpenGroveHistory()
        groveViewModel.refreshMe()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("The Open Grove", fontFamily = frauncesFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    "Habit names are never shown. Only days planted, public streak, and what you wear.",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Look at the Open Grove", fontWeight = FontWeight.SemiBold)
                        Text("See strangers who chose to stand", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                    }
                    Switch(checked = look, onCheckedChange = { groveViewModel.setLook(it) })
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Stand in the Open Grove", fontWeight = FontWeight.SemiBold)
                        Text("Appear on this board. Off anytime.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                    }
                    Switch(checked = stand, onCheckedChange = { groveViewModel.setStand(it) })
                }
                Spacer(Modifier.height(12.dp))
                if (stand && (me?.openGroveWeekRank ?: 0) > 0) {
                    val rank = me?.openGroveWeekRank ?: 0
                    val pct = me?.openGrovePercentile ?: 0
                    Text(
                        if (pct in 1..100) "You're in the top $pct% of the Open Grove this week."
                        else "You're #$rank this week.",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (!look) {
                item {
                    Text(
                        "Turn Look on to see who is standing.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                        fontSize = 13.sp,
                    )
                }
            } else if (board.top.isEmpty()) {
                item {
                    Text(
                        "The Open Grove is quiet this week. Stand, then plant.",
                        color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                        fontSize = 13.sp,
                    )
                }
            } else {
                items(board.top, key = { it.uid }) { entry ->
                    GrovePersonRow(
                        member = GroveMember(
                            uid = entry.uid,
                            username = entry.username,
                            photoUrl = entry.photoUrl,
                            membershipTier = MembershipTier.fromFirestore(entry.membershipTier),
                            titleGlowId = entry.titleGlowId,
                            profileFrameId = entry.profileFrameId,
                            grovePlantId = entry.grovePlantId,
                            pinnedTrophyLabel = entry.pinnedTrophyLabel?.takeIf {
                                entry.pinnedTrophyUntil > System.currentTimeMillis()
                            },
                            plantedToday = entry.weeklyPlantDays > 0,
                            weeklyPlantDays = entry.weeklyPlantDays,
                            currentStreakPublic = entry.currentStreakPublic,
                            groveLead = false,
                            witnessCount = 0,
                            isSelf = false,
                        ),
                        metric = "#${entry.rank} · ${entry.weeklyPlantDays}/7",
                        planted = entry.weeklyPlantDays > 0,
                        onClick = { onOpenMember(entry.uid) },
                        viewerSharesLastActive = false,
                    )
                }
            }
            if (isPremium) {
                if (history.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        Text("Your last weeks", fontWeight = FontWeight.SemiBold, fontFamily = frauncesFamily)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(history, key = { it.weekKey }) { week ->
                        Text(
                            "${week.weekKey}  ·  #${week.rank}  ·  ${week.weeklyPlantDays}/7",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Premium keeps the last 8 weeks and tells you who passed you out here.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                        modifier = Modifier.clickable(onClick = onRequestPremium),
                    )
                }
            }
        }
    }
}
