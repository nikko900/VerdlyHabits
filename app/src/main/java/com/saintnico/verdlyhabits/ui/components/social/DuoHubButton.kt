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
import androidx.compose.foundation.layout.offset
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R

private val Mint = Color(0xFF52B788)
private val RiskAmber = Color(0xFFFFB300)

@Composable
fun DuoHubButton(
    onClick: () -> Unit,
    glowing: Boolean,
    atRisk: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "duo_hub_glow")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "duo_hub_pulse",
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "duo_hub_alpha",
    )
    val accent = if (atRisk) RiskAmber else Mint
    val flameComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.duo_fire))
    val flameProgress by animateLottieCompositionAsState(
        composition = flameComp,
        iterations = LottieConstants.IterateForever,
        speed = if (atRisk) 1.3f else 1f,
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .scale(if (glowing) pulse else 1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (glowing) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = glowAlpha * 0.35f), CircleShape)
                    .border(1.dp, accent.copy(alpha = glowAlpha), CircleShape),
            )
        }
        LottieAnimation(
            composition = flameComp,
            progress = { flameProgress },
            modifier = Modifier.size(30.dp),
        )
        if (glowing) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(9.dp)
                    .background(accent, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
            )
        }
    }
}
