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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }
    
    val data = uiState!!
    val totalSlides = 6
    var currentSlide by rememberSaveable { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width / 3f) {
                            if (currentSlide > 0) currentSlide--
                        } else {
                            if (currentSlide < totalSlides - 1) currentSlide++ else onClose()
                        }
                    }
                )
            }
    ) {
        // Slide Content
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "slide_transition"
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

        // Progress Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until totalSlides) {
                StoryProgressBar(
                    modifier = Modifier.weight(1f),
                    isActive = i == currentSlide,
                    isCompleted = i < currentSlide,
                    isPaused = isPaused,
                    onComplete = {
                        if (i == currentSlide) {
                            if (currentSlide < totalSlides - 1) currentSlide++ else onClose()
                        }
                    }
                )
            }
        }
        
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 8.dp)
        ) {
            Icon(Icons.Rounded.Close, "Close", tint = Color.White)
        }
    }
}

@Composable
fun StoryProgressBar(
    modifier: Modifier,
    isActive: Boolean,
    isCompleted: Boolean,
    isPaused: Boolean,
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(isActive, isPaused) {
        if (isActive) {
            if (!isPaused) {
                val remainingTime = (6000 * (1f - progress.value)).toLong()
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingTime.toInt(), easing = LinearEasing)
                )
                onComplete()
            }
        } else {
            progress.snapTo(if (isCompleted) 1f else 0f)
        }
    }

    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(Color.White)
        )
    }
}

@Composable
private fun SlideOpener() {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
    Box(modifier = Modifier.fillMaxSize().background(gradient), contentAlignment = Alignment.Center) {
        Text(
            text = "Your Habit\nJourney",
            fontFamily = frauncesFamily,
            fontSize = 48.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            lineHeight = 54.sp
        )
    }
}

@Composable
private fun SlideConsistency(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF200122), Color(0xFF6f0000)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("CONSISTENCY", fontSize = 14.sp, color = Color.White.copy(0.7f), letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))
        Text("${data.totalCompletions}", fontFamily = frauncesFamily, fontSize = 80.sp, color = Color.White)
        Text("total check-ins", fontSize = 24.sp, color = Color.White.copy(0.9f))
        Spacer(Modifier.height(48.dp))
        Text("Across ${data.totalActiveDays} days of building momentum.", fontSize = 18.sp, color = Color.White.copy(0.8f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SlideTrend(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("THIS WEEK", fontSize = 14.sp, color = Color.White.copy(0.7f), letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))
        
        Text("${data.thisWeekCompletions}", fontFamily = frauncesFamily, fontSize = 80.sp, color = Color(0xFFE0E1DD))
        Text("check-ins", fontSize = 24.sp, color = Color.White.copy(0.9f))
        
        Spacer(Modifier.height(48.dp))
        
        val trendText = when (data.weeklyTrend) {
            WeeklyTrend.UP -> "Up ${data.trendPercent}% from last week! \uD83D\uDE80"
            WeeklyTrend.DOWN -> "Rest is part of the process. \uD83C\uDF43"
            WeeklyTrend.NEUTRAL -> "Holding steady, building discipline. \u2696\uFE0F"
        }
        
        Text(trendText, fontSize = 20.sp, color = Color.White.copy(0.8f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text("Best day: ${data.bestDayOfWeek}", fontSize = 16.sp, color = Color.White.copy(0.6f))
    }
}

@Composable
private fun SlideTopHabit(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0A1610), Color(0xFF1B4332)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("YOUR CHAMPION", fontSize = 14.sp, color = Color.White.copy(0.7f), letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))
        Text(data.bestHabitName, fontFamily = frauncesFamily, fontSize = 42.sp, color = Color(0xFFD8F3DC), textAlign = TextAlign.Center, lineHeight = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Longest streak: ${data.bestConsecutiveStreak} days", fontSize = 20.sp, color = Color.White.copy(0.8f))
        Spacer(Modifier.height(64.dp))
        
        Box(modifier = Modifier.size(200.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawGrowingPlant(1f, false, 0f)
            }
        }
    }
}

@Composable
private fun SlidePersona(data: WrappedSnapshot) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF0B0914), Color(0xFF231A45)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("YOUR PERSONA", fontSize = 14.sp, color = Color.White.copy(0.7f), letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))
        Text(data.personaName, fontFamily = frauncesFamily, fontSize = 42.sp, color = Color(0xFFE2D6FF), textAlign = TextAlign.Center, lineHeight = 48.sp)
        Spacer(Modifier.height(24.dp))
        Text(data.personaDescription, fontSize = 20.sp, color = Color.White.copy(0.8f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SlideFinale(data: WrappedSnapshot, onShare: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF1A1505), Color(0xFF42350A)))
    Column(
        modifier = Modifier.fillMaxSize().background(gradient).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("THAT'S A WRAP", fontSize = 14.sp, color = Color.White.copy(0.7f), letterSpacing = 2.sp)
        Spacer(Modifier.height(32.dp))
        Text("Keep growing.", fontFamily = frauncesFamily, fontSize = 48.sp, color = Color(0xFFFFD700), fontStyle = FontStyle.Italic)
        Spacer(Modifier.height(12.dp))
        Text(data.personaName, fontSize = 18.sp, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(64.dp))
        
        Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share Wrapped Story", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
