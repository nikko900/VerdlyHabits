package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.min
import kotlin.random.Random

private val grainOffsets = List(140) { Random(91L + it).nextFloat() * 5f }
private val hazeSeeds = List(18) {
    Pair(Random(210L + it).nextFloat(), Random(420L + it).nextFloat())
}

/**
 * Multi-layer immersive background for auth screens — gradient, breathing blooms,
 * soft haze, vignette, and fine grain for a more tactile, professional feel.
 */
@Composable
fun Modifier.immersiveAuthBackground(): Modifier {
    val infinite = rememberInfiniteTransition(label = "authBg")
    val breathe by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val shift by infinite.animateFloat(
        initialValue = -0.015f,
        targetValue = 0.015f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val top = Color(0xFF0A1A0A)
    val mid = Color(0xFF0C160F)
    val bottom = Color(0xFF050D05)
    val bloom = Color(0xFF1D6B44)
    val goldMist = Color(0xFFD4890A)
    return this
        .drawBehind {
            val brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to top,
                    (0.42f * breathe + shift).coerceIn(0.18f, 0.72f) to mid,
                    1f to bottom
                )
            )
            drawRect(brush)
        }
        .drawBehind {
            // Primary canopy bloom (top)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bloom.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * (0.48f + shift), size.height * 0.16f),
                    radius = size.width * 0.92f * breathe
                )
            )
            // Secondary low bloom (grounds the CTA area)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bloom.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.72f, size.height * 0.78f),
                    radius = size.width * 0.7f
                )
            )
            // Quiet gold accent — professionalism without neon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(goldMist.copy(alpha = 0.045f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * (0.55f + drift * 0.04f)),
                    radius = size.width * 0.55f
                )
            )
        }
        .drawBehind {
            // Soft atmospheric haze pockets
            hazeSeeds.forEachIndexed { i, (nx, ny) ->
                val cx = size.width * (nx * 0.9f + 0.05f + shift * 0.3f)
                val cy = size.height * (ny * 0.85f + 0.08f + drift * 0.02f * (if (i % 2 == 0) 1 else -1))
                drawCircle(
                    color = Color.White.copy(alpha = 0.012f + (i % 3) * 0.004f),
                    radius = min(size.width, size.height) * (0.04f + (i % 5) * 0.012f),
                    center = Offset(cx, cy)
                )
            }
        }
        .drawBehind {
            // Edge vignette for depth
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    center = Offset(size.width * 0.5f, size.height * 0.45f),
                    radius = size.maxDimension * 0.72f
                )
            )
        }
        .drawBehind {
            val cols = 12
            val rows = 12
            val stepX = size.width / cols
            val stepY = size.height / rows
            repeat(cols * rows) { i ->
                val ox = grainOffsets[i % grainOffsets.size]
                val oy = grainOffsets[(i + 23) % grainOffsets.size]
                drawCircle(
                    color = Color.White.copy(alpha = if (i % 7 == 0) 0.028f else 0.014f),
                    radius = if (i % 11 == 0) 1.35f else 0.9f,
                    center = Offset((i % cols) * stepX + ox, (i / cols) * stepY + oy)
                )
            }
        }
}
