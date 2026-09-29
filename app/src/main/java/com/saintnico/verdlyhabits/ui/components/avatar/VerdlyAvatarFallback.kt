package com.saintnico.verdlyhabits.ui.components.avatar

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.saintnico.verdlyhabits.R

/** Stable hashed avatar Lotties when no profile photo is available. */
val AVATAR_FALLBACK_LOTTIES: IntArray = intArrayOf(
    R.raw.avatar_solid,
    R.raw.avatar_developer,
    R.raw.avatars_message_plus,
    R.raw.ghost_happy,
    R.raw.emoji_smile,
    R.raw.avatars_chatting,
)

fun avatarIndexFor(userId: String): Int {
    if (userId.isBlank()) return 0
    return (userId.hashCode() and Int.MAX_VALUE) % AVATAR_FALLBACK_LOTTIES.size
}

@RawRes
fun avatarFallbackRawRes(userId: String?): Int {
    val id = userId.orEmpty()
    return AVATAR_FALLBACK_LOTTIES[avatarIndexFor(id)]
}

@Composable
fun VerdlyAvatarFallback(
    userId: String?,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val raw = avatarFallbackRawRes(userId)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(raw))
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
