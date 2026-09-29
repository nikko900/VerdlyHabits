package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R

/** Shared springs for the bottom bar — low bounce, no overshoot past rest. */
object NavMotion {
    val press = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium,
    )
    val selectLift = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMedium,
    )
    val settle = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMedium,
    )
    const val ICON_LIFT_PX = -3.5f
    const val FAB_PRESS_SCALE = 0.95f
    const val FAB_BREATH_DP = 1.5f
    const val FAB_BREATH_MS = 3800
    const val PILL_MS = 150
    const val PLUS_TO_X_DEG = 45f
    const val SHEET_OPEN_MS = 400
    const val SHEET_CLOSE_MS = 280
}

/** One-shot Arena fire when that tab is selected. The only Lottie on this bar. */
@Composable
fun ArenaSelectFire(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.duo_fire))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = playing,
        iterations = 1,
        speed = 1.35f,
    )
    if (playing && composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier.size(28.dp),
        )
    }
}

/** Lifts a tab icon 2–4dp then springs back to rest when [selected] becomes true. */
@Composable
fun rememberTabSelectLift(selected: Boolean): Float {
    val lift = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (!selected) {
            lift.snapTo(0f)
            return@LaunchedEffect
        }
        lift.snapTo(0f)
        lift.animateTo(NavMotion.ICON_LIFT_PX, NavMotion.selectLift)
        lift.animateTo(0f, NavMotion.settle)
    }
    return lift.value
}
