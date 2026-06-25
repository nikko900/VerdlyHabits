package com.saintnico.verdlyhabits.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibration / haptic feedback. All entry points are guarded so they no-op silently
 * if the device doesn't support the effect or vibration is system-disabled.
 */
class HapticsEngine(context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) { null }

    /** Quick tap — for tab swaps / button presses. */
    fun tick(enabled: Boolean = true) {
        if (!enabled) return
        play(VibrationKind.Tick)
    }

    /** Punchier — for habit completion. */
    fun success(enabled: Boolean = true) {
        if (!enabled) return
        play(VibrationKind.Success)
    }

    /** Big wave — for milestones / level-ups / achievements. */
    fun celebration(enabled: Boolean = true) {
        if (!enabled) return
        play(VibrationKind.Celebration)
    }

    /** Soft buzz — for missed-streak warnings. */
    fun warning(enabled: Boolean = true) {
        if (!enabled) return
        play(VibrationKind.Warning)
    }

    private enum class VibrationKind { Tick, Success, Celebration, Warning }

    private fun play(kind: VibrationKind) {
        val v = vibrator ?: return
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val effect = when (kind) {
                        VibrationKind.Tick -> VibrationEffect.createOneShot(15, 70)
                        VibrationKind.Success ->
                            VibrationEffect.createWaveform(longArrayOf(0, 35, 30, 80), -1)
                        VibrationKind.Celebration ->
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 60, 60, 100, 80, 180),
                                intArrayOf(0, 120, 0, 160, 0, 220),
                                -1
                            )
                        VibrationKind.Warning ->
                            VibrationEffect.createWaveform(longArrayOf(0, 50, 80, 50), -1)
                    }
                    v.vibrate(effect)
                }
                else -> {
                    @Suppress("DEPRECATION")
                    when (kind) {
                        VibrationKind.Tick        -> v.vibrate(15)
                        VibrationKind.Success     -> v.vibrate(longArrayOf(0, 35, 30, 80), -1)
                        VibrationKind.Celebration -> v.vibrate(longArrayOf(0, 60, 60, 100, 80, 180), -1)
                        VibrationKind.Warning     -> v.vibrate(longArrayOf(0, 50, 80, 50), -1)
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}
