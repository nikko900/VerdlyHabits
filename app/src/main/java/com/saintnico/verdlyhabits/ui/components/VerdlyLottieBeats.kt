package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlinx.coroutines.delay

/** Button / action phase for tap → loading → confirmed → idle. */
enum class VerdlyActionPhase {
    Idle,
    Loading,
    Success,
    Error,
}

/**
 * Compact button content: loading Lottie, brief checkmark success, or [idleContent].
 */
@Composable
fun VerdlyActionButtonContent(
    phase: VerdlyActionPhase,
    successLabel: String = "Sent",
    contentColor: Color = Color.White,
    idleContent: @Composable () -> Unit,
) {
    when (phase) {
        VerdlyActionPhase.Loading -> {
            VerdlySendingIndicator(size = 28.dp)
        }
        VerdlyActionPhase.Success -> {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                successLabel,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
        VerdlyActionPhase.Idle, VerdlyActionPhase.Error -> idleContent()
    }
}

/**
 * One-shot handshake celebration (~1–2s). Invokes [onFinished] after play-through.
 */
@Composable
fun VerdlyHandshakeBeat(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    play: Boolean = true,
    onFinished: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.handshake_loop))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = play,
        restartOnPlay = true,
    )
    LaunchedEffect(play, progress, composition) {
        if (play && composition != null && progress >= 0.99f) {
            onFinished?.invoke()
        }
    }
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size),
    )
}

/**
 * Full-screen / sheet overlay that plays handshake once then calls [onFinished].
 */
@Composable
fun VerdlyHandshakeOverlay(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        VerdlyHandshakeBeat(
            size = 180.dp,
            play = true,
            onFinished = onFinished,
        )
    }
}

/**
 * Hero alien for empty states. Loops [R.raw.alien_button]; tap bounces the character.
 */
@Composable
fun VerdlyAlienHero(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onTap: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.alien_button))
    val bounce = remember { Animatable(1f) }
    var playKey by remember { mutableStateOf(0) }

    LaunchedEffect(playKey) {
        if (playKey == 0) return@LaunchedEffect
        bounce.snapTo(0.88f)
        bounce.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f))
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        playKey += 1
                        onTap()
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Post-signup welcome: plays [R.raw.account_created] once with personalized Compose copy.
 */
@Composable
fun AccountCreatedWelcome(
    displayName: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.account_created))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
    )
    val name = displayName.trim().ifBlank { "friend" }
    var finished by remember { mutableStateOf(false) }
    val continueOnce: () -> Unit = {
        if (!finished) {
            finished = true
            onContinue()
        }
    }

    LaunchedEffect(progress, composition) {
        if (composition != null && progress >= 0.99f) {
            delay(450)
            continueOnce()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        VerdlyHandshakeBeat(size = 72.dp, play = true)
        Spacer(Modifier.height(12.dp))
        Text(
            "Thank you, $name",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            "Your account has been created successfully.",
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Let's grow your grove.",
            fontFamily = dmSansFamily,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.TextButton(onClick = continueOnce) {
            Text(
                "Continue",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
