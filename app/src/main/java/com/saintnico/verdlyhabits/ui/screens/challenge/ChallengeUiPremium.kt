package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.ui.components.rememberPressScale
import com.saintnico.verdlyhabits.ui.theme.DangerRed
import com.saintnico.verdlyhabits.ui.theme.StakeAmber
import com.saintnico.verdlyhabits.ui.theme.SuccessGreen
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.theme.isAppearanceDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val EaseInOutSine = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)

@Composable
internal fun PremiumChallengesHeader(
    rankLabel: String,
    activeCount: Int,
    streak: Int,
    primary: Color,
    tertiary: Color
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.isAppearanceDark()
    val trophyScale = rememberInfiniteTransition(label = "trophy")
    val tScale by trophyScale.animateFloat(
        0.9f, 1.1f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "tS"
    )
    val headerBrush = if (dark) {
        Brush.linearGradient(
            colors = listOf(primary, tertiary),
            start = Offset.Zero,
            end = Offset(400f, 200f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                scheme.primaryContainer,
                scheme.tertiaryContainer
            ),
            start = Offset.Zero,
            end = Offset(400f, 200f)
        )
    }
    val titleColor = if (dark) Color.White else scheme.onPrimaryContainer
    val subtitleColor = if (dark) Color.White.copy(alpha = 0.7f) else scheme.onPrimaryContainer.copy(alpha = 0.75f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(headerBrush)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Challenges",
                    style = MaterialTheme.typography.headlineMedium,
                    color = titleColor,
                    fontWeight = FontWeight.Normal,
                    fontSize = 28.sp
                )
                Text(
                    "Prove your grind.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.EmojiEvents,
                null,
                tint = titleColor,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = tScale; scaleY = tScale }
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("YOUR RANK", rankLabel, dark, scheme)
            StatChip("ACTIVE", "$activeCount", dark, scheme)
            StatChip("STREAK", "$streak", dark, scheme)
        }
    }
}

@Composable
private fun StatChip(title: String, value: String, dark: Boolean, scheme: ColorScheme) {
    val chipBg = if (dark) Color.White.copy(alpha = 0.15f) else scheme.surface.copy(alpha = 0.55f)
    val titleC = if (dark) Color.White.copy(0.65f) else scheme.onSurface.copy(alpha = 0.55f)
    val valueC = if (dark) Color.White else scheme.onSurface
    Surface(
        color = chipBg,
        shape = RoundedCornerShape(50)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, color = titleC, fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = valueC, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnonymousBannerPremium(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.isAppearanceDark()
    val shimmer = rememberInfiniteTransition(label = "sh")
    val shift by shimmer.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "shv"
    )
    val brush = if (dark) {
        Brush.linearGradient(
            listOf(scheme.primary, scheme.secondary),
            start = Offset(shift * 80f, 0f),
            end = Offset(400f + shift * 80f, 200f)
        )
    } else {
        Brush.linearGradient(
            listOf(scheme.primaryContainer, scheme.secondaryContainer),
            start = Offset(shift * 80f, 0f),
            end = Offset(400f + shift * 80f, 200f)
        )
    }
    val fg = if (dark) Color.White else scheme.onPrimaryContainer
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = fg, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("You're Anonymous", color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Claim your @name to show up on leaderboards.", color = fg.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                Icon(Icons.Default.ArrowForwardIos, null, tint = fg.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun PremiumExpandableFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    primary: Color,
    tertiary: Color
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.isAppearanceDark()
    val (fabIs, fabMod) = rememberPressScale()
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInHorizontally { it / 2 } + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + fadeOut()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val (i1, m1) = rememberPressScale()
                val (i2, m2) = rememberPressScale()
                val miniFabBg = if (dark) Color.White.copy(0.2f) else scheme.primary.copy(alpha = 0.12f)
                val miniFabFg = if (dark) Color.White else scheme.primary
                Button(
                    onClick = onCreate,
                    modifier = m1,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = miniFabBg),
                    interactionSource = i1
                ) { Text("Create", color = miniFabFg, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = onJoin,
                    modifier = m2,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = miniFabBg),
                    interactionSource = i2
                ) { Text("Join", color = miniFabFg, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(primary, tertiary)))
                .clickable(interactionSource = fabIs, indication = null) { onToggle() }
                .then(fabMod),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = if (dark) Color.White else scheme.onPrimary)
        }
    }
}

@Composable
internal fun IllustratedEmptyChallenges(
    onStartChallenge: () -> Unit,
    onJoinWithId: () -> Unit
) {
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(com.saintnico.verdlyhabits.R.raw.empty_challenges))
    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (comp != null) {
            LottieAnimation(comp, iterations = LottieConstants.IterateForever, modifier = Modifier.size(200.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(0.35f, 0.22f, 0.12f).forEach { a ->
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = a))
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Your first arena is waiting",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            "Joining or creating is already a win — you’re choosing people who will hold you to it.",
            fontFamily = dmSansFamily,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(20.dp))
        val (iJoin, mJoin) = rememberPressScale()
        OutlinedButton(
            onClick = onJoinWithId,
            modifier = mJoin
                .fillMaxWidth(0.92f)
                .padding(horizontal = 8.dp)
                .height(52.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, primary.copy(alpha = 0.55f)),
            interactionSource = iJoin
        ) {
            Text("Step into a friend’s arena", fontWeight = FontWeight.Bold, color = primary)
        }
        Spacer(Modifier.height(12.dp))
        val (i, m) = rememberPressScale()
        Button(
            onClick = onStartChallenge,
            modifier = m
                .fillMaxWidth(0.92f)
                .padding(horizontal = 8.dp)
                .height(58.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            interactionSource = i
        ) {
            Text("Host your own arena →", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Tip: tap + anytime — create or join in one breath.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivePastToggle(isPast: Boolean, onPast: (Boolean) -> Unit) {
    val activeC by animateColorAsState(
        if (!isPast) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(300), label = "a"
    )
    val pastC by animateColorAsState(
        if (isPast) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(300), label = "p"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = { onPast(false) },
            shape = RoundedCornerShape(50),
            color = activeC,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Active",
                    fontWeight = if (!isPast) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isPast) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
        Surface(
            onClick = { onPast(true) },
            shape = RoundedCornerShape(50),
            color = pastC,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Past",
                    fontWeight = if (isPast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPast) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengeCardPremium(
    challenge: Challenge,
    currentUserId: String,
    onClick: () -> Unit,
    isEnded: Boolean = false,
    todayStr: String
) {
    val leaderboard = challenge.leaderboard()
    val initials = challenge.habitName.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
        .ifBlank { challenge.habitName.take(2).uppercase() }
    val dayMs = 24 * 60 * 60 * 1000L
    val totalDays = (((challenge.endDate - challenge.startDate) / dayMs).toInt()).coerceAtLeast(1)
    val elapsed = challenge.daysElapsed().coerceAtLeast(0)
    val frac = (elapsed.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.isAppearanceDark()
    val cardBg = scheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.98f else 1f)
    val primaryC = MaterialTheme.colorScheme.primary
    val tertiaryC = MaterialTheme.colorScheme.tertiary
    val onBgC = MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isEnded) 0.dp else 8.dp),
        border = BorderStroke(1.dp, primaryC.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, primaryC.copy(alpha = 0.04f))
                        )
                    )
                }
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(primaryC, tertiaryC)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                challenge.habitName,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                color = onBgC,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            MemberRivalRow(challenge, isDark)
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = primaryC.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, primaryC.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = primaryC,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        challenge.deadlineLabel(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryC,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    CountdownBadgePremium(challenge, isEnded, currentUserId, todayStr)
                }
                if (challenge.stake.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    StakeChipRow(challenge.stake)
                }
                Spacer(Modifier.height(10.dp))
                leaderboard.take(3).forEachIndexed { index, (uid, score) ->
                    LeaderboardRow(
                        compact = true,
                        index = index,
                        leaderboardSize = 3,
                        userId = uid,
                        score = score,
                        challenge = challenge,
                        currentUserId = currentUserId,
                        todayStr = todayStr,
                        enableEnterAnimation = false
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(onBgC.copy(alpha = 0.08f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .height(6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(primaryC, tertiaryC)
                                ),
                                RoundedCornerShape(50)
                            )
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Day $elapsed of $totalDays",
                        fontSize = 11.sp,
                        color = onBgC.copy(alpha = 0.55f)
                    )
                    Text(
                        "${challenge.daysRemaining()} days left",
                        fontSize = 11.sp,
                        color = onBgC.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRivalRow(challenge: Challenge, isDark: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val onC = scheme.onSurface.copy(alpha = 0.55f)
    val ring = scheme.outline.copy(alpha = if (isDark) 0.35f else 0.25f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${challenge.members.size} rivals", fontSize = 12.sp, color = onC)
        Spacer(Modifier.width(8.dp))
        Row {
            challenge.members.take(3).forEachIndexed { i, uid ->
                val url = challenge.memberPhotos[uid]
                val off = (-8 * i).dp
                Box(Modifier.offset(x = off)) {
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ring, CircleShape)
                        )
                    } else {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .border(1.5.dp, ring, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (challenge.memberNames[uid] ?: "?").first().uppercase(),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownBadgePremium(
    challenge: Challenge,
    isEnded: Boolean,
    currentUserId: String,
    todayStr: String
) {
    if (isEnded) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)) {
            Text("Ended", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
        }
        return
    }
    val zone = ZoneId.systemDefault()
    var label by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf(0) }
    LaunchedEffect(
        challenge.id,
        challenge.dailyDeadline,
        todayStr,
        currentUserId,
        challenge.hasCompletedToday(currentUserId, todayStr)
    ) {
        while (isActive) {
            val posted = challenge.hasCompletedToday(currentUserId, todayStr)
            if (posted) {
                label = "Done"
                tier = 4
                delay(30_000)
                continue
            }
            val now = LocalDateTime.now(zone)
            val endOfDay = when (challenge.dailyDeadline) {
                "ANYTIME" -> LocalDate.now(zone).atTime(23, 59, 59)
                else -> LocalDate.now(zone).atTime(challenge.deadlineHour().coerceAtMost(23), 0)
            }
            val diff = Duration.between(now, endOfDay)
            if (diff.isNegative || diff.isZero) {
                label = "Missed deadline"
                tier = 3
            } else {
                val totalMin = diff.toMinutes()
                val hours = diff.toHours()
                val minutes = diff.toMinutesPart()
                val seconds = diff.toSecondsPart()
                label = when {
                    hours >= 1 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m ${seconds}s"
                }
                tier = when {
                    hours >= 3 -> 0
                    hours >= 1 -> 1
                    else -> 2
                }
            }
            delay(1000)
        }
    }
    val pulse = rememberInfiniteTransition(label = "cd")
    val alpha by pulse.animateFloat(
        if (tier == 2 || tier == 3) 0.55f else 1f,
        1f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "a"
    )
    val (bg, fg, usePulse) = when (tier) {
        4 -> Triple(SuccessGreen.copy(alpha = 0.18f), SuccessGreen, false)
        3 -> Triple(DangerRed.copy(alpha = 0.85f), Color.White, true)
        2 -> Triple(DangerRed.copy(alpha = 0.9f), Color.White, true)
        1 -> Triple(StakeAmber.copy(alpha = 0.2f), StakeAmber, false)
        else -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary, false)
    }
    Surface(
        color = if (usePulse) bg.copy(alpha = alpha.coerceIn(0.55f, 1f)) else bg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tier == 4) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (tier == 1 || tier == 2 || tier == 3) FontWeight.Bold else FontWeight.Medium,
                color = fg
            )
        }
    }
}

@Composable
private fun StakeChipRow(stake: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(StakeAmber.copy(alpha = 0.15f), StakeAmber.copy(alpha = 0.05f))))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "Stake: $stake",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
            )
        }
        Text("WIN", fontWeight = FontWeight.Black, color = StakeAmber, fontSize = 13.sp)
    }
}
