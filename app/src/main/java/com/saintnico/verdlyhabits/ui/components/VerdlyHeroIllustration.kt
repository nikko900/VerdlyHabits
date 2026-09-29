package com.saintnico.verdlyhabits.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun VerdlyHeroIllustration(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun VerdlyHeroLottie(
    @RawRes rawRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    loop: Boolean = true,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = if (loop) LottieConstants.IterateForever else 1,
                modifier = Modifier.size(size),
            )
        }
    }
}
