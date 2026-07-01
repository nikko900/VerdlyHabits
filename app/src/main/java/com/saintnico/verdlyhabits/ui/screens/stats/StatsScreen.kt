package com.saintnico.verdlyhabits.ui.screens.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.MotivationalEngine
import com.saintnico.verdlyhabits.engine.StatsEngine
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.ui.components.CountUpText
import com.saintnico.verdlyhabits.ui.components.PremiumInsightCard
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState
import kotlinx.coroutines.delay
import java.time.LocalDate

private val Mint = Color(0xFF52B788)
private val Teal = Color(0xFF2DD4BF)
private val Gold = Color(0xFFFFB300)
private val Purple = Color(0xFF7B6CF6)
private val Coral = Color(0xFFFF6B6B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    habits: List<HabitItem>,
    statsState: UserStatsUiState,
    hasFullAccess: Boolean,
    installDateMillis: Long,
    onRequestPaywall: (PaywallTrigger) -> Unit,
    onBack: () -> Unit,
) {
    val today = LocalDate.now()
    val metrics = remember(habits, statsState) {
        StatsEngine.computeDashboard(
            habits = habits,
            today = today,
            totalFocusMinutes = statsState.totalFocusMinutes,
            memberSinceMillis = statsState.memberSince,
        )
    }

    val dayMs = 1000L * 60 * 60 * 24
    val daysSinceInstall = (System.currentTimeMillis() - installDateMillis) / dayMs
    val shouldGateAnalytics = !hasFullAccess && daysSinceInstall >= 3
    var analyticsPrompted by remember { mutableStateOf(false) }

    LaunchedEffect(shouldGateAnalytics, analyticsPrompted) {
        if (shouldGateAnalytics && !analyticsPrompted) {
            delay(800)
            analyticsPrompted = true
            onRequestPaywall(PaywallTrigger.Analytics)
        }
    }

    val insights = remember(habits, statsState) {
        MotivationalEngine.generateInsights(
            habits = habits,
            longestStreakEver = statsState.longestStreakEver,
            totalCompletions = statsState.totalCompletions,
            totalXp = statsState.totalXp,
        ).take(3)
    }

    val quote = remember { StatsEngine.dailyQuote(today.dayOfYear) }
    val scoreColor = scoreTierColor(metrics.consistencyScore)
    val unlockedAchievements = statsState.achievements.filter { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Dashboard",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                        )
                        Text(
                            if (hasFullAccess) "Pro analytics" else "Your progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(if (shouldGateAnalytics) Modifier.blur(10.dp) else Modifier),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // ── HERO: Consistency score ─────────────────────────────
                item {
                    ConsistencyHeroCard(
                        score = metrics.consistencyScore,
                        tier = metrics.consistencyTier,
                        scoreColor = scoreColor,
                        weekDeltaPct = metrics.weekDeltaPct,
                        weekTrendUp = metrics.weekTrendUp,
                        level = statsState.level,
                        levelTitle = statsState.levelTitle,
                        progress = statsState.progressToNextLevel,
                    )
                }

                // ── Daily quote ─────────────────────────────────────────
                item {
                    QuoteCard(quote = quote)
                }

                // ── Top insight ─────────────────────────────────────────
                item {
                    PremiumInsightCard(
                        insight = insights.firstOrNull()
                            ?: MotivationalEngine.topInsight(
                                habits,
                                statsState.longestStreakEver,
                                statsState.totalCompletions,
                                statsState.totalXp,
                            ),
                    )
                }

                // ── Key metrics grid ────────────────────────────────────
                item {
                    MetricsGrid(
                        completions = metrics.allTimeCompletions,
                        perfectDays = metrics.perfectDays30,
                        focusHours = metrics.focusHours,
                        memberDays = metrics.memberDays,
                        longestStreak = metrics.longestStreak,
                        monthRate = metrics.thisMonthRate,
                    )
                }

                // ── 7-day pulse ─────────────────────────────────────────
                item {
                    SectionLabel("This week")
                    Spacer(Modifier.height(10.dp))
                    DailyPulseStrip(metrics.dailyPulse, scoreColor)
                }

                // ── 8-week trend ────────────────────────────────────────
                item {
                    WeeklyTrendCard(metrics.weeklyTrend, scoreColor)
                }

                // ── Day-of-week rhythm ──────────────────────────────────
                item {
                    WeekdayRhythmCard(metrics.weekdayRhythm, MaterialTheme.colorScheme.primary)
                }

                // ── Extra insights carousel ─────────────────────────────
                if (insights.size > 1) {
                    item {
                        SectionLabel("More insights")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(insights.drop(1)) { insight ->
                                Box(Modifier.width(280.dp)) {
                                    PremiumInsightCard(insight = insight)
                                }
                            }
                        }
                    }
                }

                // ── Habit leaderboard ───────────────────────────────────
                if (metrics.habitRankings.isNotEmpty()) {
                    item {
                        SectionLabel("Habit performance")
                        if (metrics.topHabit != null) {
                            Text(
                                "${metrics.topHabit.habit.title} is your strongest habit right now",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    items(metrics.habitRankings) { ranking ->
                        HabitPerformanceCard(ranking)
                    }
                }

                // ── Achievements snapshot ─────────────────────────────
                if (unlockedAchievements.isNotEmpty()) {
                    item {
                        SectionLabel("Achievements · ${unlockedAchievements.size} unlocked")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(unlockedAchievements.take(8)) { ach ->
                                AchievementChip(ach)
                            }
                        }
                    }
                }

                // ── Year in review teaser (locked) ────────────────────
                item {
                    YearInReviewTeaser()
                }

                item { Spacer(Modifier.height(32.dp)) }
            }

            if (shouldGateAnalytics) {
                PaywallOverlay(onUnlock = { onRequestPaywall(PaywallTrigger.Analytics) })
            }
        }
    }
}

// ── Hero consistency card ───────────────────────────────────────────────────

@Composable
private fun ConsistencyHeroCard(
    score: Int,
    tier: String,
    scoreColor: Color,
    weekDeltaPct: Int,
    weekTrendUp: Boolean,
    level: Int,
    levelTitle: String,
    progress: Float,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            scoreColor.copy(alpha = 0.22f),
                            Purple.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .border(1.dp, scoreColor.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            "CONSISTENCY SCORE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.8.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            CountUpText(
                                targetValue = score,
                                fontSize = 64.sp,
                                color = scoreColor,
                            )
                            Text(
                                "/100",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                        Text(
                            tier,
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                        ScoreRing(score / 100f, scoreColor, 96.dp)
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            null,
                            tint = scoreColor,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrendBadge(weekDeltaPct, weekTrendUp)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Level $level · $levelTitle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(120.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = scoreColor,
                            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendBadge(deltaPct: Int, up: Boolean) {
    val color = if (up) Mint else Coral
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
            null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "${if (up) "+" else ""}$deltaPct% vs last week",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun ScoreRing(rate: Float, color: Color, size: androidx.compose.ui.unit.Dp) {
    val animated by animateFloatAsState(
        targetValue = rate.coerceIn(0f, 1f),
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "score_ring",
    )
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    Canvas(Modifier.size(size)) {
        val stroke = 10.dp.toPx()
        val pad = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (animated > 0f) {
            drawArc(
                brush = Brush.sweepGradient(listOf(color, color.copy(alpha = 0.5f), color)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

// ── Quote card ──────────────────────────────────────────────────────────────

@Composable
private fun QuoteCard(quote: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.FormatQuote,
                null,
                tint = Purple.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                quote,
                fontFamily = frauncesFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            )
        }
    }
}

// ── Metrics grid ────────────────────────────────────────────────────────────

@Composable
private fun MetricsGrid(
    completions: Int,
    perfectDays: Int,
    focusHours: Float,
    memberDays: Int,
    longestStreak: Int,
    monthRate: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Check,
                value = "$completions",
                label = "Total done",
                color = Mint,
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Star,
                value = "$perfectDays",
                label = "Perfect days (30d)",
                color = Gold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Timer,
                value = if (focusHours >= 1f) "%.1fh".format(focusHours) else "${(focusHours * 60).toInt()}m",
                label = "Focus time",
                color = Teal,
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                value = "${longestStreak}d",
                label = "Best streak",
                color = Coral,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Schedule,
                value = "$memberDays",
                label = "Days with Verdly",
                color = Purple,
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.AutoAwesome,
                value = "${(monthRate * 100).toInt()}%",
                label = "This month",
                color = scoreTierColor((monthRate * 100).toInt()),
            )
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                value,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Daily pulse strip ───────────────────────────────────────────────────────

@Composable
private fun DailyPulseStrip(days: List<StatsEngine.DayPulse>, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                val ringColor = when {
                    day.rate >= 1f -> accent
                    day.rate >= 0.5f -> accent.copy(alpha = 0.7f)
                    day.rate > 0f -> Gold
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        day.label,
                        fontSize = 11.sp,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (day.isToday) accent
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(if (day.isToday) 44.dp else 38.dp)
                            .then(
                                if (day.isToday) Modifier.border(2.dp, accent, CircleShape) else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        DayRing(rate = day.rate, color = ringColor, size = if (day.isToday) 38.dp else 32.dp)
                        if (day.rate >= 1f) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRing(rate: Float, color: Color, size: androidx.compose.ui.unit.Dp) {
    val animated by animateFloatAsState(rate.coerceIn(0f, 1f), spring(), label = "day_ring")
    Canvas(Modifier.size(size)) {
        val stroke = 4.dp.toPx()
        val pad = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = Color.White.copy(alpha = 0.08f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (animated > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

// ── Weekly trend ──────────────────────────────────────────────────────────────

@Composable
private fun WeeklyTrendCard(data: List<StatsEngine.WeekPoint>, accent: Color) {
    val animValues = data.map { point ->
        val anim by animateFloatAsState(
            targetValue = point.rate,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
            label = "week_${point.label}",
        )
        anim
    }
    val maxH = 140.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionLabel("8-week trend")
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                data.forEachIndexed { i, point ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        AnimatedVisibility(visible = animValues[i] > 0.04f) {
                            Text(
                                "${(animValues[i] * 100).toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (point.isCurrent) accent
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(maxH * animValues[i].coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (point.isCurrent) {
                                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.55f)))
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            ),
                                        )
                                    },
                                ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            point.label,
                            fontSize = 10.sp,
                            fontWeight = if (point.isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (point.isCurrent) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
    }
}

// ── Weekday rhythm ────────────────────────────────────────────────────────────

@Composable
private fun WeekdayRhythmCard(data: List<StatsEngine.WeekdayPoint>, accent: Color) {
    val animValues = data.map { point ->
        val anim by animateFloatAsState(point.rate, spring(), label = "dow_${point.label}")
        anim
    }
    val best = data.maxOfOrNull { it.rate } ?: 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionLabel("Your rhythm")
            Text(
                "Which days you show up most",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                data.forEachIndexed { i, point ->
                    val isBest = point.rate > 0f && point.rate >= best
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(80.dp * animValues[i].coerceAtLeast(0.06f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isBest) Brush.verticalGradient(listOf(Gold, Gold.copy(alpha = 0.6f)))
                                    else Brush.verticalGradient(
                                        listOf(accent.copy(alpha = 0.7f), accent.copy(alpha = 0.3f)),
                                    ),
                                ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            point.label.take(3),
                            fontSize = 10.sp,
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Medium,
                            color = if (isBest) Gold else MaterialTheme.colorScheme.onBackground.copy(0.5f),
                        )
                    }
                }
            }
        }
    }
}

// ── Habit performance card ────────────────────────────────────────────────────

@Composable
private fun HabitPerformanceCard(ranking: StatsEngine.HabitRanking) {
    val habit = ranking.habit
    val habitColor = Color(habit.color.toInt())
    val rateColor = scoreTierColor(ranking.consistency30)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(habitColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#${ranking.rank}",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = habitColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(habitColor.copy(0.2f), habitColor.copy(0.05f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(habit.icon, null, tint = habitColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniChip(
                        icon = { StreakFlame(streak = ranking.streak, size = 11.dp) },
                        text = "${ranking.streak}d",
                    )
                    MiniChip(text = "${ranking.consistency30}%", color = rateColor)
                    MiniChip(text = "${ranking.total} total")
                }
            }
            HabitSparkline(ranking.spark7, habitColor)
        }
    }
}

@Composable
private fun MiniChip(
    text: String? = null,
    color: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        icon?.invoke()
        if (icon != null && text != null) Spacer(Modifier.width(3.dp))
        if (text != null) {
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun HabitSparkline(data: List<Float>, color: Color) {
    Canvas(Modifier.size(width = 52.dp, height = 28.dp)) {
        if (data.size < 2) return@Canvas
        val w = size.width / (data.size - 1)
        val h = size.height
        val pts = data.mapIndexed { i, v -> Offset(i * w, h - v * h * 0.85f) }
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

// ── Achievement chip ─────────────────────────────────────────────────────────

@Composable
private fun AchievementChip(achievement: Achievement) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.EmojiEvents, null, tint = Gold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            achievement.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp,
        )
    }
}

// ── Year in review teaser ─────────────────────────────────────────────────────

@Composable
private fun YearInReviewTeaser() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Purple.copy(0.08f), MaterialTheme.colorScheme.surface),
                    ),
                )
                .padding(24.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = Purple.copy(0.6f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "YEAR IN REVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = Purple.copy(0.7f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your wrapped story is brewing",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "A cinematic recap of your best streaks, wins, and milestones — coming soon for Pro members.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun PaywallOverlay(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(32.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Unlock Pro Analytics",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Deep insights, trends, and your full dashboard — built for serious habit builders.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.Button(onClick = onUnlock) {
                    Text("See Pro plans", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun scoreTierColor(score: Int): Color = when {
    score >= 80 -> Mint
    score >= 60 -> Teal
    score >= 40 -> Gold
    else -> Coral
}
