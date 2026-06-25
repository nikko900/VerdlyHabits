package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium animated wave-hand icon for the greeting header.
 *
 * On entry, the hand performs a natural waving motion via rotation keyframes:
 *     0° → 20° → -10° → 15° → -5° → 0°
 *
 * The animation runs once on first composition, giving a warm, personal touch
 * without being distracting on repeated visits.
 */
@Composable
fun WavingHandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = Color(0xFFFFB300)
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 1000
                0f at 0 using FastOutSlowInEasing
                20f at 150 using FastOutSlowInEasing
                -10f at 350 using FastOutSlowInEasing
                15f at 500 using FastOutSlowInEasing
                -5f at 700 using FastOutSlowInEasing
                0f at 1000 using FastOutSlowInEasing
            }
        )
    }

    Icon(
        imageVector = Icons.Rounded.WavingHand,
        contentDescription = "Wave",
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation.value
                // Pivot from the "wrist" (bottom-center) for natural wave
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.7f, 0.9f)
            }
    )
}
