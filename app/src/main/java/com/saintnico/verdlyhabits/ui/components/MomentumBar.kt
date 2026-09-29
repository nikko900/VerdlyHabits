package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thin momentum bar under the greeting. Fills as habits are completed today.
 */
@Composable
fun MomentumBar(
    progress: Float,
    xpToday: Int,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "momentum",
    )

    val isDark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val gold = Color(0xFFD4890A)
    val onBg = MaterialTheme.colorScheme.onBackground
    val trackAlpha = if (isDark) 0.10f else 0.16f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Today's momentum",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = onBg.copy(alpha = if (isDark) 0.58f else 0.72f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+$xpToday XP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isDark) 7.dp else 9.dp)
                .clip(RoundedCornerShape(50))
                .background(onBg.copy(alpha = trackAlpha)),
        ) {
            if (animated > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Mint.copy(alpha = 0.9f),
                                    primary,
                                    gold,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

private val Mint = Color(0xFF52B788)
