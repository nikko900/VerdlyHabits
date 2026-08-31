package com.saintnico.verdlyhabits.ui.screens.goals.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    viewModel: GoalsViewModel,
    habits: List<HabitItem>,
    onBack: () -> Unit,
) {
    val goal by viewModel.goalById(goalId).collectAsState(initial = null)
    val milestones by viewModel.milestonesForGoal(goalId).collectAsState(initial = emptyList())
    var showLapseConfirm by remember { mutableStateOf(false) }
    var showSos by remember { mutableStateOf(false) }
    var showPromote by remember { mutableStateOf(false) }
    var showEndGoal by remember { mutableStateOf(false) }
    var endReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            val barColor = goal?.let { Color(android.graphics.Color.parseColor(it.colorHex)) }
                ?: MaterialTheme.colorScheme.primary
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barColor.copy(alpha = 0.1f),
                    navigationIconContentColor = barColor,
                ),
            )
        },
    ) { padding ->
        val g = goal
        if (g == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val color = Color(android.graphics.Color.parseColor(g.colorHex))
        val metrics = remember(g, habits) { viewModel.metricsFor(g, habits) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            TypeHeroHeader(goal = g, metrics = metrics, color = color)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
            ) {
                Text(
                    metrics.narrative,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            TypeMetaChips(goal = g, metrics = metrics, color = color)

            if (g.goalType == GoalType.QUIT) {
                QuitActions(
                    color = color,
                    onSos = { showSos = true },
                    onLogMoment = { showLapseConfirm = true },
                )
            }

            if (g.goalType == GoalType.BUILD && metrics.buildStage >= 4) {
                OutlinedButton(
                    onClick = { showPromote = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "This feels automatic — move to Maintain",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (g.goalType == GoalType.REACH && metrics.canAutoComplete) {
                OutlinedButton(
                    onClick = { showPromote = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "Turn this into a lasting standard",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                when (g.goalType) {
                    GoalType.QUIT -> "Clear milestones"
                    GoalType.MAINTAIN -> "Held milestones"
                    GoalType.BUILD -> "Growth stages"
                    GoalType.REACH -> "Trail markers"
                },
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            MilestoneStepper(milestones, color)

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = { showEndGoal = true },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    "Need to step back from this goal?",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showLapseConfirm && goal != null) {
        LapseConfirmDialog(
            longest = GoalProgressEngine.metrics(goal!!, habits).longestCleanStreakDays,
            onDismiss = { showLapseConfirm = false },
            onConfirm = {
                viewModel.logQuitLapse(goalId)
                showLapseConfirm = false
            },
        )
    }

    if (showSos && goal != null) {
        QuitSosSheet(
            goal = goal!!,
            onDismiss = { showSos = false },
        )
    }

    if (showPromote) {
        AlertDialog(
            onDismissRequest = { showPromote = false },
            title = {
                Text("Move to Maintain?", fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "You'll defend a weekly floor instead of chasing a finish line. Your private line comes with you.",
                    fontFamily = dmSansFamily,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.promoteToMaintain(goalId)
                    showPromote = false
                }) {
                    Text("Promote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromote = false }) { Text("Not yet") }
            },
        )
    }

    if (showEndGoal && goal != null) {
        AlertDialog(
            onDismissRequest = { showEndGoal = false; endReason = "" },
            title = {
                Text(
                    "Step back from this goal?",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "No judgment — goals shift. What’s making you pause?",
                        fontFamily = dmSansFamily,
                        fontSize = 15.sp,
                    )
                    OutlinedTextField(
                        value = endReason,
                        onValueChange = { endReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Optional — helps you reflect later") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.endGoalWithReason(goalId, endReason)
                        showEndGoal = false
                        endReason = ""
                        onBack()
                    },
                    enabled = endReason.trim().length >= 3,
                ) {
                    Text("Archive goal", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndGoal = false; endReason = "" }) {
                    Text("Keep going")
                }
            },
        )
    }
}

@Composable
private fun TypeHeroHeader(
    goal: GoalEntity,
    metrics: GoalProgressEngine.GoalMetrics,
    color: Color,
) {
    val icon = when (goal.goalType) {
        GoalType.BUILD -> Icons.Rounded.Eco
        GoalType.REACH -> Icons.Rounded.TrackChanges
        GoalType.QUIT -> Icons.Rounded.SelfImprovement
        GoalType.MAINTAIN -> Icons.Rounded.LocalFireDepartment
    }
    val typeLabel = goal.goalType.name

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f))
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                typeLabel,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                color = color,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                goal.title,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (goal.whyStatement.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    goal.whyStatement,
                    fontFamily = dmSansFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))

            when (goal.goalType) {
                GoalType.REACH -> ReachHero(metrics, color)
                GoalType.QUIT -> QuitHero(metrics, color)
                GoalType.BUILD -> BuildHero(metrics, color)
                GoalType.MAINTAIN -> MaintainHero(metrics, color)
            }
        }
    }
}

@Composable
private fun ReachHero(metrics: GoalProgressEngine.GoalMetrics, color: Color) {
    val progress by animateFloatAsState(
        targetValue = metrics.progressFraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "reach_progress",
    )
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            // Trail arc — summit framing, not a generic % ring clone of Build
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 150f,
                sweepAngle = 240f * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                metrics.heroPrimary,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            metrics.heroSecondary?.let {
                Text(
                    it,
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun QuitHero(metrics: GoalProgressEngine.GoalMetrics, color: Color) {
    val breathe = rememberInfiniteTransition(label = "quit_clear")
    val glow by breathe.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "quit_glow",
    )
    val clearFactor = (metrics.cleanStreakDays / 30f).coerceIn(0.15f, 1f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = glow * clearFactor),
                        Color.Transparent,
                    ),
                ),
                radius = size.minDimension * 0.48f * clearFactor,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                metrics.heroPrimary,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            metrics.heroSecondary?.let {
                Text(
                    it,
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun BuildHero(metrics: GoalProgressEngine.GoalMetrics, color: Color) {
    val stage = metrics.buildStage
    val growth by animateFloatAsState(
        targetValue = (stage / 4f).coerceIn(0.12f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "build_growth",
    )
    val stageName = when (stage) {
        0 -> "Seed"
        1 -> "First Bloom"
        2 -> "Taking Root"
        3 -> "Steady Rhythm"
        else -> "Second Nature"
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val baseY = size.height * 0.78f
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = Offset(cx, baseY),
                end = Offset(cx, baseY - size.height * 0.45f * growth),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color.copy(alpha = 0.25f + 0.35f * growth),
                radius = size.minDimension * 0.12f * growth,
                center = Offset(cx, baseY - size.height * 0.48f * growth),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                metrics.heroPrimary,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stageName,
                fontFamily = dmSansFamily,
                fontSize = 14.sp,
                color = color,
            )
        }
    }
}

@Composable
private fun MaintainHero(metrics: GoalProgressEngine.GoalMetrics, color: Color) {
    val dim = if (metrics.consecutiveMissDays >= 2) 0.45f else if (metrics.floorMetThisWeek) 1f else 0.7f
    val pulse = rememberInfiniteTransition(label = "maintain_flame")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flame",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size((72 * scale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f * dim)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = color.copy(alpha = dim),
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            metrics.heroPrimary,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f + 0.5f * dim),
        )
        metrics.heroSecondary?.let {
            Text(
                it,
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun TypeMetaChips(
    goal: GoalEntity,
    metrics: GoalProgressEngine.GoalMetrics,
    color: Color,
) {
    val chips = buildList {
        when (goal.goalType) {
            GoalType.BUILD -> {
                goal.tinyVersionText?.let { add("Tiny: $it") }
                goal.anchorCue?.let { add(it) }
            }
            GoalType.REACH -> {
                metrics.paceDeltaLabel?.let { add(it) }
                metrics.projectedFinishLabel?.let { add(it) }
            }
            GoalType.QUIT -> {
                goal.triggerText?.let { add("Trigger: $it") }
                goal.replacementText?.let { add("Instead: $it") }
            }
            GoalType.MAINTAIN -> {
                val floor = goal.floorCount ?: 3
                val days = goal.floorPeriodDays ?: 7
                add("Floor: $floor / ${days}d")
            }
        }
    }
    if (chips.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            Text(
                chip,
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuitActions(color: Color, onSos: () -> Unit, onLogMoment: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onSos,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            Text(
                "I need a clear moment",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        TextButton(
            onClick = onLogMoment,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Log a slip — calmly",
                fontFamily = dmSansFamily,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun LapseConfirmDialog(
    longest: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "One moment",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                "That's one moment, not a reset of who you are. Longest streak stays: $longest days. Ready to start today?",
                fontFamily = dmSansFamily,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Log & restart", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuitSosSheet(goal: GoalEntity, onDismiss: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(15 * 60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "This usually passes",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                String.format("%d:%02d", mins, secs),
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ride the wave. About 15 minutes.",
                fontFamily = dmSansFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (goal.replacementText?.isNotBlank() == true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            "Do this instead",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            goal.replacementText!!,
                            fontFamily = frauncesFamily,
                            fontSize = 20.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (goal.whyStatement.isNotBlank()) {
                Text(
                    "What this costs: ${goal.whyStatement}",
                    fontFamily = dmSansFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onDismiss) {
                Text("I'm through it", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MilestoneStepper(milestones: List<MilestoneEntity>, color: Color) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        milestones.forEachIndexed { index, milestone ->
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (milestone.isCompleted) color else color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (milestone.isCompleted) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    if (index < milestones.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(
                                    if (milestone.isCompleted) color else color.copy(alpha = 0.2f),
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        milestone.title,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = if (milestone.isCompleted) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        },
                    )
                    if (milestone.isCompleted && milestone.completedDate != null) {
                        val dateStr = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                            .format(java.util.Date(milestone.completedDate))
                        Text(
                            "Reached $dateStr",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
