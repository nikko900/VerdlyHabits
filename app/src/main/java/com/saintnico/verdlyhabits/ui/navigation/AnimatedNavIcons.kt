package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R
import kotlinx.coroutines.delay

private val ChallengeGold = Color(0xFFFFD54F)
private val FocusMint = Color(0xFFB9F6CA)

/**
 * One-shot tab icon motion for the floating bottom-nav button.
 * Duo uses a brief Lottie fire burst; other tabs use Compose springs.
 */
@Composable
fun AnimatedFloatingNavIcon(
    route: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val tintMix = remember { Animatable(0f) }
    var showDuoFire by remember { mutableStateOf(false) }
    var showProfileCheck by remember { mutableStateOf(false) }

    val duoFireComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.duo_fire))
    val duoFireProgress by animateLottieCompositionAsState(
        composition = duoFireComp,
        isPlaying = showDuoFire,
        iterations = 1,
        speed = 1.35f,
    )

    LaunchedEffect(route) {
        scale.snapTo(1f)
        rotation.snapTo(0f)
        offsetY.snapTo(0f)
        tintMix.snapTo(0f)
        showDuoFire = false
        showProfileCheck = false

        when (route) {
            "home" -> {
                scale.snapTo(0.82f)
                offsetY.snapTo(4f)
                offsetY.animateTo(-8f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.55f))
                scale.animateTo(1.14f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.5f))
                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.72f))
                scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.65f))
            }

            "challenges" -> {
                tintMix.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                scale.animateTo(1.22f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.45f))
                scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.62f))
                tintMix.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
            }

            "duo" -> {
                showDuoFire = true
                scale.animateTo(1.18f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.5f))
                delay(1400)
                showDuoFire = false
                scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.7f))
            }

            "focus" -> {
                scale.snapTo(0.45f)
                scale.animateTo(1.16f, tween(680, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.68f))
                tintMix.animateTo(1f, tween(400))
                tintMix.animateTo(0f, tween(600))
            }

            "goals" -> {
                repeat(2) {
                    rotation.animateTo(14f, tween(110, easing = FastOutSlowInEasing))
                    rotation.animateTo(-14f, tween(110, easing = FastOutSlowInEasing))
                }
                rotation.animateTo(0f, tween(120))
                scale.animateTo(1.1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.55f))
                scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.7f))
            }

            "profile" -> {
                scale.animateTo(1.12f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.52f))
                showProfileCheck = true
                delay(900)
                showProfileCheck = false
                scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.68f))
            }

            else -> scale.animateTo(1f, tween(200))
        }
    }

    val iconTint = when (route) {
        "challenges" -> lerp(Color.White, ChallengeGold, tintMix.value)
        "focus" -> lerp(Color.White, FocusMint, tintMix.value * 0.85f)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
                translationY = offsetY.value
            },
        contentAlignment = Alignment.Center,
    ) {
        if (route == "duo" && showDuoFire && duoFireComp != null) {
            LottieAnimation(
                composition = duoFireComp,
                progress = { duoFireProgress },
                modifier = Modifier.size(36.dp),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp),
            )
        }

        if (route == "profile" && showProfileCheck) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 2.dp)
                    .size(14.dp)
                    .graphicsLayer {
                        scaleX = 1f + (scale.value - 1f) * 0.5f
                        scaleY = scaleX
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF74C69D),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
