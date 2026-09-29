package com.saintnico.verdlyhabits.ui.screens.goals.celebration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.engine.GoalCelebrationCopy
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/** Full-screen ritual for midpoint and other pivotal goal moments. */
@Composable
fun GoalMomentRitualScreen(
    goal: GoalEntity,
    milestone: MilestoneEntity,
    metrics: GoalProgressEngine.GoalMetrics?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val copy = remember(goal.id, milestone.id) {
        GoalCelebrationCopy.milestoneMoment(goal, milestone, metrics)
    }
    val palette = GoalCelebrationCopy.palette(goal.goalType)
    val primary = Color(palette.primary)
    val secondary = Color(palette.secondary)
    val tertiary = Color(palette.tertiary)

    var startAnimation by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "ritual_scale",
    )
    val pulse = rememberInfiniteTransition(label = "ritual_pulse")
    val emojiScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emoji_pulse",
    )

    LaunchedEffect(Unit) {
        delay(80)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.22f),
                        secondary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        if (startAnimation) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 42f,
                        damping = 0.88f,
                        spread = 360,
                        colors = listOf(
                            (palette.primary and 0xFFFFFFFFL).toInt(),
                            (palette.secondary and 0xFFFFFFFFL).toInt(),
                            (palette.tertiary and 0xFFFFFFFFL).toInt(),
                            0xFFFFFFFF.toInt(),
                        ),
                        emitter = Emitter(duration = 220, TimeUnit.MILLISECONDS).max(140),
                        position = Position.Relative(0.5, 0.22),
                    ),
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                copy.badge,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.4.sp,
                color = primary,
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(112.dp)
                    .scale(emojiScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tertiary.copy(alpha = 0.55f),
                                primary.copy(alpha = 0.15f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(copy.emoji, fontSize = 52.sp)
            }

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier.scale(cardScale),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    copy.headline,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    copy.subhead,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = secondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            copy.body,
                            fontFamily = dmSansFamily,
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        if (goal.whyStatement.isNotBlank()) {
                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(primary.copy(alpha = 0.1f))
                                    .padding(16.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Your why",
                                        fontFamily = dmSansFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = primary,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "\"${goal.whyStatement}\"",
                                        fontFamily = frauncesFamily,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            copy.cheer,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(
                    "Keep going",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share this moment",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
