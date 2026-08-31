package com.saintnico.verdlyhabits.ui.components.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.engine.StatsEngine
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.components.CountUpText
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.models.toHabitColor
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val Mint = Color(0xFF52B788)
private val Teal = Color(0xFF2DD4BF)
private val Gold = Color(0xFFFFB300)
private val Purple = Color(0xFF7B6CF6)
private val Coral = Color(0xFFFF6B6B)

private fun scoreColor(rate: Float): Color = when {
    rate >= 0.8f -> Mint
    rate >= 0.5f -> Teal
    rate >= 0.25f -> Gold
    else -> Coral
}

@Composable
private fun isLight() = !isSystemInDarkTheme()

@Composable
private fun onMuted(alphaLight: Float = 0.58f, alphaDark: Float = 0.55f): Color =
    MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLight()) alphaLight else alphaDark)

/** Soft panel — border + wash only. No elevation, no Material Card. */
@Composable
private fun SoftPanel(
    modifier: Modifier = Modifier,
    accent: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val light = isLight()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = if (light) 0.12f else 0.14f),
                        Purple.copy(alpha = if (light) 0.05f else 0.07f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(
                1.dp,
                accent.copy(alpha = if (light) 0.18f else 0.20f),
                RoundedCornerShape(24.dp),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        content = { content() },
    )
}

/** Today hero — colorful ring + big % + metric tiles. */
@Composable
fun HomeTodayHeroCard(
    completionRate: Float,
    completedCount: Int,
    totalCount: Int,
    totalStreak: Int,
    consistencyScore: Int,
    weekDeltaPct: Int,
    weekTrendUp: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = scoreColor(completionRate)
    val consistencyColor = scoreColor(consistencyScore / 100f)

    SoftPanel(modifier = modifier, accent = accent, onClick = onTap) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Today",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = onMuted(0.62f, 0.55f),
                    )
                    Spacer(Modifier.height(4.dp))
                    CountUpText(
                        targetValue = (completionRate * 100).toInt(),
                        suffix = "%",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$completedCount of $totalCount habits done",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = onMuted(0.68f, 0.62f),
                    )
                }
                HomeProgressRing(progress = completionRate, accent = accent, size = 92.dp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroMiniMetric(
                    modifier = Modifier.weight(1f),
                    icon = { StreakFlame(streak = totalStreak.coerceAtLeast(1), size = 18.dp) },
                    value = "$totalStreak",
                    label = "Streak",
                    color = Coral,
                )
                HeroMiniMetric(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = consistencyColor, modifier = Modifier.size(16.dp))
                    },
                    value = "$consistencyScore",
                    label = "Consistency",
                    color = consistencyColor,
                )
                HeroMiniMetric(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            if (weekTrendUp) Icons.AutoMirrored.Rounded.TrendingUp
                            else Icons.AutoMirrored.Rounded.TrendingDown,
                            null,
                            tint = if (weekTrendUp) Mint else Coral,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    value = "${if (weekTrendUp) "+" else ""}$weekDeltaPct%",
                    label = "This week",
                    color = if (weekTrendUp) Mint else Coral,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLight()) 0.05f else 0.04f))
                    .clickable(onClick = onTap)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.QueryStats, null, tint = Purple.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Open full dashboard",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = onMuted(0.78f, 0.78f),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = onMuted(0.40f, 0.38f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeProgressRing(progress: Float, accent: Color, size: Dp) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "home_ring",
    )
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLight()) 0.12f else 0.08f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val stroke = 9.dp.toPx()
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
                    brush = Brush.sweepGradient(listOf(accent, Gold, accent)),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (animated >= 1f) {
                Icon(Icons.Rounded.Check, null, tint = accent, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "${(animated * 100).toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = accent,
                )
            }
            Text(
                "today",
                fontSize = 10.sp,
                color = onMuted(0.50f, 0.45f),
            )
        }
    }
}

@Composable
private fun HeroMiniMetric(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    color: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isLight()) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
            )
            .border(1.dp, color.copy(alpha = if (isLight()) 0.18f else 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = onMuted(0.55f, 0.48f),
        )
    }
}

@Composable
fun HomeWeekPulseStrip(
    dailyPulse: List<StatsEngine.DayPulse>,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val completedDays = dailyPulse.count { it.rate >= 1f }
    val avgRate = if (dailyPulse.isEmpty()) 0f else dailyPulse.map { it.rate }.average().toFloat()
    val light = isLight()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = if (light) 0.08f else 0.10f),
                RoundedCornerShape(22.dp),
            )
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Your week",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "$completedDays perfect · ${(avgRate * 100).toInt()}% average",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = onMuted(0.62f, 0.52f),
                    )
                }
                Text(
                    "Stats →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                dailyPulse.forEach { day ->
                    WeekDayRing(day = day, accent = accent)
                }
            }
        }
    }
}

@Composable
private fun WeekDayRing(day: StatsEngine.DayPulse, accent: Color) {
    val ringColor = when {
        day.rate >= 1f -> accent
        day.rate >= 0.5f -> Mint
        day.rate > 0f -> Gold
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLight()) 0.28f else 0.18f)
    }
    val outer = if (day.isToday) 42.dp else 36.dp
    val inner = if (day.isToday) 34.dp else 30.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            day.label,
            fontSize = 11.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                day.isToday -> accent
                day.rate >= 1f -> accent.copy(alpha = 0.88f)
                else -> onMuted(0.62f, 0.48f)
            },
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(outer)
                .then(
                    if (day.isToday) {
                        Modifier
                            .clip(CircleShape)
                            .background(accent.copy(alpha = if (isLight()) 0.08f else 0.10f))
                            .border(2.dp, accent, CircleShape)
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            PulseRing(rate = day.rate, color = ringColor, size = inner)
            if (day.rate >= 1f) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PulseRing(rate: Float, color: Color, size: Dp) {
    val animated by animateFloatAsState(rate.coerceIn(0f, 1f), spring(), label = "pulse")
    val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLight()) 0.18f else 0.10f)
    val strokeWidth = if (isLight()) 4.dp else 3.5.dp

    Canvas(Modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val pad = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = trackColor,
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

@Composable
fun HomeTodayFocusCard(
    habit: HabitItem,
    onTapComplete: () -> Unit,
    onTapFocus: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitColor = habit.color.toHabitColor()
    val light = isLight()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        habitColor.copy(alpha = if (light) 0.12f else 0.14f),
                        Gold.copy(alpha = if (light) 0.05f else 0.06f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(
                1.dp,
                habitColor.copy(alpha = if (light) 0.22f else 0.28f),
                RoundedCornerShape(22.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Today's focus",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = habitColor,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(habitColor.copy(0.20f), habitColor.copy(0.06f)),
                            ),
                        )
                        .border(1.dp, habitColor.copy(0.18f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedHabitIcon(icon = habit.icon, color = habitColor, size = 26.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        habit.title,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StreakFlame(streak = habit.streak.coerceAtLeast(1), size = 14.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${habit.streak}-day streak · ${habit.difficulty.displayName}",
                            fontSize = 12.sp,
                            color = onMuted(0.62f, 0.58f),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTapComplete,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = habitColor),
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Complete", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onTapFocus,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(18.dp), tint = habitColor)
                    Spacer(Modifier.width(6.dp))
                    Text("Focus", fontWeight = FontWeight.SemiBold, color = habitColor)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.MoreHoriz, null, tint = onMuted(0.58f, 0.55f))
                }
            }
        }
    }
}

@Composable
fun HomeSectionHeader(
    title: String,
    badge: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (highlight) Mint.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLight()) 0.07f else 0.06f),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Mint else onMuted(0.55f, 0.52f),
            )
        }
    }
}

@Composable
fun HomeCategoryFilterRow(
    selected: HabitCategory?,
    onSelect: (HabitCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(HabitCategory.entries.toTypedArray()) { cat ->
            val isOn = cat == selected
            Surface(
                onClick = { onSelect(cat) },
                shape = RoundedCornerShape(50),
                color = if (isOn) cat.color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOn) cat.color.copy(0.45f) else MaterialTheme.colorScheme.onBackground.copy(if (isLight()) 0.10f else 0.08f),
                ),
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(cat.color),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        cat.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isOn) FontWeight.Bold else FontWeight.Medium,
                        color = if (isOn) cat.color else onMuted(0.70f, 0.65f),
                    )
                }
            }
        }
    }
}
