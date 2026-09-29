package com.saintnico.verdlyhabits.ui.screens.stats

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.engine.WrappedSnapshot
import com.saintnico.verdlyhabits.engine.WeeklyTrend
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.components.share.ShareableStreakCard
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.utils.StreakCardExporter
import com.saintnico.verdlyhabits.utils.StreakCardTheme
import kotlinx.coroutines.launch
import kotlin.math.max

private enum class WrappedSlideKind {
    Opener,
    Streak,
    Pulse,
    TopHabit,
    CheckIns,
    Social,
    Persona,
    Finale,
}

private fun buildSlides(includeSocial: Boolean): List<WrappedSlideKind> = buildList {
    add(WrappedSlideKind.Opener)
    add(WrappedSlideKind.Streak)
    add(WrappedSlideKind.Pulse)
    add(WrappedSlideKind.TopHabit)
    add(WrappedSlideKind.CheckIns)
    if (includeSocial) add(WrappedSlideKind.Social)
    add(WrappedSlideKind.Persona)
    add(WrappedSlideKind.Finale)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WrappedStoryScreen(
    onClose: () -> Unit,
    socialWeekActive: Boolean = false,
    viewModel: WrappedStoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (uiState == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Loading your story…", color = Color.White.copy(0.7f))
        }
        return
    }

    val data = uiState!!
    val slides = remember(socialWeekActive) { buildSlides(socialWeekActive) }
    val totalSlides = slides.size
    var currentSlide by rememberSaveable(socialWeekActive) { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(totalSlides) {
        if (currentSlide >= totalSlides) currentSlide = (totalSlides - 1).coerceAtLeast(0)
    }

    fun goPrev() {
        if (currentSlide > 0) currentSlide -= 1
    }

    fun goNext() {
        if (currentSlide < totalSlides - 1) currentSlide += 1 else onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
            label = "slide_transition",
            modifier = Modifier.fillMaxSize(),
        ) { slide ->
            when (slides.getOrElse(slide) { WrappedSlideKind.Finale }) {
                WrappedSlideKind.Opener -> SlideOpener()
                WrappedSlideKind.Streak -> SlideStreakHighlight(data)
                WrappedSlideKind.Pulse -> SlideTrend(data)
                WrappedSlideKind.TopHabit -> SlideTopHabit(data)
                WrappedSlideKind.CheckIns -> SlideCheckIns(data)
                WrappedSlideKind.Social -> SlideSocialWeek()
                WrappedSlideKind.Persona -> SlidePersona(data)
                WrappedSlideKind.Finale -> {
                    SlideFinale(data, onShare = {
                        coroutineScope.launch {
                            try {
                                val code = ReferralManager.getReferralCode(context).removePrefix("VERDLY-").uppercase()
                                StreakCardExporter.exportAndShare(
                                    context = context,
                                    headline = data.personaName,
                                    habitName = "${data.totalCompletions} check-ins this season",
                                    referralCode = code,
                                    theme = StreakCardTheme.MidnightViolet
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Couldn't share. Check permissions.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                    ConfettiOverlay()
                }
            }
        }

        // Left / right navigation zones (leave bottom clear for share CTA on last slide)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentSlide == totalSlides - 1) 120.dp else 0.dp)
                .pointerInput(currentSlide) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { goPrev() },
                    ),
            )
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { goNext() },
                    ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (i in 0 until totalSlides) {
                StoryProgressBar(
                    modifier = Modifier.weight(1f),
                    isActive = i == currentSlide,
                    isCompleted = i < currentSlide,
                    isPaused = isPaused,
                    onComplete = {
                        if (i == currentSlide) goNext()
                    },
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 8.dp),
        ) {
            Icon(Icons.Rounded.Close, "Close", tint = Color.White)
        }

        // Side chevrons so previous/next are always reachable without blocking share CTA
        if (currentSlide > 0) {
            IconButton(
                onClick = { goPrev() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.16f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = Color.White,
                )
            }
        }
        if (currentSlide < totalSlides - 1) {
            IconButton(
                onClick = { goNext() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.16f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
fun StoryProgressBar(
    modifier: Modifier,
    isActive: Boolean,
    isCompleted: Boolean,
    isPaused: Boolean,
    onComplete: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var wasActive by remember { mutableStateOf(false) }

    LaunchedEffect(isActive, isCompleted, isPaused) {
        if (!isActive) {
            progress.snapTo(if (isCompleted) 1f else 0f)
            wasActive = false
            return@LaunchedEffect
        }
        if (!wasActive) {
            progress.snapTo(0f)
            wasActive = true
        }
        if (isPaused) return@LaunchedEffect
        val remainingMs = (6000f * (1f - progress.value)).toInt().coerceAtLeast(1)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = remainingMs, easing = LinearEasing),
        )
        onComplete()
    }

    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(Color.White),
        )
    }
}

@Composable
private fun SlideOpener() {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF050814), Color(0xFF111B36), Color(0xFF15103A)))
    Box(
        modifier = Modifier.fillMaxSize().background(gradient).padding(horizontal = 22.dp, vertical = 48.dp),
    ) {
        AuroraOrbs()
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VERDLY",
                fontSize = 12.sp,
                color = Color(0xFF9EC9FF),
                letterSpacing = 4.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(VerdlyAssetCatalog.IllPeopleFloat),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(168.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your week",
                fontFamily = frauncesFamily,
                fontSize = 56.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 58.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Seven days of real check-ins, distilled.",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun SlideStreakHighlight(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF1A0A04), Color(0xFF3D1A0A), Color(0xFF2A1208)))
    val streakComp by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieStreak))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SlideHeader("STREAK", "The flame you kept alive")
        Spacer(Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            LottieAnimation(
                composition = streakComp,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(180.dp),
            )
            StreakFlame(
                streak = data.bestConsecutiveStreak.coerceAtLeast(1),
                size = 72.dp,
                showLabel = true,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "${data.bestConsecutiveStreak}",
            fontFamily = frauncesFamily,
            fontSize = 72.sp,
            color = Color(0xFFFFE082),
            fontWeight = FontWeight.Bold,
        )
        Text(
            "day streak on ${data.bestHabitName}",
            fontSize = 17.sp,
            color = Color.White.copy(0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SlideSocialWeek() {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0F1A24), Color(0xFF1B3550), Color(0xFF243B55)))
    val chat by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieAvatarsChatting))
    val handshake by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieHandshakeLoop))
    val stickerRing = listOf(
        VerdlyAssetCatalog.RxnHeart,
        VerdlyAssetCatalog.RxnAwesome,
        VerdlyAssetCatalog.RxnWow,
        VerdlyAssetCatalog.RxnYass,
        VerdlyAssetCatalog.RxnHeartEyes,
        VerdlyAssetCatalog.RxnThanks,
    )
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SlideHeader("GROVE", "You did not grow alone")
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(VerdlyAssetCatalog.IllPeopleParty),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(200.dp),
                contentScale = ContentScale.Fit,
            )
            stickerRing.forEachIndexed { index, res ->
                val angle = (index / stickerRing.size.toFloat()) * 360f - 90f
                val rad = Math.toRadians(angle.toDouble())
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (Math.cos(rad) * 118).dp,
                            y = (Math.sin(rad) * 98).dp,
                        )
                        .size(36.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LottieAnimation(composition = chat, iterations = LottieConstants.IterateForever, modifier = Modifier.size(56.dp))
            LottieAnimation(composition = handshake, iterations = LottieConstants.IterateForever, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Friends showed up in your grove or arena this week.",
            fontSize = 18.sp,
            color = Color.White.copy(0.88f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(12.dp))
        FrostedBadge(text = "Witness. Nudge. Keep the circle honest.")
    }
}

@Composable
private fun AuroraOrbs() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xAA60A5FA), Color.Transparent)),
            radius = size.minDimension * 0.42f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.22f),
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0x887B61FF), Color.Transparent)),
            radius = size.minDimension * 0.48f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.80f, size.height * 0.62f),
        )
    }
}

@Composable
private fun SlideTrend(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF071423), Color(0xFF102640), Color(0xFF1B3556)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlideHeader("BEST DAY", "When your rhythm peaked")
        Spacer(Modifier.height(12.dp))
        val scale by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieScaleRetro))
        LottieAnimation(composition = scale, iterations = LottieConstants.IterateForever, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(8.dp))
        WeeklyBars(data)
        Spacer(Modifier.height(16.dp))
        StatRing(rate = data.weekCompletionRate)
        Spacer(Modifier.height(14.dp))
        Text(
            "Best day: ${data.bestDayOfWeek}",
            fontFamily = frauncesFamily,
            fontSize = 28.sp,
            color = Color(0xFFE0E1DD),
            textAlign = TextAlign.Center,
        )
        Text("${data.thisWeekCompletions} check-ins this week", fontSize = 16.sp, color = Color.White.copy(0.78f))
        Spacer(Modifier.height(12.dp))
        val trendText = when (data.weeklyTrend) {
            WeeklyTrend.UP -> "Up ${data.trendPercent}% from last week. Momentum is real."
            WeeklyTrend.DOWN -> "A quieter week. Roots still run deep."
            WeeklyTrend.NEUTRAL -> "Steady cadence. Discipline compounding."
        }
        Text(trendText, fontSize = 16.sp, color = Color.White.copy(0.84f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SlideTopHabit(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0A1B14), Color(0xFF153829), Color(0xFF1F4A37)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlideHeader("TOP HABIT", "Your most consistent ritual")
        Spacer(Modifier.height(16.dp))
        Text(data.bestHabitName, fontFamily = frauncesFamily, fontSize = 38.sp, color = Color(0xFFD8F3DC), textAlign = TextAlign.Center, lineHeight = 44.sp)
        Spacer(Modifier.height(10.dp))
        FrostedBadge(text = "${data.bestHabitThisWeekCompletions} times this week")
        Spacer(Modifier.height(16.dp))
        val plantComp by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieGrowingPlant))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(VerdlyAssetCatalog.IllPlantsCitti),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
                contentScale = ContentScale.Fit,
            )
            LottieAnimation(
                composition = plantComp,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp),
            )
        }
    }
}

@Composable
private fun SlideCheckIns(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF140A1C), Color(0xFF2A1640), Color(0xFF1A0F28)))
    val pushups by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottiePersonPushups))
    val cycling by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottiePersonCycling))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SlideHeader("CHECK-INS", "The reps you actually did")
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LottieAnimation(composition = pushups, iterations = LottieConstants.IterateForever, modifier = Modifier.size(110.dp))
            LottieAnimation(composition = cycling, iterations = LottieConstants.IterateForever, modifier = Modifier.size(110.dp))
        }
        Text(
            "${data.thisWeekCompletions}",
            fontFamily = frauncesFamily,
            fontSize = 72.sp,
            color = Color(0xFFF3D9FF),
            fontWeight = FontWeight.Bold,
        )
        Text(
            "plants this week · ${data.totalCompletions} all-time",
            fontSize = 16.sp,
            color = Color.White.copy(0.82f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Image(
            painter = painterResource(VerdlyAssetCatalog.IllPeopleReview),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(120.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun SlidePersona(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0C0A18), Color(0xFF251B46), Color(0xFF35205C)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlideHeader("YOUR PERSONA", "Identity forged by behavior")
        Spacer(Modifier.height(12.dp))
        val star by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieStarDynamic))
        val medal by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieMedalFirst))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            LottieAnimation(composition = medal, iterations = LottieConstants.IterateForever, modifier = Modifier.size(72.dp))
            LottieAnimation(composition = star, iterations = LottieConstants.IterateForever, modifier = Modifier.size(72.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(data.personaName, fontFamily = frauncesFamily, fontSize = 46.sp, color = Color(0xFFE2D6FF), textAlign = TextAlign.Center, lineHeight = 50.sp)
        Spacer(Modifier.height(24.dp))
        Text(data.personaDescription, fontSize = 20.sp, color = Color.White.copy(0.84f), textAlign = TextAlign.Center, lineHeight = 27.sp)
        Spacer(Modifier.height(20.dp))
        FrostedBadge(text = "Based on ${data.totalCompletions} real completions")
    }
}

@Composable
private fun SlideFinale(data: WrappedSnapshot, onShare: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF1A1505), Color(0xFF3A2D0A), Color(0xFF5A4310)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlideHeader("YOUR MOMENT", "Shareable, like a streak card")
        Spacer(Modifier.height(8.dp))
        val applause by rememberLottieComposition(LottieCompositionSpec.RawRes(VerdlyAssetCatalog.LottieHandsApplause))
        LottieAnimation(composition = applause, iterations = LottieConstants.IterateForever, modifier = Modifier.size(88.dp))
        Spacer(Modifier.height(8.dp))
        ShareableStreakCard(
            headline = data.personaName,
            habitName = "${data.weekCompletionRate}% weekly hit-rate",
            referralCode = "",
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Keep the story visible.",
            fontFamily = frauncesFamily,
            fontSize = 20.sp,
            color = Color(0xFFFFD700),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD866), contentColor = Color(0xFF241A00)),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share Wrapped Story", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SlideHeader(title: String, subtitle: String) {
    Text(title, fontSize = 13.sp, color = Color.White.copy(0.72f), letterSpacing = 2.2.sp)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, fontSize = 16.sp, color = Color.White.copy(0.88f), textAlign = TextAlign.Center)
}

@Composable
private fun FrostedBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White.copy(0.12f))
            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(100.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, color = Color.White.copy(0.92f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatRing(rate: Int) {
    val accent = when {
        rate >= 80 -> Color(0xFF52E3A4)
        rate >= 55 -> Color(0xFFFFC766)
        else -> Color(0xFFFF8A80)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White.copy(alpha = 0.12f), style = Stroke(width = 18.dp.toPx()))
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * (rate.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(width = 18.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${rate.coerceIn(0, 100)}%", fontFamily = frauncesFamily, fontSize = 46.sp, color = Color.White)
            Text("Weekly hit-rate", fontSize = 14.sp, color = Color.White.copy(0.78f))
        }
    }
}

@Composable
private fun WeeklyBars(data: WrappedSnapshot) {
    val maxScheduled = max(1, data.weekDayStats.maxOfOrNull { it.scheduled } ?: 1)
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.weekDayStats.forEach { day ->
            val denom = day.scheduled.coerceAtLeast(1)
            val completionRatio = (day.completed.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
            val scheduledHeight = (day.scheduled.toFloat() / maxScheduled.toFloat()).coerceIn(0f, 1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((125f * scheduledHeight).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.2f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(completionRatio)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFF7BE8FF), Color(0xFF4CC9A6))
                                )
                            )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(day.label, fontSize = 11.sp, color = Color.White.copy(0.82f))
            }
        }
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val rot: Float,
    val rotSpeed: Float,
    val color: Color,
)

@Composable
fun ConfettiOverlay() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(3000, easing = LinearEasing))
    }
    val palette = listOf(
        Color(0xFF52B788),
        Color(0xFFFFB300),
        Color(0xFF7B6CF6),
        Color(0xFFFF6B6B),
        Color.White,
    )
    val particles = remember {
        List(80) {
            ConfettiParticle(
                startX = (0..100).random() / 100f,
                speedX = (-50..50).random() / 100f,
                speedY = (30..80).random() / 10f,
                size = (15..30).random() / 10f,
                rot = (0..360).random().toFloat(),
                rotSpeed = (-20..20).random() / 5f,
                color = palette.random(),
            )
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val startX = p.startX * w
            val sx = p.speedX * w * 0.2f
            val sy = p.speedY * h * 0.5f
            val pSize = p.size * 12.dp.toPx()

            val currX = startX + sx * t + kotlin.math.sin(t * 10f + p.startX) * 50f
            val currY = -20f + sy * t * t * 2f
            val rotation = p.rot + p.rotSpeed * t * 360f

            if (currY < h + 100f && t < 0.95f) {
                drawContext.canvas.save()
                drawContext.canvas.translate(currX, currY)
                drawContext.canvas.rotate(rotation)
                drawRect(
                    color = p.color.copy(alpha = 1f - t * t),
                    size = androidx.compose.ui.geometry.Size(pSize, pSize * 0.6f),
                )
                drawContext.canvas.restore()
            }
        }
    }
}
