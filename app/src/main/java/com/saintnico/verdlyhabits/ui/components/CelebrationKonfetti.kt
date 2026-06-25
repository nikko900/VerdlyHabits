package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * Top-level confetti overlay. Renders nothing until [trigger] changes; on each
 * change it fires a single celebration burst.
 *
 *  - Use it once on the dashboard (and once in Focus mode) and feed it a trigger key.
 *  - Particle palette is theme-aware: primary, secondary, gold, plus a few accent shades.
 */
@Composable
fun CelebrationKonfetti(
    trigger: Any?,
    modifier: Modifier = Modifier,
    intensity: BurstIntensity = BurstIntensity.NORMAL
) {
    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val secondary = MaterialTheme.colorScheme.secondary.toArgb()
    val palette = remember(primary, secondary) {
        listOf(
            primary,
            secondary,
            0xFFFFD54F.toInt(),
            0xFFFF6B35.toInt(),
            0xFF7C3AED.toInt()
        )
    }

    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        val (perSec, durMs) = when (intensity) {
            BurstIntensity.SOFT      -> 90 to 600
            BurstIntensity.NORMAL    -> 220 to 900
            BurstIntensity.HUGE      -> 360 to 1400
        }
        parties = listOf(
            Party(
                speed = 8f,
                maxSpeed = 28f,
                damping = 0.92f,
                spread = 360,
                colors = palette,
                position = Position.Relative(0.5, 0.35),
                emitter = Emitter(duration = durMs.toLong(), TimeUnit.MILLISECONDS).perSecond(perSec)
            )
        )
        // Auto-clear so we don't keep the view animating forever
        delay(durMs.toLong() + 2200)
        parties = emptyList()
    }

    if (parties.isNotEmpty()) {
        KonfettiView(
            modifier = modifier.fillMaxSize(),
            parties = parties
        )
    }
}

enum class BurstIntensity { SOFT, NORMAL, HUGE }
