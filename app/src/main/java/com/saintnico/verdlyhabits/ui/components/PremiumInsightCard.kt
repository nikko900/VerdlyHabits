package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.MotivationalEngine
import com.saintnico.verdlyhabits.engine.MotivationalEngine.InsightKind

/**
 * A premium insight card with a gradient background, shimmer border, and subtle
 * glow pulse for highlight insights. This replaces the basic InsightCard on the
 * Home Screen and appears in the Stats Screen hero block.
 */
@Composable
fun PremiumInsightCard(
    insight: MotivationalEngine.Insight,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val gradientColors = when (insight.kind) {
        InsightKind.FIRE -> listOf(Color(0xFFFF6B35), Color(0xFFFFB300), Color(0xFFFF6B35))
        InsightKind.TROPHY -> listOf(Color(0xFF7B6CF6), Color(0xFF2DD4BF), Color(0xFF7B6CF6))
        InsightKind.TREND -> listOf(Color(0xFF2DD4BF), Color(0xFF4CAF50), Color(0xFF2DD4BF))
        InsightKind.CLOCK -> listOf(Color(0xFFF59E0B), Color(0xFFFFD54F), Color(0xFFF59E0B))
        InsightKind.CHAIN -> listOf(Color(0xFF7B6CF6), Color(0xFFF59E0B), Color(0xFF7B6CF6))
        InsightKind.INFO -> listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary
        )
    }

    val icon = when (insight.kind) {
        InsightKind.FIRE -> Icons.Rounded.LocalFireDepartment
        InsightKind.TROPHY -> Icons.Rounded.EmojiEvents
        InsightKind.TREND -> Icons.Rounded.QueryStats
        InsightKind.CLOCK -> Icons.Rounded.Schedule
        InsightKind.CHAIN -> Icons.Rounded.Link
        InsightKind.INFO -> Icons.Rounded.TipsAndUpdates
    }

    val iconTint = gradientColors.first()

    // Glow pulse for highlight insights
    val glowAlpha = if (insight.isHighlight) {
        val infinite = rememberInfiniteTransition(label = "insight_glow")
        val pulse by infinite.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "insight_glow_alpha"
        )
        pulse
    } else {
        0.08f
    }

    ShimmerGradientBorder(
        modifier = modifier.fillMaxWidth(),
        gradientColors = gradientColors,
        cornerRadius = 18.dp,
        borderWidth = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = glowAlpha),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with glow
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = when (insight.kind) {
                        InsightKind.FIRE -> "ON FIRE"
                        InsightKind.TROPHY -> "MILESTONE"
                        InsightKind.TREND -> "TRENDING UP"
                        InsightKind.CLOCK -> "PRO TIP"
                        InsightKind.CHAIN -> "STREAK"
                        InsightKind.INFO -> "INSIGHT"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = iconTint,
                    letterSpacing = 1.5.sp,
                    fontSize = 9.sp
                )
                Text(
                    text = insight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Close, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}
