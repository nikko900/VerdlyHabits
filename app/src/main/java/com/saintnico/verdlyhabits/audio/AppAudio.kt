package com.saintnico.verdlyhabits.audio

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.saintnico.verdlyhabits.haptics.HapticsEngine

/**
 * Lightweight, lazily-initialized singletons for SoundEngine + HapticsEngine.
 *
 * Both are bound to the *application context* (never an Activity) so they don't
 * leak on rotation. Compose call sites get them via [rememberAppSound] /
 * [rememberAppHaptics] to keep the API ergonomic.
 */
object AppAudio {

    @Volatile private var soundEngine: SoundEngine? = null
    @Volatile private var hapticsEngine: HapticsEngine? = null

    fun sound(context: Context): SoundEngine =
        soundEngine ?: synchronized(this) {
            soundEngine ?: SoundEngine(context.applicationContext).also { soundEngine = it }
        }

    fun haptics(context: Context): HapticsEngine =
        hapticsEngine ?: synchronized(this) {
            hapticsEngine ?: HapticsEngine(context.applicationContext).also { hapticsEngine = it }
        }
}

@Composable
fun rememberAppSound(): SoundEngine {
    val ctx = LocalContext.current
    return remember(ctx.applicationContext) { AppAudio.sound(ctx) }
}

@Composable
fun rememberAppHaptics(): HapticsEngine {
    val ctx = LocalContext.current
    return remember(ctx.applicationContext) { AppAudio.haptics(ctx) }
}
