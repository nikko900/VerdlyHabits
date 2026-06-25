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
import androidx.compose.ui.graphics.lerp
import kotlin.random.Random

private val grainOffsets = List(100) { Random(91L + it).nextFloat() * 4f }

/**
 * Three-layer immersive background for auth screens (matches onboarding spirit).
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
    val top = Color(0xFF0A1A0A)
    val bottom = Color(0xFF050D05)
    val bloom = Color(0xFF1D6B44)
    return this
        .drawBehind {
            val brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to top,
                    (0.45f * breathe + shift).coerceIn(0.2f, 0.8f) to lerp(top, bottom, 0.5f),
                    1f to bottom
                )
            )
            drawRect(brush)
        }
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(bloom.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.18f),
                    radius = size.width * 0.85f
                )
            )
        }
        .drawBehind {
            val stepX = size.width / 10f
            val stepY = size.height / 10f
            repeat(100) { i ->
                val ox = grainOffsets[i]
                val oy = grainOffsets[(i + 19) % 100]
                drawCircle(
                    color = Color.White.copy(alpha = 0.018f),
                    radius = 1f,
                    center = Offset((i % 10) * stepX + ox, (i / 10) * stepY + oy)
                )
            }
        }
}
