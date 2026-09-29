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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class BurstParticle(
    val angleRad: Float,
    val distance: Float,
    val scale: Float,
    val spin: Float,
)

/**
 * Restrained coin burst after a successful pack purchase — coins arc out from
 * the center and fade. Runs once (~900ms), then [onFinished].
 */
@Composable
fun CoinBurstAnimation(
    premium: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val particles = remember {
        val rng = Random(System.currentTimeMillis())
        List(14) { i ->
            BurstParticle(
                angleRad = (i / 14f) * (2f * Math.PI.toFloat()) + rng.nextFloat() * 0.3f,
                distance = 0.22f + rng.nextFloat() * 0.16f,
                scale = 0.5f + rng.nextFloat() * 0.6f,
                spin = rng.nextFloat() * 40f - 20f,
            )
        }
    }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = progress.value
            val minDim = size.minDimension
            val origin = Offset(size.width / 2f, size.height / 2f)
            val alpha = (1f - t).coerceIn(0f, 1f)
            particles.forEach { p ->
                val travelled = p.distance * minDim * t
                val x = origin.x + cos(p.angleRad) * travelled
                // Slight gravity dip on the tail of the arc
                val y = origin.y + sin(p.angleRad) * travelled + (t * t) * minDim * 0.06f
                drawCoinAt(
                    premium = premium,
                    alpha = alpha,
                    center = Offset(x, y),
                    radius = minDim * 0.028f * p.scale,
                )
            }
        }
    }
}
