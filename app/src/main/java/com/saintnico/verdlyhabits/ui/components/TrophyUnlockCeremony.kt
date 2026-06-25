package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.haptics.HapticsEngine
import com.saintnico.verdlyhabits.audio.SoundEngine
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.TrophyCatalog
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun TrophyUnlockCeremony(
    achievement: Achievement,
    haptics: HapticsEngine?,
    sound: SoundEngine?,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val def = TrophyCatalog.find(achievement.id)
    val visualTier = if (def != null) tierLevelToVisualTier(def.tier) else TrophyTier.BRONZE

    var startAnimation by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sound?.playChime(SoundEngine.Chime.MILESTONE_100, true)
        // Custom haptic: 3 escalating pulses
        delay(100)
        haptics?.success(true)
        delay(150)
        haptics?.success(true)
        delay(200)
        haptics?.celebration(true)
        
        startAnimation = true
        delay(300)
        showText = true
        delay(200)
        showSubtitle = true
        delay(1500)
        showShare = true
    }

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f, // Bouncy
            stiffness = Spring.StiffnessLow
        ),
        label = "trophy_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)) // 90% black
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Particle Explosion
        if (startAnimation) {
            ParticleExplosion(color = GoldColor)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // "UNLOCKED" Header
            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -20 })
            ) {
                Text(
                    "TROPHY UNLOCKED",
                    color = GoldColor,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }

            // Trophy (Scaling in)
            Box(
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                AchievementTrophy(
                    icon = achievement.icon,
                    tier = visualTier,
                    unlocked = true,
                    size = 180.dp
                )
            }

            Spacer(Modifier.height(48.dp))

            // Trophy Name
            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 20 })
            ) {
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            // Trophy Description
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 20 })
            ) {
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(60.dp))

            // Share Button
            AnimatedVisibility(
                visible = showShare,
                enter = fadeIn(tween(800))
            ) {
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldColor.copy(alpha = 0.2f),
                        contentColor = GoldColor
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Milestone", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            AnimatedVisibility(
                visible = showShare,
                enter = fadeIn(tween(800))
            ) {
                Text(
                    "Tap anywhere to continue",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ParticleExplosion(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_progress"
    )

    val particles = remember {
        List(40) {
            val angle = Random.nextFloat() * 2 * Math.PI
            val speed = Random.nextFloat() * 150f + 50f
            val size = Random.nextFloat() * 4f + 2f
            Particle(angle.toFloat(), speed, size)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        particles.forEach { p ->
            // Particles slow down as they move outwards
            val currentDistance = p.speed * progress * (2f - progress)
            val x = cx + cos(p.angle) * currentDistance
            val y = cy + sin(p.angle) * currentDistance
            
            // Fade out towards the end
            val alpha = (1f - progress).coerceIn(0f, 1f)
            
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}

private data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float
)
