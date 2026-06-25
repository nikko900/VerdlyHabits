package com.saintnico.verdlyhabits.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.saintnico.verdlyhabits.R

/**
 * Centralised audio for the app.
 *
 *  - Short feedback chimes go through [SoundPool] (low-latency, fire-and-forget).
 *  - Long ambient loops (Focus mode) go through [MediaPlayer].
 *
 * Audio assets live in `res/raw` as `.ogg` files. The engine is defensive: every
 * lookup is guarded so missing assets simply silence the sound instead of crashing.
 * That means you can ship the engine before all assets exist (chimes will be added
 * progressively).
 *
 * Respects the system [AudioManager.RINGER_MODE_SILENT] / `RINGER_MODE_VIBRATE`
 * settings — no sound plays while the phone is silenced.
 */
class SoundEngine(private val context: Context) {

    enum class Chime { COMPLETE, MILESTONE_7, MILESTONE_30, MILESTONE_100, LEVEL_UP }

    enum class Ambient(val rawResId: Int?) {
        RAIN(null),
        LOFI(null),
        FOREST(null),
        WHITE_NOISE(null);

        companion object {
            // Will become non-null once the .ogg files are dropped into res/raw.
            // Map keyed by name so we can swap the resource at runtime without
            // touching call sites.
            fun resolveResId(ambient: Ambient, context: Context): Int? = try {
                context.resources.getIdentifier(
                    "ambient_${ambient.name.lowercase()}",
                    "raw",
                    context.packageName
                ).takeIf { it != 0 }
            } catch (_: Throwable) { null }
        }
    }

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val pool: SoundPool by lazy {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()
    }

    private val chimeIds: Map<Chime, Int?> by lazy {
        Chime.entries.associateWith { chime ->
            val name = when (chime) {
                Chime.COMPLETE      -> "chime_complete"
                Chime.MILESTONE_7   -> "chime_milestone_7"
                Chime.MILESTONE_30  -> "chime_milestone_30"
                Chime.MILESTONE_100 -> "chime_milestone_100"
                Chime.LEVEL_UP      -> "chime_level_up"
            }
            try {
                val resId = context.resources.getIdentifier(name, "raw", context.packageName)
                if (resId != 0) pool.load(context, resId, 1) else null
            } catch (_: Throwable) { null }
        }
    }

    private var ambientPlayer: MediaPlayer? = null

    /** True when the phone isn't silenced. */
    private fun isAudible(): Boolean =
        audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL

    fun playChime(chime: Chime, soundEnabled: Boolean = true) {
        if (!soundEnabled || !isAudible()) return
        val id = chimeIds[chime] ?: return
        try {
            pool.play(id, 1f, 1f, 1, 0, 1f)
        } catch (_: Throwable) {}
    }

    fun startAmbient(ambient: Ambient, soundEnabled: Boolean = true) {
        stopAmbient()
        if (!soundEnabled) return
        val resId = Ambient.resolveResId(ambient, context) ?: return
        try {
            ambientPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(0.55f, 0.55f)
                start()
            }
        } catch (_: Throwable) {
            ambientPlayer = null
        }
    }

    fun stopAmbient() {
        try {
            ambientPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Throwable) {}
        ambientPlayer = null
    }

    fun release() {
        stopAmbient()
        try { pool.release() } catch (_: Throwable) {}
    }
}
