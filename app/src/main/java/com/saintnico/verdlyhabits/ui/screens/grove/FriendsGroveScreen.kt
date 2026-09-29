package com.saintnico.verdlyhabits.ui.screens.grove

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.NudgeSituation
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.engine.share.ShareCardEngine
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.VerdlyActionLottie
import com.saintnico.verdlyhabits.ui.components.VerdlyHeroIllustration
import com.saintnico.verdlyhabits.ui.components.social.rememberNudgeCooldownState
import com.saintnico.verdlyhabits.ui.components.social.NudgeSheetHost
import com.saintnico.verdlyhabits.ui.components.social.NudgeTarget
import com.saintnico.verdlyhabits.ui.screens.home.GrovePulseSheet
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember
import com.saintnico.verdlyhabits.ui.viewmodel.GroveViewModel
import com.saintnico.verdlyhabits.utils.ShareCardExporter

private val Mint = Color(0xFF52B788)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsGroveScreen(
    groveViewModel: GroveViewModel,
    friendsViewModel: FriendsViewModel,
    onBack: () -> Unit,
    onOpenMember: (String) -> Unit,
    onOpenOpenGrove: () -> Unit,
    onOpenConnect: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    val friends by friendsViewModel.friendSummaries.collectAsState()
    val me by groveViewModel.myPresence.collectAsState()
    val witnessed by groveViewModel.witnessedToday.collectAsState()
    val rivalUid by groveViewModel.rivalUid.collectAsState()
    val viewerSharesLastActive by groveViewModel.shareLastActive.collectAsState()
    val members = remember(friends, me) { groveViewModel.membersFrom(friends) }
    val pulse = remember(members) { groveViewModel.pulse(members) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var nudgeTarget by remember { mutableStateOf<NudgeTarget?>(null) }
    var detailMember by remember { mutableStateOf<GroveMember?>(null) }
    val context = LocalContext.current
    val nudgeCooldown = rememberNudgeCooldownState(members.map { it.uid })

    LaunchedEffect(Unit) {
        groveViewModel.refreshMe()
        friendsViewModel.refreshFriendPresence()
    }

    val filtered = remember(members, query, tab) {
        val q = query.trim().removePrefix("@").lowercase()
        members.filter { m ->
            (q.isBlank() || m.username.lowercase().contains(q)) &&
                when (tab) {
                    0 -> true
                    1 -> true
                    else -> true
                }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Friends Grove", fontFamily = frauncesFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(pulse.copy, fontFamily = dmSansFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val code = ReferralManager.getReferralCode(context)
                        val spec = ShareCardEngine.forGrovePulse(
                            plantedCount = pulse.plantedFriends.size,
                            friendCount = pulse.friendCount,
                            lead = members.firstOrNull { it.groveLead }?.username,
                            referralCode = code,
                        )
                        ShareCardExporter.exportAndShare(context, spec, "Share the grove")
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share grove")
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Today") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("This week") })
                    FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Roots") })
                }
                Spacer(Modifier.height(12.dp))
            }
            if (pulse.friendCount == 0) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Mint.copy(0.1f))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        VerdlyHeroIllustration(
                            drawableRes = VerdlyAssetCatalog.GroveEmptyHero,
                            size = 160.dp,
                        )
                        com.saintnico.verdlyhabits.ui.components.VerdlyHeroLottie(
                            rawRes = VerdlyAssetCatalog.GroveEmptyLottie,
                            size = 72.dp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your grove is empty",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Habits grow faster with people you trust. Invite someone you know and watch the grove fill in.",
                            fontFamily = dmSansFamily,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 320.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(onClick = onOpenConnect) { Text("Find friends") }
                    }
                }
            }
            items(filtered, key = { it.uid }) { member ->
                val metric = when (tab) {
                    1 -> "${member.weeklyPlantDays}/7 days"
                    2 -> "${member.currentStreakPublic}d rooted"
                    else -> if (member.plantedToday) "planted" else "still growing"
                }
                GrovePersonRow(
                    member = member,
                    metric = metric,
                    planted = member.plantedToday,
                    onClick = { detailMember = member },
                    onNudge = if (!member.isSelf && !nudgeCooldown.isCooling(member.uid)) {
                        {
                            nudgeTarget = NudgeTarget(
                                uid = member.uid,
                                username = member.username,
                                photoUrl = member.photoUrl,
                                surface = NudgeSurface.GROVE,
                                situation = if (member.plantedToday) NudgeSituation.GENERAL else NudgeSituation.DUO_WAITING_ON_THEM,
                            )
                        }
                    } else null,
                    onWitness = if (!member.isSelf && member.plantedToday) {
                        {
                            groveViewModel.witness(member.uid) { ok, err ->
                                onShowNotification(if (ok) "They'll feel that." else (err ?: "Couldn't witness"), !ok)
                            }
                        }
                    } else null,
                    witnessed = member.uid in witnessed,
                    showWitness = tab == 0,
                    viewerSharesLastActive = viewerSharesLastActive,
                )
                if (!member.isSelf) {
                    Row(Modifier.padding(start = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (rivalUid == member.uid) "Your rival this week" else "Pin as rival",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    groveViewModel.setRival(if (rivalUid == member.uid) null else member.uid)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = if (rivalUid == member.uid) Mint else MaterialTheme.colorScheme.onBackground.copy(0.45f),
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    "The Open Grove",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenOpenGrove)
                        .padding(vertical = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.32f),
                    fontFamily = dmSansFamily,
                )
            }
        }
    }

    val openDetail = detailMember
    if (openDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { detailMember = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            GrovePulseSheet(
                member = openDetail,
                witnessed = openDetail.uid in witnessed,
                completedToday = emptyList(),
                onWitness = {
                    groveViewModel.witness(openDetail.uid) { ok, err ->
                        onShowNotification(if (ok) "They'll feel that." else (err ?: "Couldn't witness"), !ok)
                    }
                },
                onOpenProfile = {
                    detailMember = null
                    onOpenMember(openDetail.uid)
                },
                onShareHabit = { title -> groveViewModel.sharePulse(title) },
                onClearShare = { groveViewModel.clearPulseShare() },
                viewerSharesLastActive = viewerSharesLastActive,
                onNudge = if (!openDetail.isSelf && !nudgeCooldown.isCooling(openDetail.uid)) {
                    {
                        detailMember = null
                        nudgeTarget = NudgeTarget(
                            uid = openDetail.uid,
                            username = openDetail.username,
                            photoUrl = openDetail.photoUrl,
                            surface = NudgeSurface.GROVE,
                            situation = if (openDetail.plantedToday) NudgeSituation.GENERAL else NudgeSituation.DUO_WAITING_ON_THEM,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }

    NudgeSheetHost(
        target = nudgeTarget,
        onDismiss = { nudgeTarget = null },
        onSent = {
            nudgeTarget?.uid?.let { nudgeCooldown.markSent(it) }
            onShowNotification("Nudge sent", false)
        },
        onError = { onShowNotification(it, true) },
    )
}
