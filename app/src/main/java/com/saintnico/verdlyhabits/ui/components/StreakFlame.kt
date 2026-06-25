package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Canvas-drawn streak flame.
 *
 *  - Color shifts gray → amber → red as the streak gets longer.
 *  - A subtle pulse animation runs continuously, faster as the streak grows.
 *  - The flame itself is built from two stacked bezier "petals" — outer (warm) +
 *    inner (white-hot core) — to give depth without using a vector asset.
 *
 * Designed to feel premium-trophy-like, not a generic icon.
 */
@Composable
fun StreakFlame(
    streak: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showLabel: Boolean = false
) {
    val tier = flameTier(streak)
    val (outer, inner, glow) = tier.colors

    val infinite = rememberInfiniteTransition(label = "flame_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = tier.pulseMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse_val"
    )
    val flicker by infinite.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_flicker"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            // Glow halo
            if (streak > 0) {
                drawCircle(
                    color = glow.copy(alpha = 0.18f),
                    radius = w * 0.55f * pulse,
                    center = Offset(cx, h * 0.55f)
                )
                drawCircle(
                    color = glow.copy(alpha = 0.10f),
                    radius = w * 0.75f * pulse,
                    center = Offset(cx, h * 0.55f)
                )
            }

            // Outer flame petal
            val outerPath = Path().apply {
                moveTo(cx, h * 0.03f)
                cubicTo(
                    cx + w * 0.05f + flicker, h * 0.18f,
                    cx + w * 0.42f, h * 0.32f,
                    cx + w * 0.40f, h * 0.62f
                )
                cubicTo(
                    cx + w * 0.40f, h * 0.92f,
                    cx - w * 0.40f, h * 0.92f,
                    cx - w * 0.40f, h * 0.62f
                )
                cubicTo(
                    cx - w * 0.42f, h * 0.32f,
                    cx - w * 0.05f - flicker, h * 0.18f,
                    cx, h * 0.03f
                )
                close()
            }
            drawPath(
                path = outerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(outer, outer.copy(alpha = 0.85f), inner.copy(alpha = 0.65f)),
                    startY = h * 0.0f,
                    endY = h * 0.95f
                )
            )

            // Inner core
            val innerPath = Path().apply {
                moveTo(cx, h * 0.28f)
                cubicTo(
                    cx + w * 0.18f, h * 0.42f,
                    cx + w * 0.22f, h * 0.62f,
                    cx, h * 0.85f
                )
                cubicTo(
                    cx - w * 0.22f, h * 0.62f,
                    cx - w * 0.18f, h * 0.42f,
                    cx, h * 0.28f
                )
                close()
            }
            drawPath(
                path = innerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), inner, inner.copy(alpha = 0.85f)),
                    startY = h * 0.28f,
                    endY = h * 0.85f
                )
            )

            // Thin highlight rim
            drawPath(
                path = outerPath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = 1.5f)
            )
        }

        if (showLabel) {
            Text(
                streak.toString(),
                fontSize = (size.value * 0.30f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
            )
        }
    }
}

private data class FlameTier(
    val colors: FlameColors,
    val pulseMs: Int
)

private data class FlameColors(
    val outer: Color,
    val inner: Color,
    val glow: Color
)

private fun flameTier(streak: Int): FlameTier = when {
    streak <= 0   -> FlameTier(FlameColors(Color(0xFF8E8E8E), Color(0xFFB0B0B0), Color(0xFFCFCFCF)), pulseMs = 1600)
    streak < 3    -> FlameTier(FlameColors(Color(0xFFFFB74D), Color(0xFFFFE082), Color(0xFFFFD54F)), pulseMs = 1300)
    streak < 7    -> FlameTier(FlameColors(Color(0xFFFFA726), Color(0xFFFFD54F), Color(0xFFFFB300)), pulseMs = 1100)
    streak < 14   -> FlameTier(FlameColors(Color(0xFFFF7043), Color(0xFFFFB74D), Color(0xFFFF8A65)), pulseMs = 900)
    streak < 30   -> FlameTier(FlameColors(Color(0xFFE53935), Color(0xFFFF8A65), Color(0xFFEF5350)), pulseMs = 750)
    streak < 100  -> FlameTier(FlameColors(Color(0xFFD32F2F), Color(0xFFFF7043), Color(0xFFEF5350)), pulseMs = 600)
    else          -> FlameTier(FlameColors(Color(0xFFB71C1C), Color(0xFFFF5252), Color(0xFFE040FB)), pulseMs = 500)
}
