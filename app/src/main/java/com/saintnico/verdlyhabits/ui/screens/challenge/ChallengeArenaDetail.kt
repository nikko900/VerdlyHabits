package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ProofWindowState
import com.saintnico.verdlyhabits.ui.components.ProfileAvatar
import com.saintnico.verdlyhabits.ui.components.VerdlyCameraFlashIcon
import com.saintnico.verdlyhabits.ui.components.challenge.ChallengeInvitePreviewCard
import com.saintnico.verdlyhabits.ui.components.reactions.ProofPhotoReactionFrame
import com.saintnico.verdlyhabits.ui.components.reactions.proofReactionKey
import com.saintnico.verdlyhabits.ui.components.reactions.reactionCountsFor
import com.saintnico.verdlyhabits.ui.theme.DangerRed
import com.saintnico.verdlyhabits.ui.theme.StakeAmber
import com.saintnico.verdlyhabits.ui.theme.SuccessGreen
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.theme.jetBrainsMonoFamily

internal data class LatestProofItem(
    val userId: String,
    val date: String,
    val photoUrl: String,
    val proofKey: String,
)

internal enum class TodayPresence { Planted, Late, Waiting, Missed }

internal fun Challenge.latestProofItems(limit: Int = 12): List<LatestProofItem> =
    proofPhotos.flatMap { (uid, byDate) ->
        byDate.map { (date, url) ->
            LatestProofItem(
                userId = uid,
                date = date,
                photoUrl = url,
                proofKey = proofReactionKey(uid, date),
            )
        }
    }.sortedByDescending { it.date }.take(limit)

internal fun Challenge.proofDateForUrl(userId: String, photoUrl: String, fallbackDate: String): String =
    proofPhotos[userId]?.entries?.firstOrNull { it.value == photoUrl }?.key ?: fallbackDate

internal fun Challenge.todayPresence(userId: String, todayStr: String): TodayPresence {
    val posted = hasCompletedToday(userId, todayStr)
    val late = posted && (isLateProof(userId, todayStr) || isAfterDeadlineProof(userId, todayStr))
    val missed = !posted && currentProofWindowState(userId, todayStr) == ProofWindowState.Locked
    return when {
        late -> TodayPresence.Late
        posted -> TodayPresence.Planted
        missed -> TodayPresence.Missed
        else -> TodayPresence.Waiting
    }
}

@Composable
internal fun ArenaSectionLabel(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(
                trailing,
                fontFamily = dmSansFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
internal fun ArenaHero(
    challenge: Challenge,
    coinChip: @Composable () -> Unit,
    onCopyId: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val creator = challenge.memberNames[challenge.creatorId] ?: "Unknown"
    val faces = challenge.members.take(4)
    val extra = (challenge.members.size - faces.size).coerceAtLeast(0)
    var menuOpen by remember { mutableStateOf(false) }
    val remaining = challenge.daysRemaining().coerceAtLeast(0)
    val duration = challenge.durationDays().coerceAtLeast(1)
    val progressed = ((duration - remaining).toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    challenge.habitName,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Started by @$creator",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            coinChip()
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy challenge ID") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = {
                            menuOpen = false
                            onCopyId()
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                faces.forEachIndexed { index, uid ->
                    ProfileAvatar(
                        photoUri = challenge.memberPhotos[uid],
                        userId = uid,
                        size = 32.dp,
                        modifier = Modifier
                            .padding(start = 12.dp * index)
                            .zIndex((faces.size - index).toFloat())
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    )
                }
                if (extra > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp * faces.size)
                            .size(32.dp)
                            .zIndex(0f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+$extra",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = dmSansFamily,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            ArenaMetaPill("$remaining d left")
            Spacer(Modifier.width(8.dp))
            ArenaMetaPill("${challenge.members.size} in")
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressed.coerceAtLeast(0.04f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun ArenaMetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LatestProofsRail(
    challenge: Challenge,
    currentUserId: String,
    items: List<LatestProofItem>,
    onReact: (proofKey: String, emoji: String) -> Unit,
    onExpand: (LatestProofItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val listState = rememberLazyListState()
    Column(modifier.fillMaxWidth()) {
        ArenaSectionLabel("LATEST PROOFS", "${items.size} moments")
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth * 0.82f
            LazyRow(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 28.dp),
            ) {
                items(items, key = { it.proofKey }) { item ->
                    ProofStoryCard(
                        challenge = challenge,
                        item = item,
                        currentUserId = currentUserId,
                        onReact = onReact,
                        onExpand = { onExpand(item) },
                        modifier = Modifier.width(cardWidth),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProofStoryCard(
    challenge: Challenge,
    item: LatestProofItem,
    currentUserId: String,
    onReact: (proofKey: String, emoji: String) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    showRemove: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val name = challenge.memberNames[item.userId] ?: "Rival"
    val late = challenge.isLateProof(item.userId, item.date) ||
        challenge.isAfterDeadlineProof(item.userId, item.date)
    val counts = reactionCountsFor(challenge, item.proofKey)
    val canReact = currentUserId.isNotBlank() && currentUserId != item.userId
    val emptyCheer = counts.isEmpty() && canReact

    Column(modifier) {
        ProofPhotoReactionFrame(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(26.dp)),
            reactionCounts = counts,
            reactionScale = 0.16f,
            enablePicker = canReact,
            onReact = if (canReact) { key -> onReact(item.proofKey, key) } else null,
            onExpand = onExpand,
        ) {
            AsyncImage(
                model = item.photoUrl,
                contentDescription = "Proof from $name",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f),
                        )
                    )
            )
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(
                    photoUri = challenge.memberPhotos[item.userId],
                    userId = item.userId,
                    size = 34.dp,
                    modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = Color.White,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (item.userId == currentUserId) "Your proof" else "Tap to sticker · double-tap to open",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = dmSansFamily,
                    )
                }
                if (late) {
                    Surface(shape = RoundedCornerShape(50), color = StakeAmber.copy(alpha = 0.92f)) {
                        Text(
                            "LATE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color.Black,
                        )
                    }
                }
            }
            if (emptyCheer) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.45f),
                ) {
                    Text(
                        "Be first to cheer",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = dmSansFamily,
                    )
                }
            }
        }
        if (showRemove && onRemove != null) {
            androidx.compose.material3.TextButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Remove today’s check-in",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
internal fun TodayFaceStrip(
    challenge: Challenge,
    todayStr: String,
    onMemberProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        val planted = challenge.members.count { challenge.todayPresence(it, todayStr) == TodayPresence.Planted || challenge.todayPresence(it, todayStr) == TodayPresence.Late }
        ArenaSectionLabel("TODAY", "$planted / ${challenge.members.size} showed up")
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            challenge.members.forEach { uid ->
                val presence = challenge.todayPresence(uid, todayStr)
                val ring = when (presence) {
                    TodayPresence.Planted -> SuccessGreen
                    TodayPresence.Late -> StakeAmber
                    TodayPresence.Missed -> DangerRed
                    TodayPresence.Waiting -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onMemberProfile(uid) },
                ) {
                    Box {
                        ProfileAvatar(
                            photoUri = challenge.memberPhotos[uid],
                            userId = uid,
                            size = 56.dp,
                            modifier = Modifier.border(3.dp, ring, CircleShape),
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(14.dp)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clip(CircleShape)
                                .background(ring),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        challenge.memberNames[uid] ?: "Rival",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        when (presence) {
                            TodayPresence.Planted -> "Planted"
                            TodayPresence.Late -> "Late"
                            TodayPresence.Waiting -> "Waiting"
                            TodayPresence.Missed -> "Missed"
                        },
                        fontFamily = dmSansFamily,
                        fontSize = 11.sp,
                        color = ring,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PostProofMoment(
    challenge: Challenge,
    currentUserId: String,
    todayStr: String,
    windowState: ProofWindowState,
    hasCompletedToday: Boolean,
    onPostProof: () -> Unit,
    onExpandOwn: (LatestProofItem) -> Unit,
    onReact: (String, String) -> Unit,
    onRemove: () -> Unit,
    extraBelow: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ownUrl = challenge.proofPhotos[currentUserId]?.get(todayStr)
    val reduced = windowState == ProofWindowState.OpenReducedXP

    Column(modifier.fillMaxWidth()) {
        when {
            hasCompletedToday && !ownUrl.isNullOrBlank() -> {
                ArenaSectionLabel("YOUR DAY")
                Spacer(Modifier.height(12.dp))
                ProofStoryCard(
                    challenge = challenge,
                    item = LatestProofItem(
                        userId = currentUserId,
                        date = todayStr,
                        photoUrl = ownUrl,
                        proofKey = proofReactionKey(currentUserId, todayStr),
                    ),
                    currentUserId = currentUserId,
                    onReact = onReact,
                    onExpand = {
                        onExpandOwn(
                            LatestProofItem(
                                currentUserId, todayStr, ownUrl, proofReactionKey(currentUserId, todayStr)
                            )
                        )
                    },
                    showRemove = challenge.isActive,
                    onRemove = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            windowState == ProofWindowState.OpenFullXP || windowState == ProofWindowState.OpenReducedXP -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (reduced) StakeAmber.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = BorderStroke(
                        1.dp,
                        if (reduced) StakeAmber.copy(alpha = 0.28f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    ),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            if (reduced) "After cutoff" else "Prove it",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (reduced) {
                                "Full XP window has passed · ${challenge.deadlineLabel()}"
                            } else {
                                challenge.deadlineLabel()
                            },
                            fontFamily = dmSansFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onPostProof,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reduced) StakeAmber else MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            VerdlyCameraFlashIcon(size = 22.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (reduced) "Post proof  ·  +8 XP" else "Post today’s proof  ·  +10 XP",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.White,
                                fontFamily = dmSansFamily,
                            )
                        }
                    }
                }
                extraBelow()
            }
            windowState == ProofWindowState.Locked -> {
                Text(
                    "Daily proof opens after 3:00 AM.",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                )
                extraBelow()
            }
            else -> extraBelow()
        }
    }
}

@Composable
internal fun GroupOathPlaque(
    stake: String,
    modifier: Modifier = Modifier,
) {
    if (stake.isBlank()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = StakeAmber.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, StakeAmber.copy(alpha = 0.22f)),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                "THE OATH",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                color = StakeAmber,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stake,
                fontFamily = frauncesFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
internal fun PeopleInviteBlock(
    challenge: Challenge,
    hasConnections: Boolean,
    onInvite: () -> Unit,
    onAddConnections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ArenaSectionLabel("PEOPLE", "Send this like a poster")
        Spacer(Modifier.height(12.dp))
        ChallengeInvitePreviewCard(
            challengeName = challenge.habitName,
            inviteCode = challenge.inviteCode,
            stake = challenge.stake,
            daysRemaining = challenge.daysRemaining(),
            memberCount = challenge.members.size,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onInvite,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Invite", fontWeight = FontWeight.SemiBold)
            }
            if (hasConnections && challenge.isActive) {
                OutlinedButton(
                    onClick = onAddConnections,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun LiveFeedProofFrame(
    photoUrl: String,
    late: Boolean,
    reactionCounts: Map<String, Int>,
    canReact: Boolean,
    onReact: (String) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProofPhotoReactionFrame(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (late) Modifier.border(1.5.dp, StakeAmber, RoundedCornerShape(18.dp))
                else Modifier
            ),
        reactionCounts = reactionCounts,
        enablePicker = canReact,
        onReact = if (canReact) onReact else null,
        onExpand = onExpand,
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Proof",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f))
                    )
                )
        )
    }
}

@Composable
internal fun GapToLeaderLine(challenge: Challenge, currentUserId: String) {
    if (!challenge.isActive || currentUserId.isBlank()) return
    val gap = challenge.gapToLeader(currentUserId) ?: return
    if (gap <= 0 || challenge.challengeMode == com.saintnico.verdlyhabits.data.model.ChallengeMode.STREAK) return
    Text(
        "$gap ${challenge.scoreLabel()} behind #1",
        fontFamily = jetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}
