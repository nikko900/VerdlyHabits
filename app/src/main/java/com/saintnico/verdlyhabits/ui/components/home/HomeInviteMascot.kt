package com.saintnico.verdlyhabits.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val WaveGold = Color(0xFFFFE8A3)

/**
 * Waving hand slides left from the Verdly title, comes back, then the invite opens.
 */
@Composable
fun HomeInviteWaveOverlay(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val slide = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }
    val scale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
        slide.animateTo(
            targetValue = -46f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
        wave.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 980
                0f at 0 using FastOutSlowInEasing
                22f at 140 using FastOutSlowInEasing
                -16f at 320 using FastOutSlowInEasing
                18f at 500 using FastOutSlowInEasing
                -10f at 680 using FastOutSlowInEasing
                12f at 840 using FastOutSlowInEasing
                0f at 980 using FastOutSlowInEasing
            },
        )
        slide.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
        delay(120)
        alpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                translationX = slide.value
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.WavingHand,
            contentDescription = null,
            tint = WaveGold,
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer {
                    rotationZ = wave.value
                    transformOrigin = TransformOrigin(0.7f, 0.9f)
                },
        )
    }
}
