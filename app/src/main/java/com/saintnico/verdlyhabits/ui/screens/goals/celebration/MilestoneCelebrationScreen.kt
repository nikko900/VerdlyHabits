package com.saintnico.verdlyhabits.ui.screens.goals.celebration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun MilestoneCelebrationScreen(
    goal: GoalEntity,
    milestone: MilestoneEntity,
    metrics: GoalProgressEngine.GoalMetrics? = null,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val copy = remember(goal.id, milestone.id) {
        GoalCelebrationCopy.milestoneMoment(goal, milestone, metrics)
    }
    val cheer = remember(goal.id, milestone.id) {
        GoalCelebrationCopy.milestoneCheer(goal, milestone)
    }
    val palette = GoalCelebrationCopy.palette(goal.goalType)
    val color = Color(palette.primary)
    val secondary = Color(palette.secondary)

    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "milestone_scale",
    )

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.15f),
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
                        maxSpeed = 34f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(
                            (palette.primary and 0xFFFFFFFFL).toInt(),
                            (palette.secondary and 0xFFFFFFFFL).toInt(),
                            0xFFFFFFFF.toInt(),
                        ),
                        emitter = Emitter(duration = 120, TimeUnit.MILLISECONDS).max(90),
                        position = Position.Relative(0.5, 0.3),
                    ),
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.scale(scale),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(secondary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(copy.emoji, fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    copy.badge,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = color,
                    letterSpacing = 2.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    copy.headline,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 40.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    milestone.title,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = secondary,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (goal.whyStatement.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "\"${goal.whyStatement}\"",
                                fontFamily = frauncesFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            cheer,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = color,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Milestone", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    "Keep going",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
