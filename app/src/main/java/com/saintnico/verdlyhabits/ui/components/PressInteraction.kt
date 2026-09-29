package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.95f
): Pair<MutableInteractionSource, Modifier> {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(120),
        label = "pressScale"
    )
    return interactionSource to Modifier.scale(scale)
}
