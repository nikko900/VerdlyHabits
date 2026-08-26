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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saintnico.verdlyhabits.engine.WrappedSnapshot
import com.saintnico.verdlyhabits.engine.WeeklyTrend
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.screens.focus.drawGrowingPlant
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.utils.StreakCardExporter
import com.saintnico.verdlyhabits.utils.StreakCardTheme
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WrappedStoryScreen(
    onClose: () -> Unit,
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
    val totalSlides = 6
    var currentSlide by rememberSaveable { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

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
            when (slide) {
                0 -> SlideOpener()
                1 -> SlideConsistency(data)
                2 -> SlideTrend(data)
                3 -> SlideTopHabit(data)
                4 -> SlidePersona(data)
                5 -> {
                    SlideFinale(data, onShare = {
                        coroutineScope.launch {
                            try {
                                val code = ReferralManager.getReferralCode(context).removePrefix("VERDLY-").uppercase()
                                StreakCardExporter.exportAndShare(
                                    context = context,
                                    headline = data.personaName,
                                    habitName = "${data.totalCompletions} Habits Built",
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
                text = "VERDLY\nWRAPPED",
                fontSize = 14.sp,
                color = Color(0xFF9EC9FF),
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Your Habit\nJourney",
                fontFamily = frauncesFamily,
                fontSize = 52.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 56.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Built from your real check-ins, streaks, and weekly rhythm.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(28.dp))
            FrostedBadge(text = "Tap right to continue")
        }
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
private fun SlideConsistency(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF1A0D1F), Color(0xFF2E1634), Color(0xFF3A1532)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SlideHeader("CONSISTENCY", "How strong your week actually was")
        Spacer(Modifier.height(22.dp))
        StatRing(rate = data.weekCompletionRate)
        Spacer(Modifier.height(16.dp))
        Text(
            "${data.totalCompletions} total check-ins",
            fontFamily = frauncesFamily,
            fontSize = 34.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Across ${data.totalActiveDays} active days, you finished ${data.weekCompletionRate}% of scheduled habits this week.",
            fontSize = 16.sp,
            color = Color.White.copy(0.82f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
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
        SlideHeader("WEEK PULSE", "Scheduled vs completed by day")
        Spacer(Modifier.height(20.dp))
        WeeklyBars(data)
        Spacer(Modifier.height(20.dp))
        Text("${data.thisWeekCompletions}", fontFamily = frauncesFamily, fontSize = 62.sp, color = Color(0xFFE0E1DD))
        Text("completed this week", fontSize = 19.sp, color = Color.White.copy(0.9f))
        Spacer(Modifier.height(16.dp))
        val trendText = when (data.weeklyTrend) {
            WeeklyTrend.UP -> "Up ${data.trendPercent}% from last week! \uD83D\uDE80"
            WeeklyTrend.DOWN -> "A slower week, still part of the process. \uD83C\uDF43"
            WeeklyTrend.NEUTRAL -> "Steady cadence. Discipline is compounding. \u2696\uFE0F"
        }
        Text(trendText, fontSize = 18.sp, color = Color.White.copy(0.84f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Best day: ${data.bestDayOfWeek}", fontSize = 16.sp, color = Color.White.copy(0.6f))
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
        SlideHeader("TOP HABIT", "Your most resilient ritual")
        Spacer(Modifier.height(24.dp))
        Text(data.bestHabitName, fontFamily = frauncesFamily, fontSize = 40.sp, color = Color(0xFFD8F3DC), textAlign = TextAlign.Center, lineHeight = 46.sp)
        Spacer(Modifier.height(14.dp))
        FrostedBadge(text = "${data.bestHabitThisWeekCompletions} completions this week")
        Spacer(Modifier.height(10.dp))
        Text("Longest streak: ${data.bestConsecutiveStreak} days", fontSize = 20.sp, color = Color.White.copy(0.85f))
        Spacer(Modifier.height(38.dp))
        Box(modifier = Modifier.size(200.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawGrowingPlant(1f, false, 0f)
            }
        }
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
        Spacer(Modifier.height(28.dp))
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
        SlideHeader("THAT'S A WRAP", "Make your streak visible")
        Spacer(Modifier.height(28.dp))
        Text("Keep growing.", fontFamily = frauncesFamily, fontSize = 48.sp, color = Color(0xFFFFD700), fontStyle = FontStyle.Italic)
        Spacer(Modifier.height(12.dp))
        Text("${data.personaName} · ${data.weekCompletionRate}% weekly hit-rate", fontSize = 18.sp, color = Color.White.copy(0.75f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(42.dp))
        
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
