package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.ui.theme.BronzeColor
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.theme.SilverColor
import com.saintnico.verdlyhabits.ui.theme.StakeAmber
import com.saintnico.verdlyhabits.ui.theme.SuccessGreen

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> GoldColor
    2 -> SilverColor
    else -> BronzeColor
}

/**
 * Premium top-3 podium. [entries] must be the leaderboard's top three in rank order
 * (rank 1 first). Renders 2nd · 1st · 3rd with the champion elevated and crowned.
 */
@Composable
fun ChallengePodium(
    entries: List<Pair<String, Int>>,
    challenge: Challenge,
    currentUserId: String,
    todayStr: String,
    onProfileClick: (String) -> Unit,
    onProofClick: (userId: String, photoUrl: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val first = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third = entries.getOrNull(2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        GoldColor.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                    )
                )
            )
            .padding(top = 18.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            second?.let {
                PodiumPillar(
                    rank = 2,
                    userId = it.first,
                    score = it.second,
                    challenge = challenge,
                    currentUserId = currentUserId,
                    todayStr = todayStr,
                    pedestalHeight = 56.dp,
                    avatarSize = 58.dp,
                    onProfileClick = onProfileClick,
                    onProofClick = onProofClick,
                    modifier = Modifier.weight(1f),
                )
            }
            first?.let {
                PodiumPillar(
                    rank = 1,
                    userId = it.first,
                    score = it.second,
                    challenge = challenge,
                    currentUserId = currentUserId,
                    todayStr = todayStr,
                    pedestalHeight = 84.dp,
                    avatarSize = 74.dp,
                    onProfileClick = onProfileClick,
                    onProofClick = onProofClick,
                    modifier = Modifier.weight(1f),
                )
            }
            third?.let {
                PodiumPillar(
                    rank = 3,
                    userId = it.first,
                    score = it.second,
                    challenge = challenge,
                    currentUserId = currentUserId,
                    todayStr = todayStr,
                    pedestalHeight = 40.dp,
                    avatarSize = 54.dp,
                    onProfileClick = onProfileClick,
                    onProofClick = onProofClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PodiumPillar(
    rank: Int,
    userId: String,
    score: Int,
    challenge: Challenge,
    currentUserId: String,
    todayStr: String,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    avatarSize: androidx.compose.ui.unit.Dp,
    onProfileClick: (String) -> Unit,
    onProofClick: (userId: String, photoUrl: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = medalColor(rank)
    val name = challenge.memberNames[userId] ?: "Unknown"
    val photo = challenge.memberPhotos[userId]
    val isMe = userId == currentUserId
    val posted = challenge.hasCompletedToday(userId, todayStr)
    val late = challenge.isLateProof(userId, todayStr)
    val proofPhoto = challenge.proofPhotos[userId]?.get(todayStr)
    val rankChange = challenge.rankChanges[userId] ?: 0

    // Champion ring shimmer
    val shimmer = rememberInfiniteTransition(label = "podiumShimmer")
    val ringAlpha by shimmer.animateFloat(
        initialValue = if (rank == 1) 0.45f else 0.3f,
        targetValue = if (rank == 1) 1f else 0.3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "ringAlpha",
    )
    val popScale by animateFloatAsState(targetValue = 1f, label = "pop")

    Column(
        modifier = modifier.scale(popScale),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (rank == 1) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = "Champion",
                tint = GoldColor.copy(alpha = ringAlpha),
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(2.dp))
        } else {
            Spacer(Modifier.height(28.dp))
        }

        Box(contentAlignment = Alignment.BottomCenter) {
            // Avatar with glowing medal ring
            Box(
                modifier = Modifier
                    .size(avatarSize + 10.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f * ringAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = name,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(3.dp, color.copy(alpha = ringAlpha), CircleShape)
                            .clickable { onProfileClick(userId) },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.18f))
                                )
                            )
                            .border(3.dp, color.copy(alpha = ringAlpha), CircleShape)
                            .clickable { onProfileClick(userId) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name.firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (avatarSize.value * 0.4f).sp,
                            color = Color.White,
                        )
                    }
                }
            }
            // Posted-today status dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            posted && late -> StakeAmber
                            posted -> SuccessGreen
                            else -> Color(0xFFFFB300)
                        }
                    )
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (posted) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(12.dp))
                } else {
                    Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (isMe) "You" else name,
            fontWeight = FontWeight.Bold,
            fontSize = if (rank == 1) 14.sp else 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(avatarSize + 22.dp),
        )

        // Score + rank movement
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$score",
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (rank == 1) 22.sp else 18.sp,
                color = color,
                fontFamily = FontFamily.Default,
            )
            if (rankChange != 0) {
                Icon(
                    if (rankChange > 0) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    null,
                    tint = if (rankChange > 0) SuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            challenge.scoreLabel().uppercase(),
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )

        Spacer(Modifier.height(8.dp))
        // Pedestal
        Box(
            modifier = Modifier
                .width(avatarSize + 18.dp)
                .height(pedestalHeight)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.16f))
                    )
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    "$rank",
                    fontWeight = FontWeight.Black,
                    fontSize = if (rank == 1) 30.sp else 22.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Default,
                )
                if (proofPhoto != null) {
                    Spacer(Modifier.height(4.dp))
                    AsyncImage(
                        model = proofPhoto,
                        contentDescription = "Proof",
                        modifier = Modifier
                            .size(if (rank == 1) 30.dp else 24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
                            .clickable { onProofClick(userId, proofPhoto) },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }
        }
    }
}

/**
 * Sleek list row for ranks 4+. Clean, premium, with a "You" highlight,
 * streak chip, score, and a tappable proof thumbnail.
 */
@Composable
fun PremiumStandingRow(
    rank: Int,
    userId: String,
    score: Int,
    challenge: Challenge,
    currentUserId: String,
    todayStr: String,
    onProfileClick: (String) -> Unit,
    onProofClick: (userId: String, photoUrl: String) -> Unit,
    onNudgeClick: ((String) -> Unit)? = null,
    onSupportClick: ((String) -> Unit)? = null,
    scoreSuffix: String? = null,
    modifier: Modifier = Modifier,
) {
    val name = challenge.memberNames[userId] ?: "Unknown"
    val photo = challenge.memberPhotos[userId]
    val isMe = userId == currentUserId
    val posted = challenge.hasCompletedToday(userId, todayStr)
    val late = challenge.isLateProof(userId, todayStr)
    val streak = challenge.streakFor(userId)
    val proofPhoto = challenge.proofPhotos[userId]?.get(todayStr)
    val rankChange = challenge.rankChanges[userId] ?: 0

    val rowBg = if (isMe) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    }
    val borderColor = if (isMe) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onProfileClick(userId) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(8.dp))

        val statusColor = when {
            posted && late -> StakeAmber
            posted -> SuccessGreen
            else -> Color(0xFFFFB300)
        }
        Box {
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = name,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(2.dp, statusColor, CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .border(2.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }

        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isMe) "You" else name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "$streak day streak",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
        }

        if (!isMe && onNudgeClick != null) {
            IconButton(
                onClick = { onNudgeClick(userId) },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = "Nudge $name",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (!isMe && onSupportClick != null) {
            com.saintnico.verdlyhabits.ui.components.coins.SupportIconButton(
                onClick = { onSupportClick(userId) },
                contentDescription = "Support $name",
                modifier = Modifier.size(28.dp),
            )
        }

        if (rankChange != 0) {
            Icon(
                if (rankChange > 0) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                null,
                tint = if (rankChange > 0) SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$score",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                (scoreSuffix ?: challenge.scoreLabel()).uppercase(),
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }

        if (proofPhoto != null) {
            Spacer(Modifier.width(10.dp))
            AsyncImage(
                model = proofPhoto,
                contentDescription = "Proof",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onProofClick(userId, proofPhoto) },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}
