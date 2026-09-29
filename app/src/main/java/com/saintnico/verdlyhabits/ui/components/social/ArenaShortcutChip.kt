package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.saintnico.verdlyhabits.ui.navigation.ArenaConjoinedIcon

private val RiskAmber = Color(0xFFE08A2E)

/** Quiet Home header chip — loud only when duo is at risk. */
@Composable
fun ArenaShortcutChip(
    onClick: () -> Unit,
    atRisk: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "arena_chip_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "arena_chip_scale",
    )
    val accent = if (atRisk) RiskAmber else MaterialTheme.colorScheme.onBackground.copy(0.45f)

    Box(
        modifier = modifier
            .size(40.dp)
            .scale(if (atRisk) pulse else 1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (atRisk) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(RiskAmber.copy(0.18f), CircleShape)
                    .border(1.dp, RiskAmber.copy(0.55f), CircleShape),
            )
        }
        ArenaConjoinedIcon(
            tint = accent,
            iconSize = if (atRisk) 16.dp else 14.dp,
        )
    }
}
