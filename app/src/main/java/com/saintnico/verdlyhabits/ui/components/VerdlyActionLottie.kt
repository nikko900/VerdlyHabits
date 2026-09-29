package com.saintnico.verdlyhabits.ui.components

import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import kotlinx.coroutines.CancellationException

/** Small inline Lottie for toolbar / button / row icons (20–28dp). */
@Composable
fun VerdlyActionLottie(
    @RawRes rawRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    iterations: Int = LottieConstants.IterateForever,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    LottieAnimation(
        composition = composition,
        iterations = iterations,
        modifier = modifier.size(size),
    )
}

@Composable
fun VerdlyCameraFlashIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    VerdlyActionLottie(
        rawRes = R.raw.camera_flash,
        modifier = modifier,
        size = size,
    )
}

/** Send-action loading beat ([R.raw.sending]). */
@Composable
fun VerdlySendingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    VerdlyActionLottie(
        rawRes = R.raw.sending,
        modifier = modifier,
        size = size,
    )
}

/**
 * Hold-to-confirm destructive control using [R.raw.hold_to_delete].
 */
@Composable
fun VerdlyHoldToDeleteButton(
    onConfirmed: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDurationMs: Long = 1400L,
    containerColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = Color.White,
) {
    var holding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.hold_to_delete))

    LaunchedEffect(holding, enabled) {
        if (!enabled || !holding) {
            try {
                progress.snapTo(0f)
            } catch (_: CancellationException) {
            }
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        try {
            progress.animateTo(
                1f,
                animationSpec = tween(holdDurationMs.toInt(), easing = LinearEasing),
            )
            onConfirmed()
        } catch (_: CancellationException) {
            progress.snapTo(0f)
        } finally {
            holding = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                holding = true
                                tryAwaitRelease()
                                holding = false
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress.value.coerceIn(0f, 1f) },
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (holding) "Keep holding…" else label,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.6f),
            )
        }
    }
}
