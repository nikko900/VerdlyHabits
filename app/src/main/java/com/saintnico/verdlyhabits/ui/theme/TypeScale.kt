package com.saintnico.verdlyhabits.ui.theme

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * System font scale, capped so plant / grove / nudge chrome still fits when
 * people bump accessibility text size.
 */
@Composable
fun cappedSp(unscaled: Float, maxScale: Float = 1.35f): TextUnit {
    val fontScale = LocalDensity.current.fontScale
    val applied = fontScale.coerceIn(0.85f, maxScale)
    return (unscaled * applied / fontScale).sp
}

/** Lift primary actions above the system nav bar and the keyboard. */
fun Modifier.aboveSystemNav(): Modifier =
    this.navigationBarsPadding().imePadding()
