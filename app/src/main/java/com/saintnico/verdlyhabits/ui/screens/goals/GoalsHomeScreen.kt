package com.saintnico.verdlyhabits.ui.screens.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.goals.GoalCreationDraft
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.notifications.GoalReminderScheduler
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Warm, calm surface for goals: an ambient gradient, one headline momentum score, and each
 * goal as a frosted row that opens to reveal its "why". The score and the rows read from the
 * same progress fraction, so the headline can never disagree with the list under it.
 */
@Composable
fun GoalsHomeScreen(
    viewModel: GoalsViewModel,
    habits: List<com.saintnico.verdlyhabits.ui.screens.home.HabitItem>,
    onNavigateToAddGoal: () -> Unit,
    onNavigateToContinueGoal: () -> Unit = onNavigateToAddGoal,
    onNavigateToGoalDetail: (String) -> Unit
) {
    val activeGoals by viewModel.activeGoals.collectAsState()
    val creationDraft by viewModel.creationDraft.collectAsState()
    val context = LocalContext.current
    val themePref = remember { ThemePreference(context) }
    val scope = rememberCoroutineScope()
    val dailyOn by themePref.goalDailyReminderEnabled.collectAsState(initial = true)
    val weeklyOn by themePref.goalWeeklyCheckInEnabled.collectAsState(initial = true)
    val dailyTime by themePref.goalDailyReminderTime.collectAsState(initial = "20:30")

    val palette = rememberGoalsPalette()

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackdrop(palette)

        if (activeGoals.isEmpty() && creationDraft?.hasProgress != true) {
            GoalsEmptyState(palette, onNavigateToAddGoal)
            return@Box
        }

        if (activeGoals.isEmpty()) {
            GoalsEmptyStateWithDraft(
                palette = palette,
                draft = creationDraft!!,
                onContinue = onNavigateToContinueGoal,
                onStartOver = {
                    viewModel.clearCreationDraft()
                    onNavigateToAddGoal()
                },
            )
            return@Box
        }

        val scored = activeGoals.map { it to viewModel.progressFor(it, habits) }
        val momentum = scored.map { it.second }.average().toFloat()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MomentumHero(
                    score = momentum,
                    goalCount = activeGoals.size,
                    palette = palette,
                )
            }

            if (creationDraft?.hasProgress == true) {
                item {
                    ContinueGoalDraftCard(
                        draft = creationDraft!!,
                        palette = palette,
                        onContinue = onNavigateToContinueGoal,
                    )
                }
            }

            items(scored, key = { it.first.id }) { (goal, progress) ->
                GoalGlassRow(
                    goal = goal,
                    progress = progress,
                    metrics = viewModel.metricsFor(goal, habits),
                    palette = palette,
                    onOpen = { onNavigateToGoalDetail(goal.id) },
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                RemindersGlassCard(
                    palette = palette,
                    dailyEnabled = dailyOn,
                    weeklyEnabled = weeklyOn,
                    dailyTime = dailyTime,
                    onDailyToggle = { enabled ->
                        scope.launch {
                            themePref.setGoalDailyReminderEnabled(enabled)
                            GoalReminderScheduler.scheduleAll(context)
                        }
                    },
                    onWeeklyToggle = { enabled ->
                        scope.launch {
                            themePref.setGoalWeeklyCheckInEnabled(enabled)
                            GoalReminderScheduler.scheduleAll(context)
                        }
                    },
                    onDailyTimeChange = { time ->
                        scope.launch {
                            themePref.setGoalDailyReminderTime(time)
                            GoalReminderScheduler.scheduleAll(context)
                        }
                    },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                FooterActions(palette = palette, onAddGoal = onNavigateToAddGoal)
            }
        }
    }
}

// ── Palette ───────────────────────────────────────────────────────────────────

/**
 * Warm ambient palette. Kept separate from [MaterialTheme] because this surface is
 * deliberately warmer than the rest of the app, and the glass tiles need explicit
 * light/dark alphas rather than tonal elevation.
 */
private data class GoalsPalette(
    val dark: Boolean,
    val backdropTop: Color,
    val backdropBottom: Color,
    val glowWarm: Color,
    val glowDeep: Color,
    val glass: Color,
    val glassBorder: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
)

@Composable
private fun rememberGoalsPalette(): GoalsPalette {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    return remember(dark) {
        if (dark) {
            GoalsPalette(
                dark = true,
                backdropTop = Color(0xFF2A1B0E),
                backdropBottom = Color(0xFF0E1210),
                glowWarm = Color(0xFFD4890A),
                glowDeep = Color(0xFF1D6B44),
                glass = Color.White.copy(alpha = 0.07f),
                glassBorder = Color.White.copy(alpha = 0.14f),
                ink = Color(0xFFFDF6EC),
                inkSoft = Color(0xFFFDF6EC).copy(alpha = 0.62f),
                accent = Color(0xFFFFC05A),
            )
        } else {
            GoalsPalette(
                dark = false,
                backdropTop = Color(0xFFFBEBD2),
                backdropBottom = Color(0xFFF9F6F0),
                glowWarm = Color(0xFFF0B457),
                glowDeep = Color(0xFF66B386),
                glass = Color.White.copy(alpha = 0.66f),
                glassBorder = Color.White.copy(alpha = 0.85f),
                ink = Color(0xFF2A2118),
                inkSoft = Color(0xFF2A2118).copy(alpha = 0.60f),
                accent = Color(0xFFB8760A),
            )
        }
    }
}

private fun Color.luminanceIsDark(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f

// ── Backdrop ──────────────────────────────────────────────────────────────────

@Composable
private fun AmbientBackdrop(palette: GoalsPalette) {
    val drift = rememberInfiniteTransition(label = "goal_ambient")
    val shift by drift.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "goal_ambient_shift",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.backdropTop, palette.backdropBottom),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.glowWarm.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.16f),
                radius = size.width * 0.95f * shift,
            ),
            radius = size.width * 0.95f * shift,
            center = Offset(size.width * 0.5f, size.height * 0.16f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.glowDeep.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.08f, size.height * 0.68f),
                radius = size.width * 0.8f,
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width * 0.08f, size.height * 0.68f),
        )
    }
}

// ── Hero score ────────────────────────────────────────────────────────────────

@Composable
private fun MomentumHero(score: Float, goalCount: Int, palette: GoalsPalette) {
    val animated by animateFloatAsState(
        targetValue = score.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "momentum",
    )
    val outOfTen = (animated * 10f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Goal momentum",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = palette.inkSoft,
        )

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier.size(184.dp),
            contentAlignment = Alignment.Center,
        ) {
            LotusBloom(progress = animated, palette = palette)
            Text(
                String.format("%.1f", outOfTen),
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                color = palette.ink,
            )
        }

        Text(
            if (goalCount == 1) "1 goal in motion" else "$goalCount goals in motion",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = palette.inkSoft,
        )

        Spacer(Modifier.height(16.dp))
    }
}

/** Layered petals plus a progress ring. Purely decorative; the ring carries the real value. */
@Composable
private fun LotusBloom(progress: Float, palette: GoalsPalette) {
    val breathe = rememberInfiniteTransition(label = "lotus")
    val scale by breathe.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lotus_breathe",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val petalCount = 8
        val petalLength = size.minDimension * 0.40f * scale
        val petalWidth = size.minDimension * 0.155f

        for (i in 0 until petalCount) {
            val angle = (360f / petalCount) * i
            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                val path = Path().apply {
                    moveTo(cx, cy)
                    cubicTo(
                        cx - petalWidth, cy - petalLength * 0.42f,
                        cx - petalWidth * 0.62f, cy - petalLength,
                        cx, cy - petalLength,
                    )
                    cubicTo(
                        cx + petalWidth * 0.62f, cy - petalLength,
                        cx + petalWidth, cy - petalLength * 0.42f,
                        cx, cy,
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.glowWarm.copy(alpha = if (palette.dark) 0.30f else 0.38f),
                            palette.glowWarm.copy(alpha = 0.05f),
                        ),
                        startY = cy - petalLength,
                        endY = cy,
                    ),
                )
            }
        }

        val ringRadius = size.minDimension * 0.46f
        drawArc(
            color = palette.glassBorder,
            startAngle = 130f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(cx - ringRadius, cy - ringRadius),
            size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(palette.glowDeep, palette.glowWarm, palette.glowDeep),
                center = Offset(cx, cy),
            ),
            startAngle = 130f,
            sweepAngle = 280f * progress,
            useCenter = false,
            topLeft = Offset(cx - ringRadius, cy - ringRadius),
            size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

// ── Goal row ──────────────────────────────────────────────────────────────────

@Composable
private fun GoalGlassRow(
    goal: GoalEntity,
    progress: Float,
    metrics: GoalProgressEngine.GoalMetrics,
    palette: GoalsPalette,
    onOpen: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val goalColor = remember(goal.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(goal.colorHex)) }
            .getOrDefault(palette.accent)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "goal_progress_${goal.id}",
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "chevron_${goal.id}",
    )
    val daysLeft = TimeUnit.MILLISECONDS
        .toDays(goal.targetDate - System.currentTimeMillis())
        .coerceAtLeast(0)
    val showBar = metrics.showProgressBar
    val metaLine = when (goal.goalType) {
        GoalType.REACH -> if (daysLeft == 0L) "Due today" else "$daysLeft days left"
        GoalType.BUILD -> metrics.heroSecondary ?: "Growing a rhythm"
        GoalType.QUIT -> metrics.heroSecondary ?: "Clear days"
        GoalType.MAINTAIN -> metrics.heroSecondary ?: "Holding the floor"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.glass)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(22.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(goalColor),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.goalType.name,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    color = goalColor,
                )
                Text(
                    goal.title,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = palette.ink,
                )
            }
            Text(
                metrics.listStatus,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = palette.ink,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = palette.inkSoft,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation),
            )
        }

        if (showBar) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(palette.ink.copy(alpha = 0.10f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(goalColor.copy(alpha = 0.75f), goalColor),
                            ),
                        ),
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                metaLine,
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = palette.inkSoft,
                maxLines = 1,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (goal.whyStatement.isNotBlank()) {
                    Text(
                        goal.whyStatement,
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = palette.inkSoft,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        metaLine,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = palette.inkSoft,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(
                            "Open",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = palette.accent,
                        )
                    }
                }
            }
        }
    }
}

// ── Reminders ─────────────────────────────────────────────────────────────────

@Composable
private fun RemindersGlassCard(
    palette: GoalsPalette,
    dailyEnabled: Boolean,
    weeklyEnabled: Boolean,
    dailyTime: String,
    onDailyToggle: (Boolean) -> Unit,
    onWeeklyToggle: (Boolean) -> Unit,
    onDailyTimeChange: (String) -> Unit,
) {
    val times = listOf("08:00", "12:00", "18:00", "20:30")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.glass)
            .border(1.dp, palette.glassBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Goal reminders",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = palette.ink,
            )
        }
        Text(
            "Weekly check-in on Sunday evening, plus a daily nudge at the time you pick.",
            fontFamily = dmSansFamily,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = palette.inkSoft,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Weekly check-in", fontFamily = dmSansFamily, fontSize = 14.sp, color = palette.ink)
            Switch(checked = weeklyEnabled, onCheckedChange = onWeeklyToggle)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Daily motivation", fontFamily = dmSansFamily, fontSize = 14.sp, color = palette.ink)
            Switch(checked = dailyEnabled, onCheckedChange = onDailyToggle)
        }
        if (dailyEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                times.forEach { t ->
                    val selected = t == dailyTime
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) palette.accent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (selected) palette.accent.copy(alpha = 0.55f) else palette.glassBorder,
                                CircleShape,
                            )
                            .clickable { onDailyTimeChange(t) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            t,
                            fontFamily = dmSansFamily,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (selected) palette.accent else palette.inkSoft,
                        )
                    }
                }
            }
        }
    }
}

// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun FooterActions(palette: GoalsPalette, onAddGoal: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.glass)
                .border(1.dp, palette.glassBorder, RoundedCornerShape(18.dp))
                .clickable { onAddGoal() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = palette.ink,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "New goal",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = palette.ink,
                )
            }
        }
    }
}

// ── Empty ─────────────────────────────────────────────────────────────────────

@Composable
private fun ContinueGoalDraftCard(
    draft: GoalCreationDraft,
    palette: GoalsPalette,
    onContinue: () -> Unit,
) {
    Card(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.accent.copy(alpha = 0.12f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    draft.continueLabel(),
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = palette.ink,
                )
                Text(
                    "Step ${draft.currentStep} of 6 · pick up where you left off",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = palette.inkSoft,
                )
            }
        }
    }
}

@Composable
private fun GoalsEmptyStateWithDraft(
    palette: GoalsPalette,
    draft: GoalCreationDraft,
    onContinue: () -> Unit,
    onStartOver: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ContinueGoalDraftCard(draft = draft, palette = palette, onContinue = onContinue)
        Spacer(Modifier.height(24.dp))
        Text(
            "You were shaping something. Continue, or begin fresh.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = palette.inkSoft,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onStartOver) {
            Text(
                "Start a different goal",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                color = palette.inkSoft,
            )
        }
    }
}

@Composable
private fun GoalsEmptyState(palette: GoalsPalette, onNavigateToAddGoal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(168.dp),
            contentAlignment = Alignment.Center,
        ) {
            LotusBloom(progress = 0f, palette = palette)
            Icon(
                Icons.Rounded.TrackChanges,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "What are you working toward?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 27.sp,
            textAlign = TextAlign.Center,
            color = palette.ink,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Set your first goal and Verdly will build the habits that get you there.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = palette.inkSoft,
        )

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(palette.glowDeep, palette.glowWarm),
                    ),
                )
                .clickable { onNavigateToAddGoal() }
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            Text(
                "Set my first goal",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
        }
    }
}
