package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * GitHub-style activity heatmap showing habit completions over the past 3 months.
 * Each cell represents a day; color intensity reflects number of habits completed.
 */
@Composable
fun HabitHeatmap(
    completionData: Map<String, Int>, // "YYYY-MM-DD" -> number of completions
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    weeks: Int = 13 // ~3 months
) {
    val today = LocalDate.now()
    val startDate = today.minusWeeks(weeks.toLong() - 1)
        .with(DayOfWeek.MONDAY) // Align to Monday
    val maxCompletions = completionData.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val emptyColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)

    val dayLabels = listOf("M", "", "W", "", "F", "", "S")

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            // Day labels column
            Column(
                modifier = Modifier.padding(end = 4.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (label.isNotEmpty()) {
                            Text(label, fontSize = 7.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            // Grid
            Column {
                // Month labels
                Row(
                    modifier = Modifier.padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    var currentMonth = -1
                    for (week in 0 until weeks) {
                        val weekDate = startDate.plusWeeks(week.toLong())
                        val month = weekDate.monthValue
                        Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                            if (month != currentMonth) {
                                currentMonth = month
                                Text(
                                    weekDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                                    fontSize = 7.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                // Heatmap cells
                for (dayOfWeek in 0 until 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (week in 0 until weeks) {
                            val date = startDate.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                            val dateStr = date.toString()
                            val count = completionData[dateStr] ?: 0
                            val isFuture = date.isAfter(today)

                            val cellColor = when {
                                isFuture -> Color.Transparent
                                count == 0 -> emptyColor
                                else -> {
                                    val intensity = (count.toFloat() / maxCompletions).coerceIn(0.15f, 1f)
                                    accentColor.copy(alpha = intensity)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
        }

        // Legend
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            Spacer(Modifier.width(4.dp))
            listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { level ->
                Box(
                    modifier = Modifier.size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (level == 0f) emptyColor
                            else accentColor.copy(alpha = level.coerceAtLeast(0.15f))
                        )
                )
                Spacer(Modifier.width(2.dp))
            }
            Text("More", fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}
