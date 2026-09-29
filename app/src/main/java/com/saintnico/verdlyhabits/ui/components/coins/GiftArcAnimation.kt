package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Coins arcing from sender to receiver — under 1.2s, restrained motion.
 * [premium] selects richer particle count and a subtle trail.
 */
@Composable
fun GiftArcAnimation(
    premium: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val particleCount = if (premium) 6 else 3

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val t = progress.value
            val start = Offset(size.width * 0.22f, size.height * 0.72f)
            val end = Offset(size.width * 0.78f, size.height * 0.28f)
            val ctrl = Offset(size.width * 0.5f, size.height * 0.08f)

            if (premium && t > 0.05f) {
                val trail = Path().apply {
                    moveTo(start.x, start.y)
                    quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y)
                }
                drawPath(
                    trail,
                    color = androidx.compose.ui.graphics.Color(0xFF52B788).copy(alpha = 0.12f * (1f - t)),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }

            repeat(particleCount) { i ->
                val stagger = i * 0.08f
                val localT = ((t - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                if (localT <= 0f) return@repeat
                val u = localT
                val x = (1 - u) * (1 - u) * start.x + 2 * (1 - u) * u * ctrl.x + u * u * end.x
                val y = (1 - u) * (1 - u) * start.y + 2 * (1 - u) * u * ctrl.y + u * u * end.y +
                    sin(localT * Math.PI).toFloat() * size.height * 0.02f
                drawCoinAt(
                    premium = premium,
                    alpha = (1f - localT).coerceIn(0.15f, 1f),
                    center = Offset(x, y),
                    radius = size.minDimension * (if (premium) 0.028f else 0.022f),
                )
            }
        }
    }
}
