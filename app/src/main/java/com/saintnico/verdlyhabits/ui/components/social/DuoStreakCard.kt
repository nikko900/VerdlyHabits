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
import androidx.compose.foundation.layout.aspectRatio
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
import com.google.firebase.auth.FirebaseAuth
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.res.painterResource
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
    buddyNudged: Boolean = false,
    onCoverBuddy: ((((Boolean, String?) -> Unit) -> Unit))? = null,
    myPhotoUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val accent = if (state.streakAtRisk) RiskAmber else Mint
    val haptic = LocalHapticFeedback.current
    var coverPhase by remember { mutableStateOf(com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Idle) }
    var showCoverHandshake by remember { mutableStateOf(false) }
    val infinite = rememberInfiniteTransition(label = "duo_ambient")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "avatar_pulse",
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shield_shimmer",
    )
    val streakScale by animateFloatAsState(
        targetValue = if (state.bothDoneToday) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "streak_pop",
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
    val canCoverBuddy = onCoverBuddy != null &&
        state.status == "active" &&
        !state.isIncomingInvite &&
        state.myDoneToday &&
        !state.buddyDoneToday
    val canNudgeBuddy = onNudgeBuddy != null &&
        state.status == "active" &&
        !state.isIncomingInvite &&
        !state.buddyDoneToday &&
        !state.bothDoneToday

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            if (state.status == "active" && !state.isIncomingInvite) {
                DuoRootsHero(
                    bothLocked = state.bothDoneToday,
                    atRisk = state.streakAtRisk,
                    streakDays = state.streakDays,
                    streakScale = streakScale,
                    onNudge = if (canNudgeBuddy && !buddyNudged) onNudgeBuddy else null,
                    buddyNudged = canNudgeBuddy && buddyNudged,
                )
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (state.status == "active" && !state.isIncomingInvite) {
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            state.streakAtRisk -> RiskAmber
                            state.bothDoneToday -> Mint
                            else -> MaterialTheme.colorScheme.onSurface.copy(0.72f)
                        },
                        maxLines = 2,
                    )
                    if (!state.bothDoneToday) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            DuoStreakEngine.countdownLabel(state.countdownHours, state.countdownMinutes),
                            fontFamily = dmSansFamily,
                            fontSize = 10.sp,
                            fontWeight = if (state.streakAtRisk) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (state.streakAtRisk) RiskAmber else MaterialTheme.colorScheme.onSurface.copy(0.55f),
                        )
                    }
                    if (canCoverBuddy) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (coverPhase == com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Loading) return@OutlinedButton
                                coverPhase = com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Loading
                                onCoverBuddy?.invoke { ok, err ->
                                    if (ok) {
                                        coverPhase = com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Success
                                        showCoverHandshake = true
                                    } else {
                                        coverPhase = com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Idle
                                    }
                                }
                            },
                            enabled = coverPhase != com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Loading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint),
                        ) {
                            com.saintnico.verdlyhabits.ui.components.VerdlyActionButtonContent(
                                phase = coverPhase,
                                successLabel = "Covered",
                                contentColor = Mint,
                            ) {
                                Text("Cover their day", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Text(
                            "You finished. This plants today for them so the streak holds. One swap.",
                            fontFamily = dmSansFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showBuddyNudge,
                    enter = slideInVertically { -it } + fadeIn(tween(380)),
                    exit = slideOutVertically { -it } + fadeOut(),
                ) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${state.buddyUsername} just finished. Don't break the streak!",
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
                        "Streak at risk. Finish all habits before midnight.",
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
                            Text("Cover yesterday", fontFamily = dmSansFamily, color = Gold, fontSize = 12.sp)
                        }
                    } else if (state.graceUsedThisWeek) {
                        Text(
                            "Premium shield used this week",
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
                            photoUrl = myPhotoUrl,
                            userId = FirebaseAuth.getInstance().currentUser?.uid,
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
                            userId = state.buddyUid,
                            accent = accent,
                            pulse = if (state.buddyDoneToday && !state.myDoneToday) pulse else 1f,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (state.status == "pending") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Waiting for ${state.buddyUsername} to accept…",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF95D5B2),
                        )
                    }
                }
            }
        }
    }

    if (showCoverHandshake) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCoverHandshake = false }) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    com.saintnico.verdlyhabits.ui.components.VerdlyHandshakeBeat(
                        size = 150.dp,
                        play = true,
                        onFinished = {
                            showCoverHandshake = false
                            coverPhase = com.saintnico.verdlyhabits.ui.components.VerdlyActionPhase.Idle
                        },
                    )
                    Text(
                        "Day covered",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DuoRootsHero(
    bothLocked: Boolean,
    atRisk: Boolean,
    streakDays: Int,
    streakScale: Float,
    onNudge: (() -> Unit)?,
    buddyNudged: Boolean,
) {
    val streakComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.streak))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1440f / 905f)
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
    ) {
        AnimatedContent(
            targetState = bothLocked,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(520, easing = FastOutSlowInEasing)) + scaleIn(
                    initialScale = 1.04f,
                    animationSpec = tween(520, easing = FastOutSlowInEasing),
                )).togetherWith(
                    fadeOut(tween(380)) + scaleOut(targetScale = 0.98f, animationSpec = tween(380)),
                )
            },
            label = "duo_roots",
        ) { locked ->
            androidx.compose.foundation.Image(
                painter = painterResource(
                    if (locked) R.drawable.duo_roots_locked else R.drawable.duo_roots_waiting,
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color(0xCC0F1A14)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flagship Lottie fire lives only here (duo card), not in StreakFlame elsewhere.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                LottieAnimation(
                    composition = streakComp,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (bothLocked) "Both locked in" else if (atRisk) "Streak at risk" else "Waiting on the pair",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Text(
                    "${DuoStreakEngine.tierLabel(streakDays)} · ${streakDays}d",
                    fontFamily = dmSansFamily,
                    fontSize = 11.sp,
                    color = if (bothLocked) Gold else Color.White.copy(0.75f),
                    modifier = Modifier.scale(streakScale),
                )
            }
            // Hide nudge entirely after a successful send (no "Nudged" stub).
            if (onNudge != null && !buddyNudged) {
                IconButton(onClick = onNudge, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Bolt, contentDescription = "Nudge", tint = Gold, modifier = Modifier.size(18.dp))
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
    userId: String?,
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
            modifier = Modifier.size((44 * pulse).dp),
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.12f))
                    .border(1.dp, if (done) accent else accent.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                com.saintnico.verdlyhabits.ui.components.ProfileAvatar(
                    photoUri = photoUrl,
                    size = 34.dp,
                    userId = userId,
                    fallbackTint = accent,
                    fallbackBackground = accent.copy(0.12f),
                )
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
