package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.data.remote.firestore.DuoMilestoneReached
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.engine.DuoStreakEngine
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.ui.components.BurstIntensity
import com.saintnico.verdlyhabits.ui.components.CelebrationKonfetti
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlinx.coroutines.delay

private val Mint = Color(0xFF52B788)
private val Shell = Color(0xFF0F1A14)
private val ShellDeep = Color(0xFF132A1F)
private val Gold = Color(0xFFFFE8A3)
private val RiskAmber = Color(0xFFFFB300)
private val RiskRed = Color(0xFFFF6B6B)
private val RiskShell = Color(0xFF2A1510)

@Composable
fun DuoMilestoneCelebration(
    milestone: DuoMilestoneReached,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val cardScale = remember { Animatable(0.6f) }
    val titleAlpha = remember { Animatable(0f) }
    val bodyAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }
  val xpTarget = GamificationEngine.duoMilestoneXp(milestone.streakDays)
    var xpTick by remember(milestone) { mutableIntStateOf(0) }
    val animatedXp by animateIntAsState(xpTarget, tween(900, easing = FastOutSlowInEasing), label = "xp")
    val konfettiKey = remember(milestone) { System.currentTimeMillis() }

    val celebrationComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.duo_celebration))
    val celebrationProgress by animateLottieCompositionAsState(
        composition = celebrationComp,
        iterations = LottieConstants.IterateForever,
        speed = 1.1f,
    )

    LaunchedEffect(milestone) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        cardScale.snapTo(0.6f)
        titleAlpha.snapTo(0f)
        bodyAlpha.snapTo(0f)
        buttonsAlpha.snapTo(0f)
        xpTick = 0
        cardScale.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 340f))
        delay(80)
        titleAlpha.animateTo(1f, tween(420))
        delay(120)
        bodyAlpha.animateTo(1f, tween(380))
        delay(100)
        xpTick = xpTarget
        buttonsAlpha.animateTo(1f, tween(360))
        delay(60)
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    Box(Modifier.fillMaxSize()) {
        CelebrationKonfetti(trigger = konfettiKey, intensity = BurstIntensity.HUGE)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A3D2E), Color.Black.copy(0.94f)),
                        radius = 900f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                LottieAnimation(
                    composition = celebrationComp,
                    progress = { celebrationProgress },
                    modifier = Modifier.size(220.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(cardScale.value)
                        .graphicsLayer { alpha = titleAlpha.value },
                ) {
                    Text(
                        DuoStreakEngine.milestoneTitle(milestone.streakDays),
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                DuoStreakEngine.milestoneSubtitle(milestone.streakDays, milestone.buddyUsername),
                fontFamily = dmSansFamily,
                fontSize = 15.sp,
                color = Color.White.copy(0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = bodyAlpha.value },
            )
            if (xpTarget > 0) {
                Spacer(Modifier.height(14.dp))
                AnimatedVisibility(
                    visible = xpTick > 0,
                    enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(),
                ) {
                    Text(
                        "+$animatedXp XP",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Mint,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.graphicsLayer { alpha = buttonsAlpha.value },
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Keep going", fontFamily = dmSansFamily)
                }
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DuoStreakBrokenBanner(
    previousStreak: Int,
    buddyName: String,
    onDismiss: () -> Unit,
    visible: Boolean = true,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            repeat(3) {
                shake.animateTo(4f, tween(50))
                shake.animateTo(-4f, tween(50))
            }
            shake.animateTo(0f, tween(80))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(tween(400)),
        exit = slideOutVertically { -it / 3 } + fadeOut(tween(280)),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = shake.value.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RiskRed.copy(0.14f)),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = RiskRed,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { alpha = 0.65f },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Duo streak ended at ${previousStreak}d",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White,
                    )
                    Text(
                        "You and $buddyName missed a day. Start fresh today.",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(0.7f),
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("OK", color = Mint, fontFamily = dmSansFamily)
                }
            }
        }
    }
}

@Composable
fun DuoStreakCard(
    state: DuoStreakState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onApplyGrace: (() -> Unit)? = null,
    onNudgeBuddy: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = if (state.streakAtRisk) RiskAmber else Mint
    val haptic = LocalHapticFeedback.current
    val infinite = rememberInfiniteTransition(label = "duo_ambient")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "avatar_pulse",
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.12f,
        targetValue = if (state.streakAtRisk) 0.55f else 0.32f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "border_glow",
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shield_shimmer",
    )
    val streakScale by animateFloatAsState(
        targetValue = if (state.bothDoneToday) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "streak_pop",
    )
    val dayProgress = remember(state.countdownHours, state.countdownMinutes) {
        val totalMins = 24 * 60
        val left = state.countdownHours * 60 + state.countdownMinutes
        (totalMins - left).toFloat() / totalMins.toFloat()
    }
    val countdownProgress by animateFloatAsState(dayProgress, tween(900, easing = FastOutSlowInEasing), label = "day_prog")

    val flameComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.duo_fire))
    val flameProgress by animateLottieCompositionAsState(
        composition = flameComp,
        iterations = LottieConstants.IterateForever,
        speed = if (state.streakAtRisk) 1.35f else if (state.bothDoneToday) 0.85f else 1f,
    )

    LaunchedEffect(state.bothDoneToday) {
        if (state.bothDoneToday) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    val showBuddyNudge = state.status == "active" &&
        state.buddyDoneToday &&
        !state.myDoneToday &&
        !state.isIncomingInvite

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bgColors = when {
        state.streakAtRisk -> if (isDark) {
            listOf(Color(0xFF3A2A1C), Color(0xFF2A2118))
        } else {
            listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))
        }
        state.streakDays >= 30 -> if (isDark) {
            listOf(Color(0xFF3A3218), Color(0xFF2A2616))
        } else {
            listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))
        }
        state.streakDays >= 14 -> if (isDark) {
            listOf(Color(0xFF2C2438), Color(0xFF221C2E))
        } else {
            listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7))
        }
        state.streakDays >= 7 -> if (isDark) {
            listOf(Color(0xFF1E2A36), Color(0xFF18222C))
        } else {
            listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
        }
        else -> if (isDark) {
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.surface,
            )
        } else {
            listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
        }
    }

    // 3D Parallax Tilt Effect
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    val animatedTiltX by animateFloatAsState(tiltX, spring(dampingRatio = 0.5f, stiffness = 200f))
    val animatedTiltY by animateFloatAsState(tiltY, spring(dampingRatio = 0.5f, stiffness = 200f))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = animatedTiltX
                rotationY = animatedTiltY
                cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        tiltX = 0f
                        tiltY = 0f
                    },
                    onDragCancel = {
                        tiltX = 0f
                        tiltY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // Map drag to subtle rotation (-8 to 8 degrees)
                    tiltX = (tiltX - dragAmount.y * 0.1f).coerceIn(-8f, 8f)
                    tiltY = (tiltY + dragAmount.x * 0.1f).coerceIn(-8f, 8f)
                }
            }
            .drawBehind {
                val stroke = 2.5.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = glowAlpha),
                    size = Size(size.width + stroke, size.height + stroke),
                    topLeft = Offset(-stroke / 2f, -stroke / 2f),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = stroke),
                )
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(bgColors),
                    RoundedCornerShape(22.dp),
                )
                .border(1.dp, accent.copy(if (state.streakAtRisk) 0.5f else 0.28f), RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        LottieAnimation(
                            composition = flameComp,
                            progress = { flameProgress },
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Duo streak",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (state.status == "active" && state.streakDays > 0) {
                        Spacer(Modifier.width(8.dp))
                        DuoTierChip(
                            label = DuoStreakEngine.tierLabel(state.streakDays),
                            streakDays = state.streakDays,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    val canNudgeBuddy = onNudgeBuddy != null &&
                        state.status == "active" &&
                        !state.isIncomingInvite &&
                        !state.buddyDoneToday
                    if (canNudgeBuddy) {
                        IconButton(
                            onClick = { onNudgeBuddy?.invoke() },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = "Nudge ${state.buddyUsername.ifBlank { "buddy" }}",
                                tint = accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                    if (state.status == "active") {
                        AnimatedContent(
                            targetState = state.streakDays,
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()).togetherWith(
                                    scaleOut() + fadeOut(),
                                )
                            },
                            label = "streak_days",
                        ) { days ->
                            Text(
                                "${days}d",
                                modifier = Modifier.scale(streakScale),
                                fontFamily = dmSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Gold,
                            )
                        }
                    }
                }

                if (state.status == "active" && !state.isIncomingInvite) {
                    Spacer(Modifier.height(8.dp))
                    DuoWeekFlameStrip(
                        streakDays = state.streakDays,
                        bothDoneToday = state.bothDoneToday,
                        accent = accent,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        DuoStreakEngine.vibeLine(
                            streakDays = state.streakDays,
                            buddyName = state.buddyUsername.ifBlank { "your buddy" },
                            bothDone = state.bothDoneToday,
                            atRisk = state.streakAtRisk,
                            buddyDone = state.buddyDoneToday,
                            myDone = state.myDoneToday,
                        ),
                        fontFamily = dmSansFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            state.streakAtRisk -> RiskAmber
                            state.bothDoneToday -> Mint
                            else -> MaterialTheme.colorScheme.onSurface.copy(0.72f)
                        },
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { countdownProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(0.1f),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        DuoStreakEngine.countdownLabel(state.countdownHours, state.countdownMinutes),
                        fontFamily = dmSansFamily,
                        fontSize = 10.sp,
                        fontWeight = if (state.streakAtRisk) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (state.streakAtRisk) RiskAmber else MaterialTheme.colorScheme.onSurface.copy(0.7f),
                    )
                }

                AnimatedVisibility(
                    visible = showBuddyNudge,
                    enter = slideInVertically { -it } + fadeIn(tween(380)),
                    exit = slideOutVertically { -it } + fadeOut(),
                ) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${state.buddyUsername} just finished — don't break the streak!",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RiskAmber,
                        )
                    }
                }

                if (state.streakAtRisk) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Streak at risk — finish all habits before midnight.",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RiskAmber,
                    )
                    if (state.graceAvailable && onApplyGrace != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onApplyGrace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { alpha = shimmer },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(0.75f)),
                        ) {
                            Icon(Icons.Rounded.Shield, null, tint = Gold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Use Pro streak shield", fontFamily = dmSansFamily, color = Gold, fontSize = 12.sp)
                        }
                    } else if (state.graceUsedThisWeek) {
                        Text(
                            "Pro shield used this week",
                            fontFamily = dmSansFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (state.isIncomingInvite) {
                    Text(
                        "${state.buddyUsername} wants to be your accountability buddy.",
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.75f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                            Text("Decline", fontFamily = dmSansFamily)
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        ) {
                            Text("Accept", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DuoMemberColumn(
                            label = "You",
                            progress = state.myProgress,
                            done = state.myDoneToday,
                            photoUrl = null,
                            accent = accent,
                            pulse = if (showBuddyNudge) pulse else 1f,
                            modifier = Modifier.weight(1f),
                        )
                        DuoSyncBridge(
                            myDone = state.myDoneToday,
                            buddyDone = state.buddyDoneToday,
                            atRisk = state.streakAtRisk,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        DuoMemberColumn(
                            label = state.buddyUsername.ifBlank { "Buddy" },
                            progress = state.buddyProgress,
                            done = state.buddyDoneToday,
                            photoUrl = state.buddyPhotoUrl,
                            accent = accent,
                            pulse = if (state.buddyDoneToday && !state.myDoneToday) pulse else 1f,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AnimatedVisibility(
                        visible = state.bothDoneToday,
                        enter = scaleIn(spring(dampingRatio = 0.55f)) + fadeIn(),
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "+${GamificationEngine.duoMilestoneXp(state.streakDays + 1).coerceAtLeast(5)} XP at midnight",
                                fontFamily = dmSansFamily,
                                fontSize = 11.sp,
                                color = Gold.copy(0.85f),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (state.status == "pending") {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Waiting for ${state.buddyUsername} to accept…",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF95D5B2),
                        )
                    }

                    // Upcoming Milestone progress to keep them coming back
                    if (state.status == "active" && !state.isIncomingInvite) {
                        DuoUpcomingMilestoneSection(state.streakDays, accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuoTierChip(label: String, streakDays: Int) {
    val chipColors = when {
        streakDays >= 30 -> listOf(Color(0xFFFFE082), Color(0xFFFF8F00))
        streakDays >= 14 -> listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA))
        streakDays >= 7 -> listOf(Color(0xFF90CAF9), Color(0xFF1565C0))
        else -> listOf(Mint.copy(0.35f), Mint.copy(0.15f))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(chipColors.map { it.copy(alpha = 0.35f) }))
            .border(1.dp, chipColors.first().copy(0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = chipColors.first(),
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(0.85f),
        )
    }
}

/** Snapchat-style 7-day flame strip — filled dots = streak momentum. */
@Composable
private fun DuoWeekFlameStrip(
    streakDays: Int,
    bothDoneToday: Boolean,
    accent: Color,
) {
    val filled = streakDays.coerceIn(0, 7)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(7) { index ->
            val dayNum = index + 1
            val isFilled = dayNum <= filled
            val isToday = dayNum == filled.coerceAtLeast(1)
            val dotScale by animateFloatAsState(
                targetValue = if (isToday && bothDoneToday) 1.2f else 1f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "flame_dot",
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = if (isFilled) dotScale else 1f
                        scaleY = if (isFilled) dotScale else 1f
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isFilled) 22.dp else 18.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) {
                                Brush.radialGradient(
                                    listOf(accent.copy(0.9f), accent.copy(0.35f)),
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(0.08f),
                                        MaterialTheme.colorScheme.onSurface.copy(0.04f),
                                    ),
                                )
                            },
                        )
                        .then(
                            if (isToday && bothDoneToday) {
                                Modifier.border(1.5.dp, Gold.copy(0.8f), CircleShape)
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isFilled) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isToday && bothDoneToday) Gold else Color.White.copy(0.95f),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuoUpcomingMilestoneSection(currentStreak: Int, accent: Color) {
    val nextMilestone = DuoStreakEngine.CELEBRATION_MILESTONES.firstOrNull { it > currentStreak } ?: return
    val prevMilestone = DuoStreakEngine.CELEBRATION_MILESTONES.lastOrNull { it <= currentStreak } ?: 0
    val totalRequired = nextMilestone - prevMilestone
    val currentProgress = currentStreak - prevMilestone
    val fraction = if (totalRequired > 0) currentProgress.toFloat() / totalRequired.toFloat() else 0f
    
    Spacer(Modifier.height(18.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Gold.copy(alpha = 0.2f), Gold.copy(alpha = 0.05f))
                        )
                    )
                    .border(1.dp, Gold.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Next Milestone: ${DuoStreakEngine.milestoneTitle(nextMilestone)}",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Gold,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${nextMilestone - currentStreak}d left",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Gold.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun DuoSyncBridge(
    myDone: Boolean,
    buddyDone: Boolean,
    atRisk: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (atRisk) RiskAmber else Mint
    val target = when {
        myDone && buddyDone -> 1f
        myDone || buddyDone -> 0.55f
        else -> 0.12f
    }
    val progress by animateFloatAsState(target, spring(dampingRatio = 0.65f), label = "sync_bridge")
    val synced = myDone && buddyDone
    val bridgePulse = rememberInfiniteTransition(label = "bridge_pulse")
    val pulseAlpha by bridgePulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "bridge_glow",
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(0.12f)
    Column(
        modifier = modifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (synced) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                tint = accent.copy(alpha = pulseAlpha),
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(
                "vs",
                fontFamily = frauncesFamily,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(0.35f),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(if (synced) 6.dp else 4.dp)) {
            val h = size.height
            drawRoundRect(
                color = trackColor,
                size = Size(size.width, h),
                cornerRadius = CornerRadius(h / 2f),
            )
            if (progress > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            accent.copy(if (synced) pulseAlpha * 0.6f else 0.5f),
                            accent.copy(if (synced) pulseAlpha else 1f),
                        ),
                    ),
                    size = Size(size.width * progress, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
            }
        }
    }
}

@Composable
private fun DuoMemberColumn(
    label: String,
    progress: String,
    done: Boolean,
    photoUrl: String?,
    accent: Color,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val (completed, total) = remember(progress) { parseProgressFraction(progress) }
    val ringProgress by animateFloatAsState(
        if (total > 0) completed.toFloat() / total else 0f,
        tween(700, easing = FastOutSlowInEasing),
        label = "ring",
    )
    val checkScale by animateFloatAsState(
        if (done) 1f else 0.85f,
        spring(dampingRatio = 0.5f),
        label = "check_pop",
    )
    val ringTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.12f)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size((56 * pulse).dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                drawArc(
                    color = ringTrackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.12f))
                    .border(1.dp, if (done) accent else accent.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (photoUrl != null) {
                    com.saintnico.verdlyhabits.ui.components.ProfileAvatar(
                        photoUri = photoUrl,
                        size = 44.dp,
                        fallbackTint = accent,
                        fallbackBackground = accent.copy(0.12f),
                    )
                } else {
                    Icon(Icons.Rounded.Person, null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(progress, fontFamily = dmSansFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) accent else MaterialTheme.colorScheme.onSurface.copy(0.35f),
            modifier = Modifier
                .size(18.dp)
                .scale(checkScale),
        )
    }
}

private fun parseProgressFraction(progress: String): Pair<Int, Int> {
    val parts = progress.split("/")
    val done = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
    val total = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    return done to total
}
