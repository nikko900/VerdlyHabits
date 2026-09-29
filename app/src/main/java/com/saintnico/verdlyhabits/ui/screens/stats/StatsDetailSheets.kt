package com.saintnico.verdlyhabits.ui.screens.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.StatsEngine
import com.saintnico.verdlyhabits.engine.StatsInsightEngine
import com.saintnico.verdlyhabits.ui.components.CountUpText
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.theme.jetBrainsMonoFamily
import kotlin.math.roundToInt

private val SheetMint = Color(0xFF52B788)
private val SheetGold = Color(0xFFFFB300)
private val SheetCoral = Color(0xFFFF6B6B)
private val SheetPurple = Color(0xFF7B6CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsExploreHost(
    explore: StatsExplore?,
    report: StatsInsightEngine.Report,
    metrics: StatsEngine.DashboardMetrics,
    accent: Color,
    onDismiss: () -> Unit,
) {
    if (explore == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (explore) {
                StatsExplore.Consistency -> ConsistencyExplore(report, accent)
                StatsExplore.Week -> WeekExplore(report, accent)
                StatsExplore.Trend -> TrendExplore(metrics, report, accent)
                StatsExplore.Rhythm -> RhythmExplore(metrics, report, accent)
                is StatsExplore.Metric -> MetricExplore(report.metrics[explore.key], accent)
                is StatsExplore.Habit -> {
                    val dive = report.habits.firstOrNull { it.habit.id == explore.habitId }
                    if (dive != null) HabitExplore(dive, accent)
                }
                is StatsExplore.Signal -> {
                    val signal = report.signals.firstOrNull { it.title == explore.title }
                    if (signal != null) SignalExplore(signal, accent)
                }
            }
        }
    }
}

@Composable
private fun ConsistencyExplore(report: StatsInsightEngine.Report, accent: Color) {
    val score = report.score
    Eyebrow("CONSISTENCY")
    Text(
        score.headline,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(108.dp)) {
            ExploreRing(score.score / 100f, accent, 108.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountUpText(targetValue = score.score, fontSize = 32.sp, color = accent)
                Text("/100", fontSize = 11.sp, color = muted())
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PointBar("Show-up · 30d", score.ratePoints, 60, accent)
            PointBar("Streak strength", score.streakPoints, 25, SheetGold)
            PointBar("Perfect days", score.perfectPoints, 15, SheetPurple)
        }
    }
    Story(score.story)
    score.congratulations?.let { GlowNote(it, SheetGold, Icons.Rounded.AutoAwesome) }
    NextMove(score.nextMove)
    Text("Windows", fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    score.windows.forEach { window ->
        WindowRow(window.label, window.rate, accent)
    }
    MomentumBlock(report.momentum, accent)
}

@Composable
private fun MomentumBlock(momentum: StatsInsightEngine.Momentum, accent: Color) {
    Spacer(Modifier.height(4.dp))
    Eyebrow("MOMENTUM")
    Text(
        momentum.headline,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(momentum.label, if (momentum.rising) SheetMint else SheetCoral)
        Text(
            "${if (momentum.rising) "+" else ""}${momentum.deltaPct}% vs last week",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (momentum.rising) SheetMint else SheetCoral,
        )
    }
    Sparkline(momentum.spark28, if (momentum.rising) accent else SheetCoral)
    Story(momentum.story)
    NextMove(momentum.nextMove)
}

@Composable
private fun WeekExplore(report: StatsInsightEngine.Report, accent: Color) {
    val week = report.week
    Eyebrow("THIS WEEK")
    Text(
        week.headline,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${(week.rate * 100).roundToInt()}%", "planted", accent)
        MiniStat("${week.done}/${week.due}", "marks", SheetMint)
        MiniStat("${week.perfectDays}", "perfect", SheetGold)
    }
    Story(week.story)
    week.days.forEach { day ->
        DayLine(day, accent)
    }
    if (week.days.lastOrNull()?.missed?.isNotEmpty() == true) {
        GlowNote(
            "Still open today: ${week.days.last().missed.joinToString(", ")}",
            SheetCoral,
            Icons.Rounded.PriorityHigh,
        )
    } else if (week.rate >= 0.8f) {
        GlowNote("This week is a congratulations week. Hold the close.", SheetGold, Icons.Rounded.AutoAwesome)
    }
    NextMove(week.nextMove)
}

@Composable
private fun TrendExplore(
    metrics: StatsEngine.DashboardMetrics,
    report: StatsInsightEngine.Report,
    accent: Color,
) {
    Eyebrow("8-WEEK TREND")
    Text(
        report.momentum.headline,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        metrics.weeklyTrend.forEach { point ->
            val h by animateFloatAsState(point.rate, spring(dampingRatio = 0.7f), label = "t_${point.label}")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(point.rate * 100).roundToInt()}",
                    fontSize = 9.sp,
                    fontFamily = jetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (point.isCurrent) accent else muted(),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height((120 * h.coerceAtLeast(0.06f)).dp)
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                        .background(
                            if (point.isCurrent) accent
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(point.label, fontSize = 10.sp, color = if (point.isCurrent) accent else muted())
            }
        }
    }
    Story(report.momentum.story)
    NextMove(report.momentum.nextMove)
}

@Composable
private fun RhythmExplore(
    metrics: StatsEngine.DashboardMetrics,
    report: StatsInsightEngine.Report,
    accent: Color,
) {
    val rhythm = report.rhythm
    Eyebrow("YOUR RHYTHM")
    Text(
        rhythm.headline,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${(rhythm.weekdayRate * 100).roundToInt()}%", "weekdays", accent)
        MiniStat("${(rhythm.weekendRate * 100).roundToInt()}%", "weekends", SheetPurple)
    }
    metrics.weekdayRhythm.forEach { point ->
        WindowRow(point.label, point.rate, if (point.label == rhythm.strongest) SheetGold else accent)
    }
    Story(rhythm.story)
    NextMove(rhythm.nextMove)
}

@Composable
private fun MetricExplore(story: StatsInsightEngine.MetricStory?, accent: Color) {
    if (story == null) return
    Eyebrow("STAT")
    Text(story.title, fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp)
    Text(
        story.value,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        color = accent,
    )
    Story(story.story)
    story.facts.forEach { (k, v) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(k, fontFamily = dmSansFamily, color = muted(), fontSize = 13.sp)
            Text(v, fontFamily = jetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
    story.congratulations?.let { GlowNote(it, SheetGold, Icons.Rounded.AutoAwesome) }
    NextMove(story.nextMove)
}

@Composable
private fun HabitExplore(dive: StatsInsightEngine.HabitDeepDive, accent: Color) {
    Eyebrow("HABIT · #${dive.rank}")
    Text(
        dive.headline,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(dive.verdict, accent)
        Pill("${dive.scorePct}% score", SheetMint)
        if (dive.missStreak > 0) Pill("${dive.missStreak} miss", SheetCoral)
    }
    Sparkline(dive.spark30, accent)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("${dive.consistency30}%", "30 days", accent)
        MiniStat("${dive.consistency90}%", "90 days", SheetPurple)
        MiniStat("${dive.streak}d", "streak", SheetGold)
    }
    Story(dive.story)
    dive.congratulations?.let { GlowNote(it, SheetGold, Icons.Rounded.AutoAwesome) }
    dive.leak?.let { GlowNote(it, SheetCoral, Icons.Rounded.PriorityHigh) }
    Fact("Best weekday", dive.bestWeekday ?: ".")
    Fact("Softest weekday", dive.worstWeekday ?: ".")
    Fact("Personal best streak", "${dive.bestStreak} days")
    Fact("All-time marks", "${dive.total}")
    NextMove(dive.nextMove)
}

@Composable
private fun SignalExplore(signal: StatsInsightEngine.Signal, accent: Color) {
    Eyebrow(signal.kind.name)
    Text(
        signal.title,
        fontFamily = frauncesFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )
    Story(signal.body)
    signal.action?.let { NextMove(it) }
}

@Composable
private fun DayLine(day: StatsInsightEngine.DayDetail, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (day.isToday) accent.copy(0.35f) else MaterialTheme.colorScheme.onBackground.copy(0.06f),
                RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (day.isToday) "Today · ${day.label}" else day.label,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                if (day.due == 0) "off" else "${day.done}/${day.due}",
                fontFamily = jetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = if (day.due > 0 && day.missed.isEmpty()) SheetMint else muted(),
            )
        }
        if (day.completed.isNotEmpty()) {
            Text(
                "Planted  ${day.completed.joinToString(" · ")}",
                fontSize = 12.sp,
                color = SheetMint,
                lineHeight = 16.sp,
            )
        }
        if (day.missed.isNotEmpty()) {
            Text(
                "Open  ${day.missed.joinToString(" · ")}",
                fontSize = 12.sp,
                color = SheetCoral,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun PointBar(label: String, value: Int, max: Int, color: Color) {
    val anim by animateFloatAsState(
        targetValue = if (max == 0) 0f else value / max.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = label,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, fontFamily = dmSansFamily, color = muted())
            Text("$value/$max", fontSize = 11.sp, fontFamily = jetBrainsMonoFamily, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anim.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color),
            )
        }
    }
}

@Composable
private fun WindowRow(label: String, rate: Float, accent: Color) {
    val anim by animateFloatAsState(rate, tween(800, easing = FastOutSlowInEasing), label = label)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, modifier = Modifier.width(76.dp), fontSize = 13.sp, fontFamily = dmSansFamily)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anim.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(accent),
            )
        }
        Text(
            "${(rate * 100).roundToInt()}%",
            fontFamily = jetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
private fun Sparkline(data: List<Float>, color: Color) {
    val track = MaterialTheme.colorScheme.onBackground.copy(0.08f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
    ) {
        if (data.size < 2) return@Canvas
        val w = size.width / (data.size - 1)
        val h = size.height
        val pts = data.mapIndexed { i, v -> Offset(i * w, h - v.coerceIn(0f, 1f) * h * 0.88f - 4f) }
        val fill = Path().apply {
            moveTo(pts.first().x, h)
            lineTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
            lineTo(pts.last().x, h)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.16f))
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(track, Offset(0f, h - 2f), Offset(size.width, h - 2f), 1.dp.toPx())
    }
}

@Composable
private fun ExploreRing(rate: Float, color: Color, size: androidx.compose.ui.unit.Dp) {
    val animated by animateFloatAsState(rate.coerceIn(0f, 1f), tween(1200, easing = FastOutSlowInEasing), label = "er")
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    Canvas(Modifier.size(size)) {
        val stroke = 9.dp.toPx()
        val pad = stroke / 2f
        val arc = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(track, -90f, 360f, false, Offset(pad, pad), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        if (animated > 0f) {
            drawArc(
                color,
                -90f,
                360f * animated,
                false,
                Offset(pad, pad),
                arc,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text,
        fontFamily = jetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.8.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
    )
}

@Composable
private fun Story(text: String) {
    Text(
        text,
        fontFamily = dmSansFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.82f),
    )
}

@Composable
private fun GlowNote(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.28f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, fontFamily = dmSansFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun NextMove(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.TipsAndUpdates, null, tint = SheetPurple, modifier = Modifier.size(18.dp))
        Column {
            Text(
                "WHAT TO DO NEXT",
                fontFamily = jetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = SheetPurple,
            )
            Spacer(Modifier.height(4.dp))
            Text(text, fontFamily = dmSansFamily, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = muted())
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = muted())
        Text(value, fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun muted() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
