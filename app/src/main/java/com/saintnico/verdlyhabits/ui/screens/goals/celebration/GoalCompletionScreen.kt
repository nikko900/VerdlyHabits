package com.saintnico.verdlyhabits.ui.screens.goals.celebration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.ui.viewmodel.GoalCompletionStats
import com.saintnico.verdlyhabits.ui.components.share.shareGoalCompletionCard
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun GoalCompletionScreen(
    goal: GoalEntity,
    stats: GoalCompletionStats? = null,
    onArchive: () -> Unit,
    onSetNewGoal: () -> Unit,
    onShare: () -> Unit = {},
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    val color = Color(android.graphics.Color.parseColor(goal.colorHex))
    val timeTaken = stats?.daysTaken
        ?: TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - goal.startDate)
    val completionCheer = remember(goal.id) {
        com.saintnico.verdlyhabits.engine.GoalCelebrationCopy.milestoneCheer(
            goal,
            com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity(
                id = "complete",
                goalId = goal.id,
                title = "Finish line",
                targetPercent = 100f,
                completedDate = System.currentTimeMillis(),
                isCompleted = true,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = color, modifier = Modifier.size(64.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            if (goal.goalType == com.saintnico.verdlyhabits.data.local.goals.GoalType.REACH)
                "You reached it"
            else
                "Goal Completed",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            goal.title,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 40.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 48.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "\"${goal.whyStatement}\"",
            fontFamily = frauncesFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            completionCheer,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = color,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Time Taken", "$timeTaken days", Modifier.weight(1f))
            StatCard(
                "Milestones",
                stats?.let { "${it.milestonesCompleted} / ${it.milestonesTotal}" } ?: ".",
                Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                "Habit reps",
                stats?.habitsCompleted?.toString() ?: ".",
                Modifier.weight(1f),
            )
            StatCard(
                "Check-ins",
                stats?.checkInCount?.toString() ?: ".",
                Modifier.weight(1f),
            )
        }
        stats?.costSavedLabel?.let { saved ->
            Spacer(modifier = Modifier.height(16.dp))
            StatCard("Reclaimed", saved, Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSetNewGoal,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set a New Goal", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                if (stats != null) {
                    shareGoalCompletionCard(context = context, goal = goal, stats = stats)
                } else {
                    shareGoalCompletionCard(
                        context = context,
                        goalTitle = goal.title,
                        whyStatement = goal.whyStatement,
                        daysTaken = timeTaken,
                    )
                }
                onShare()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share your win", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onArchive,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Rounded.Archive, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Archive Goal", fontFamily = dmSansFamily, fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
