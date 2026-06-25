package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Loop-style 30-day completion strip — one column per day, color intensity from
 * the completion rate that day. Dashboard variant: small, fixed height, no labels.
 *
 *  - Today is the rightmost column.
 *  - Empty days use the surface tint instead of pure transparency so the strip
 *    keeps its shape even when the user has just started.
 */
@Composable
fun MiniHeatmapStrip(
    habits: List<HabitItem>,
    modifier: Modifier = Modifier,
    days: Int = 30
) {
    val fmt = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val today = remember { LocalDate.now() }
    val active = remember(habits) { habits.filter { !it.isArchived } }
    val totalActive = active.size.coerceAtLeast(1)

    val rates = remember(habits, today) {
        (days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val key = date.format(fmt)
            val done = active.count { it.completedDates.contains(key) }
            done.toFloat() / totalActive
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last $days days",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
            val streak = rates.takeLastWhile { it > 0f }.size
            if (streak > 0) {
                Text(
                    text = "$streak in a row",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val gapPx = 3.dp.toPx()
            val cellWidth = (size.width - gapPx * (days - 1)) / days
            val h = size.height
            rates.forEachIndexed { index, rate ->
                val alpha = when {
                    rate <= 0f -> 0f
                    rate < 0.25f -> 0.30f
                    rate < 0.5f -> 0.55f
                    rate < 0.75f -> 0.80f
                    else -> 1f
                }
                val color = if (alpha == 0f) track else primary.copy(alpha = alpha)
                val x = index * (cellWidth + gapPx)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(cellWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
    }
}

/** Helper for non-Compose callers (e.g. widgets). Not currently used in dashboard. */
fun computeDailyCompletionRates(
    habits: List<HabitItem>,
    days: Int = 30,
    today: LocalDate = LocalDate.now()
): List<Float> {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val active = habits.filter { !it.isArchived }
    val totalActive = active.size.coerceAtLeast(1)
    return (days - 1 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        val key = date.format(fmt)
        val done = active.count { it.completedDates.contains(key) }
        done.toFloat() / totalActive
    }
}

@Suppress("unused")
private val PreviewColor = Color(0xFF1D6B44)
