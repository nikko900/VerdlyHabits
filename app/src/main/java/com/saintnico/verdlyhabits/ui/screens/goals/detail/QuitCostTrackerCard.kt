package com.saintnico.verdlyhabits.ui.screens.goals.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@Composable
fun QuitCostTrackerCard(
    cost: GoalProgressEngine.CostSavedMetrics,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            accent.copy(alpha = 0.22f),
            Color(0xFF26A69A).copy(alpha = 0.14f),
        ),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "What you're reclaiming",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CostStatTile(
                    label = if (cost.cleanDays > 0) "Total so far" else "Per clear day",
                    value = if (cost.isMoney) {
                        "~${formatCost(displayAmount(cost))}"
                    } else {
                        "~${formatCost(displayAmount(cost))} ${cost.unit}"
                    },
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                CostStatTile(
                    label = "Clear days",
                    value = if (cost.cleanDays == 1) "1 day" else "${cost.cleanDays} days",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }

            cost.monthlyProjection?.let { monthly ->
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (cost.isMoney) {
                            "At this pace: ~${formatCost(monthly)} / month back in your life"
                        } else {
                            "At this pace: ~${formatCost(monthly)} ${cost.unit} / month reclaimed"
                        },
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CostStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .padding(14.dp),
    ) {
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = accent,
        )
    }
}

private fun displayAmount(cost: GoalProgressEngine.CostSavedMetrics): Float =
    if (cost.cleanDays > 0) cost.totalSaved else cost.perOccurrence

private fun formatCost(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else String.format("%.1f", v)
