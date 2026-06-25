package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ProofWindowState
import com.saintnico.verdlyhabits.ui.theme.BronzeColor
import com.saintnico.verdlyhabits.ui.theme.DangerRed
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.theme.SilverColor
import com.saintnico.verdlyhabits.ui.theme.StakeAmber
import com.saintnico.verdlyhabits.ui.theme.SuccessGreen
import com.saintnico.verdlyhabits.ui.theme.XPPurple
import kotlinx.coroutines.delay

private data class ProofBadgeStyle(val bg: Color, val fg: Color, val icon: ImageVector, val label: String)

@Composable
fun ProofStatusBadge(
    challenge: Challenge,
    userId: String,
    todayStr: String,
    compact: Boolean = false
) {
    val posted = challenge.hasCompletedToday(userId, todayStr)
    val isLate = challenge.isLateProof(userId, todayStr)
    val after = challenge.isAfterDeadlineProof(userId, todayStr)
    val window = challenge.currentProofWindowState(userId, todayStr)
    val iconSize = if (compact) 10.dp else 12.dp
    val padH = if (compact) 6.dp else 8.dp
    val padV = if (compact) 2.dp else 4.dp

    val pendingPulse = rememberInfiniteTransition(label = "pend")
    val pendingAlpha by pendingPulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pa"
    )

    val style = when {
        posted && isLate -> ProofBadgeStyle(
            DangerRed.copy(alpha = 0.1f),
            DangerRed,
            Icons.Default.History,
            "Late save"
        )
        posted && after -> ProofBadgeStyle(
            StakeAmber.copy(alpha = 0.15f),
            StakeAmber,
            Icons.Default.Timer,
            "Late"
        )
        posted -> ProofBadgeStyle(
            SuccessGreen.copy(alpha = 0.15f),
            SuccessGreen,
            Icons.Default.CheckCircle,
            "Done"
        )
        window == ProofWindowState.Locked -> ProofBadgeStyle(
            DangerRed.copy(alpha = 0.08f),
            DangerRed,
            Icons.Default.Close,
            "Missed"
        )
        else -> ProofBadgeStyle(
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
            Icons.Default.RadioButtonUnchecked,
            "Pending"
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = style.bg
    ) {
        Row(
            Modifier.padding(horizontal = padH, vertical = padV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                style.icon,
                contentDescription = null,
                tint = style.fg.copy(alpha = if (!posted && window != ProofWindowState.Locked) pendingAlpha else 1f),
                modifier = Modifier.size(iconSize)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                style.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = style.fg,
                fontSize = if (compact) 9.sp else 11.sp,
                maxLines = 1
            )
        }
    }
}

private fun challengeTotalDays(challenge: Challenge): Int {
    val dayMs = 24 * 60 * 60 * 1000L
    return (((challenge.endDate - challenge.startDate) / dayMs).toInt()).coerceAtLeast(1)
}

@Composable
fun LeaderboardRow(
    compact: Boolean,
    index: Int,
    leaderboardSize: Int,
    userId: String,
    score: Int,
    challenge: Challenge,
    currentUserId: String,
    todayStr: String,
    modifier: Modifier = Modifier,
    onProofThumbnailClick: ((String) -> Unit)? = null,
    onProfileClick: ((String) -> Unit)? = null,
    enableEnterAnimation: Boolean = true
) {
    val rankColor = when (index) {
        0 -> GoldColor
        1 -> SilverColor
        2 -> BronzeColor
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    }
    val name = challenge.memberNames[userId] ?: "Unknown"
    val photo = challenge.memberPhotos[userId]
    val isMe = userId == currentUserId
    val profileModifier = if (onProfileClick != null) {
        Modifier.clickable { onProfileClick.invoke(userId) }
    } else {
        Modifier
    }
    val proofPhoto = challenge.proofPhotos[userId]?.get(todayStr)
    val hasPostedToday = challenge.hasCompletedToday(userId, todayStr)
    val streak = challenge.streakFor(userId)
    val reactionPoints = challenge.reactionPointsFor(userId)
    val scoreLabel = challenge.scoreLabel()
    val completion = challenge.completionRate(userId)
    val totalDays = challengeTotalDays(challenge)
    val rankVector = when (index) {
        0 -> Icons.Default.EmojiEvents
        1, 2 -> Icons.Default.MilitaryTech
        else -> null
    }

    var rowVisible by remember { mutableStateOf(!enableEnterAnimation) }
    LaunchedEffect(index, enableEnterAnimation) {
        if (enableEnterAnimation) {
            delay(index * 60L)
            rowVisible = true
        } else {
            rowVisible = true
        }
    }

    val rankChange = challenge.rankChanges[userId] ?: 0

    val rowBg: Color = when {
        !compact && rankChange < 0 -> DangerRed.copy(alpha = 0.04f)
        compact -> Color.Transparent
        index == 0 -> GoldColor.copy(alpha = 0.1f)
        index == 1 -> SilverColor.copy(alpha = 0.08f)
        index == 2 -> BronzeColor.copy(alpha = 0.06f)
        index % 2 == 0 -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = rowVisible,
            enter = slideInVertically { it / 2 } + fadeIn(tween(320)),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .then(
                        if (!compact && index < 3) {
                            Modifier.drawBehind {
                                drawLine(
                                    color = when (index) {
                                        0 -> GoldColor
                                        1 -> SilverColor
                                        else -> BronzeColor
                                    },
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                    .background(rowBg)
                    .padding(vertical = if (compact) 6.dp else 10.dp, horizontal = if (compact) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (compact) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(rankColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = rankColor)
                    }
                } else {
                    when {
                        rankVector != null -> Box(contentAlignment = Alignment.Center) {
                            if (index == 0) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .background(GoldColor.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(rankVector, null, tint = GoldColor, modifier = Modifier.size(22.dp))
                                }
                            } else {
                                Icon(rankVector, null, tint = rankColor, modifier = Modifier.size(24.dp))
                            }
                        }

                        else -> Text(
                            "#${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankColor.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.width(if (compact) 10.dp else 12.dp))

                val avatarSize = if (compact) 30.dp else 40.dp
                val lateToday = challenge.isLateProof(userId, todayStr)
                val borderColor = when {
                    hasPostedToday && lateToday -> StakeAmber
                    hasPostedToday -> SuccessGreen
                    !hasPostedToday && challenge.isActive -> Color(0xFFFFB300)
                    else -> rankColor
                }
                Box {
                    if (!photo.isNullOrBlank()) {
                        AsyncImage(
                            model = photo,
                            contentDescription = null,
                            modifier = Modifier
                                .size(avatarSize)
                                .clip(CircleShape)
                                .border(2.dp, borderColor, CircleShape)
                                .then(profileModifier)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(avatarSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(2.dp, borderColor, CircleShape)
                                .then(profileModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = if (compact) 11.sp else 13.sp
                            )
                        }
                    }
                    if (!compact) {
                        val dotBg = when {
                            hasPostedToday && lateToday -> StakeAmber
                            hasPostedToday -> SuccessGreen
                            else -> Color(0xFFFFB300)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                                .offset(x = 2.dp, y = 2.dp)
                                .clip(CircleShape)
                                .background(dotBg)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasPostedToday) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(8.dp)
                                )
                            } else {
                                Text(
                                    "!",
                                    fontSize = 7.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (hasPostedToday) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(8.dp)
                                .offset(x = 2.dp, y = 2.dp)
                                .clip(CircleShape)
                                .background(if (lateToday) StakeAmber else SuccessGreen)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f).then(profileModifier)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isMe) "You" else name,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compact) 13.sp else 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!compact && index == 0 && rankChange > 0) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.EmojiEvents, null, tint = GoldColor, modifier = Modifier.size(16.dp))
                        }
                        if (challenge.hasStreakFreeze(userId)) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Shield, null, tint = XPPurple, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (!compact) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$streak day streak",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                        ) {
                            val frac = (streak.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .fillMaxWidth(frac)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                if (compact) {
                    Icon(
                        if (challenge.challengeMode == com.saintnico.verdlyhabits.data.model.ChallengeMode.STREAK) Icons.Default.LocalFireDepartment else Icons.Default.EmojiEvents,
                        null,
                        tint = if (score > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("$score", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                    val rc = challenge.rankChanges[userId] ?: 0
                    if (rc > 0) {
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropUp, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    } else if (rc < 0) {
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropDown, null, tint = DangerRed, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(completion.coerceIn(0.05f, 1f))
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        )
                    }
                } else {
                    Box(contentAlignment = Alignment.CenterEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (rankChange != 0) {
                                val glowT = rememberInfiniteTransition(label = "rankGlow")
                                val glowA by glowT.animateFloat(
                                    initialValue = 0.28f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
                                    label = "glowA"
                                )
                                val glowC = if (rankChange > 0) SuccessGreen else DangerRed
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 2.dp)) {
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .background(glowC.copy(alpha = glowA), CircleShape)
                                    )
                                    Icon(
                                        if (rankChange > 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = glowC,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    if (rankChange > 0) "+$rankChange" else "$rankChange",
                                    color = glowC,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$score",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontFamily = MaterialTheme.typography.headlineMedium.fontFamily
                                )
                                Text(
                                    when (challenge.challengeMode) {
                                        com.saintnico.verdlyhabits.data.model.ChallengeMode.STREAK -> scoreLabel
                                        com.saintnico.verdlyhabits.data.model.ChallengeMode.REACTIONS -> "$reactionPoints pts"
                                        com.saintnico.verdlyhabits.data.model.ChallengeMode.HYBRID -> "$reactionPoints react pts"
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            ProofStatusBadge(challenge = challenge, userId = userId, todayStr = todayStr, compact = true)
                            if (proofPhoto != null) {
                                Spacer(Modifier.width(8.dp))
                                AsyncImage(
                                    model = proofPhoto,
                                    contentDescription = "Proof",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = onProofThumbnailClick != null) {
                                            onProofThumbnailClick?.invoke(proofPhoto)
                                        },
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!compact && index < leaderboardSize - 1) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
            )
        }
    }
}
