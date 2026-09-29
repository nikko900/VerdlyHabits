package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R

/**
 * Inline / sheet / full-screen loading beat using [R.raw.loading].
 * Composition is remembered so the loop does not restart on unrelated recompositions.
 */
@Composable
fun VerdlyLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier.size(size),
    )
}
