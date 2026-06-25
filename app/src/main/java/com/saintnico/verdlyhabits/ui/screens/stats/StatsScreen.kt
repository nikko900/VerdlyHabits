package com.saintnico.verdlyhabits.ui.screens.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.MotivationalEngine
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.ui.components.CountUpText
import com.saintnico.verdlyhabits.ui.components.PremiumInsightCard
import com.saintnico.verdlyhabits.ui.components.ShimmerGradientBorder
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    habits: List<HabitItem>,
    hasFullAccess: Boolean,
    installDateMillis: Long,
    onRequestPaywall: (PaywallTrigger) -> Unit,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE

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

    val activeHabits = habits.filter { !it.isArchived }

    // Build 365-day map
    val heatmapData: Map<LocalDate, Float> = buildMap {
        for (i in 0 until 365) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(fmt)
            val completed = activeHabits.count { it.completedDates.contains(dateStr) }
            val total = activeHabits.size.coerceAtLeast(1)
            put(date, completed.toFloat() / total)
        }
    }

    // 8-week data
    val weeklyData: List<Pair<String, Float>> = (0 until 8).map { weekBack ->
        val weekEnd = today.minusWeeks(weekBack.toLong())
        val label = if (weekBack == 0) "Now" else "-${weekBack}w"
        var totalRate = 0f
        for (d in 0 until 7) {
            val date = weekEnd.minusDays(d.toLong())
            totalRate += heatmapData[date] ?: 0f
        }
        label to (totalRate / 7f)
    }.reversed()

    // Key stats
    val allTimeCompletions = activeHabits.sumOf { it.completedDates.size }
    val longestStreak = activeHabits.maxOfOrNull { it.streak } ?: 0
    val perfectDays = run {
        var count = 0
        for (i in 0 until 365) {
            val dateStr = today.minusDays(i.toLong()).format(fmt)
            if (activeHabits.isNotEmpty() && activeHabits.all { it.completedDates.contains(dateStr) }) count++
        }
        count
    }
    val bestWeekRate = weeklyData.maxOfOrNull { it.second } ?: 0f
    val thisMonthRate = run {
        val daysInMonth = today.lengthOfMonth()
        var done = 0; var total = 0
        for (d in 0 until today.dayOfMonth) {
            val dateStr = today.minusDays(d.toLong()).format(fmt)
            total += activeHabits.size
            done += activeHabits.count { it.completedDates.contains(dateStr) }
        }
        if (total == 0) 0f else done.toFloat() / total
    }

    // Color coding for completion rate
    val rateColor = completionColor(thisMonthRate)
    val weekTrendUp = weeklyData.size >= 2 && weeklyData.last().second >= weeklyData[weeklyData.size - 2].second

    // Motivational insight
    val topInsight = remember(habits, longestStreak, allTimeCompletions) {
        MotivationalEngine.topInsight(habits, longestStreak, allTimeCompletions, 0)
    }

    var tooltipDate by remember { mutableStateOf<LocalDate?>(null) }
    var tooltipText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Statistics", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val blurMod = if (shouldGateAnalytics) Modifier.blur(8.dp) else Modifier
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .then(blurMod),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── HERO BLOCK ──────────────────────────────────────────────
            item {
                HeroStatsBlock(
                    longestStreak = longestStreak,
                    thisMonthRate = thisMonthRate,
                    rateColor = rateColor,
                    allTimeCompletions = allTimeCompletions,
                    perfectDays = perfectDays,
                    weekTrendUp = weekTrendUp
                )
            }

            // ── MOTIVATIONAL INSIGHT ────────────────────────────────────
            item {
                PremiumInsightCard(insight = topInsight)
            }

            // ── WEEKLY HEATMAP ──────────────────────────────────────────
            item {
                SectionHeader("This Week")
                Spacer(Modifier.height(8.dp))
                WeeklyHeatmap(
                    data = heatmapData,
                    today = today,
                    accentColor = MaterialTheme.colorScheme.primary,
                    surfaceColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // ── YEAR CONTRIBUTION HEATMAP ───────────────────────────────
            item {
                SectionHeader("Year in Review")
                Spacer(Modifier.height(8.dp))
                HeatmapGrid(
                    data = heatmapData,
                    today = today,
                    accentColor = MaterialTheme.colorScheme.primary,
                    surfaceColor = MaterialTheme.colorScheme.surfaceVariant,
                    onCellTap = { date ->
                        tooltipDate = date
                        val dateStr = date.format(fmt)
                        val done = activeHabits.count { it.completedDates.contains(dateStr) }
                        tooltipText = "${date.format(DateTimeFormatter.ofPattern("MMM d"))} — $done/${activeHabits.size}"
                    }
                )
                if (tooltipDate != null) {
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ── WEEKLY BAR CHART ────────────────────────────────────────
            item {
                SectionHeader("Weekly Trend")
                Spacer(Modifier.height(8.dp))
                WeeklyBarChart(weeklyData, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant)
            }

            // ── PER-HABIT STAT CARDS ────────────────────────────────────
            item { SectionHeader("Per Habit") }
            items(activeHabits) { habit ->
                HabitStatCard(habit, MaterialTheme.colorScheme.primary, today, fmt)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Color coding ────────────────────────────────────────────────────────────

@Composable
private fun completionColor(rate: Float): Color = when {
    rate < 0.50f -> Color(0xFFF59E0B) // warm amber
    rate < 0.80f -> Color(0xFF2DD4BF) // teal
    else -> Color(0xFF4CAF50)          // electric green
}

// ── HERO BLOCK ──────────────────────────────────────────────────────────────

@Composable
private fun HeroStatsBlock(
    longestStreak: Int,
    thisMonthRate: Float,
    rateColor: Color,
    allTimeCompletions: Int,
    perfectDays: Int,
    weekTrendUp: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Best Streak + Monthly Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Best Streak
                Column {
                    Text(
                        "BEST STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp,
                        fontSize = 10.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        CountUpText(
                            targetValue = longestStreak,
                            fontSize = 52.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "days",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                // Monthly completion arc
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CompletionArc(rate = thisMonthRate, color = rateColor, size = 80.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(thisMonthRate * 100).toInt()}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = rateColor
                        )
                        Text(
                            "this month",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Row 2: Secondary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStatPill(
                    icon = Icons.Rounded.CheckCircle,
                    value = "$allTimeCompletions",
                    label = "Total Done",
                    color = MaterialTheme.colorScheme.primary
                )
                MiniStatPill(
                    icon = Icons.Rounded.Star,
                    value = "$perfectDays",
                    label = "Perfect Days",
                    color = Color(0xFFFFB300)
                )
                MiniStatPill(
                    icon = if (weekTrendUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                    value = if (weekTrendUp) "Up" else "Down",
                    label = "This Week",
                    color = if (weekTrendUp) Color(0xFF4CAF50) else Color(0xFFEF5350)
                )
            }
        }
    }
}

@Composable
private fun CompletionArc(rate: Float, color: Color, size: androidx.compose.ui.unit.Dp) {
    val animated by animateFloatAsState(
        targetValue = rate.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "arc_progress"
    )
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 8.dp.toPx()
        val pad = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = track,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(pad, pad), size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        if (animated > 0f) {
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = Offset(pad, pad), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun MiniStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            value, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            label, fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

// ── WEEKLY HEATMAP ──────────────────────────────────────────────────────────

@Composable
private fun WeeklyHeatmap(
    data: Map<LocalDate, Float>,
    today: LocalDate,
    accentColor: Color,
    surfaceColor: Color
) {
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dayLabels = days.map { it.dayOfWeek.name.take(3) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { i, date ->
            val rate = data[date] ?: 0f
            val cellColor = if (rate == 0f) surfaceColor
            else accentColor.copy(alpha = 0.2f + rate * 0.8f)
            val isToday = date == today

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dayLabels[i],
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) accentColor
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellColor)
                        .then(
                            if (isToday) Modifier.border(
                                2.dp, accentColor, RoundedCornerShape(10.dp)
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (rate >= 1f) {
                        Icon(
                            Icons.Rounded.Check, null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (rate > 0f) {
                        Text(
                            "${(rate * 100).toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

// ── YEAR HEATMAP (from original, kept intact) ───────────────────────────────

@Composable
private fun HeatmapGrid(
    data: Map<LocalDate, Float>,
    today: LocalDate,
    accentColor: Color,
    surfaceColor: Color,
    onCellTap: (LocalDate) -> Unit
) {
    val cellSize = 10.dp
    val gap = 2.dp
    val totalDays = 364
    val startDate = today.minusDays(totalDays.toLong())
    val dowOffset = (startDate.dayOfWeek.value - 1) % 7
    val paddedStart = startDate.minusDays(dowOffset.toLong())

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height((cellSize + gap) * 7 + gap)
    ) {
        val cellPx = cellSize.toPx()
        val gapPx = gap.toPx()
        for (col in 0..52) {
            for (row in 0..6) {
                val date = paddedStart.plusDays((col * 7 + row).toLong())
                val rate = data[date] ?: 0f
                val alpha = when {
                    rate == 0f -> 0.08f
                    rate < 0.25f -> 0.25f
                    rate < 0.5f -> 0.5f
                    rate < 0.75f -> 0.75f
                    else -> 1f
                }
                drawRoundRect(
                    color = if (rate == 0f) surfaceColor else accentColor.copy(alpha = alpha),
                    topLeft = Offset(col * (cellPx + gapPx), row * (cellPx + gapPx)),
                    size = Size(cellPx, cellPx),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

// ── WEEKLY BAR CHART ────────────────────────────────────────────────────────

@Composable
private fun WeeklyBarChart(
    data: List<Pair<String, Float>>,
    accentColor: Color,
    surfaceColor: Color
) {
    val animValues = data.mapIndexed { i, (_, rate) ->
        val anim by animateFloatAsState(
            targetValue = rate,
            animationSpec = tween(700, delayMillis = i * 60),
            label = "bar_$i"
        )
        anim
    }
    val maxH = 120.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { i, (label, _) ->
            val isNow = label == "Now"
            val barColor = if (isNow) accentColor else surfaceColor
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Value label on top
                if (animValues[i] > 0.01f) {
                    Text(
                        "${(animValues[i] * 100).toInt()}%",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxH * animValues[i].coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label, fontSize = 9.sp,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── PER-HABIT STAT CARDS ────────────────────────────────────────────────────

@Composable
private fun HabitStatCard(habit: HabitItem, accentColor: Color, today: LocalDate, fmt: DateTimeFormatter) {
    val last30 = (0 until 30).count { day ->
        habit.completedDates.contains(today.minusDays(day.toLong()).format(fmt))
    }
    val rate30 = (last30 * 100 / 30)
    val rateColor = completionColor(rate30 / 100f)

    // 7-day sparkline
    val spark = (6 downTo 0).map { day ->
        val dateStr = today.minusDays(day.toLong()).format(fmt)
        if (habit.completedDates.contains(dateStr)) 1f else 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(habit.color.toInt()).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(habit.icon, null, tint = Color(habit.color.toInt()), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))

            // Name + stats
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(
                        icon = { StreakFlame(streak = habit.streak, size = 12.dp) },
                        text = "${habit.streak}d"
                    )
                    StatChip(
                        icon = null,
                        text = "$rate30%",
                        textColor = rateColor
                    )
                    StatChip(
                        icon = null,
                        text = "${habit.completedDates.size} total"
                    )
                }
            }

            // Sparkline
            Sparkline(spark, accentColor)
        }
    }
}

@Composable
private fun StatChip(
    icon: (@Composable () -> Unit)? = null,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        icon?.invoke()
        if (icon != null) Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
private fun Sparkline(data: List<Float>, color: Color) {
    Canvas(modifier = Modifier.size(width = 56.dp, height = 24.dp)) {
        val w = size.width / (data.size - 1).coerceAtLeast(1)
        val h = size.height
        val pts = data.mapIndexed { i, v -> Offset(i * w, h - v * h * 0.85f) }
        for (i in 0 until pts.size - 1) {
            drawLine(color.copy(alpha = 0.7f), pts[i], pts[i + 1], strokeWidth = 3.dp.toPx())
        }
        pts.forEach { p ->
            drawCircle(color, radius = 2.5.dp.toPx(), center = p)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}
