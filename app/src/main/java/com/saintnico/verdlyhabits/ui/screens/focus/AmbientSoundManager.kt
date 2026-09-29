package com.saintnico.verdlyhabits.ui.screens.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.saintnico.verdlyhabits.data.remote.storage.AmbientSoundRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ambient loop playback for Focus Mode.
 *
 * Playback order: local cache (on-demand download) → procedural fallback.
 * Audio files are **not** bundled in the APK; they download once from Firebase Storage on first use.
 */
class AmbientSoundManager(private val context: Context) {

    private val repository = AmbientSoundRepository(context)

    enum class AmbientCategory(val title: String) {
        RAIN("Rain"),
        LOFI("Lo-fi"),
        MOTIVATION("Motivation"),
        CALM("Calm"),
    }

    enum class AmbientSound(
        val label: String,
        val emoji: String,
        val rawName: String?,
        val category: AmbientCategory? = null,
    ) {
        NONE("None", "○", null),
        RAIN_DAY("Rainy Day", "🌧", "ambient_rain", AmbientCategory.RAIN),
        LOFI_HITS("Lofi Hits", "♪", "ambient_lofi_hits", AmbientCategory.LOFI),
        LOFI_CHILL("Lofi Chill", "🎧", "ambient_lofi_chill", AmbientCategory.LOFI),
        LOFI_BACKGROUND("Chill Background", "☕", "ambient_lofi_background", AmbientCategory.LOFI),
        LOFI_PHONK("Phonk", "🔊", "ambient_lofi_phonk", AmbientCategory.LOFI),
        MOTIVATION_PULSE("Pulse", "🔥", "ambient_motivation_pulse", AmbientCategory.MOTIVATION),
        MOTIVATION_ON("Motivation On", "⚡", "ambient_motivation_on", AmbientCategory.MOTIVATION),
        MOTIVATION_DRIVE("Drive", "🏃", "ambient_motivation_drive", AmbientCategory.MOTIVATION),
        MOTIVATION_RISE("Rise Up", "🚀", "ambient_motivation_rise", AmbientCategory.MOTIVATION),
        CALM_STILLNESS("Stillness", "🧘", "ambient_calm_stillness", AmbientCategory.CALM),
        CALM_RENEWAL("Renewal", "🌿", "ambient_calm_renewal", AmbientCategory.CALM),
    }

    companion object {
        fun soundsByCategory(): List<Pair<AmbientCategory, List<AmbientSound>>> =
            AmbientCategory.entries.mapNotNull { cat ->
                val sounds = AmbientSound.entries.filter { it.category == cat }
                if (sounds.isEmpty()) null else cat to sounds
            }
    }

    fun isCached(sound: AmbientSound): Boolean =
        sound.rawName?.let { repository.isCached(it) } == true

    fun cachedSizeMb(): Float = repository.cachedSizeBytes() / (1024f * 1024f)

    private fun rawId(name: String): Int? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (id != 0) id else null
    }

    private var mediaPlayer: MediaPlayer? = null
    private val procedural = ProceduralAmbientPlayer()
    private var currentSound: AmbientSound = AmbientSound.NONE
    private var targetVolume: Float = 0.7f
    private val mainHandler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null

    private var soundPool: SoundPool? = null
    private var chimeLoadedId: Int = 0

    init {
        try {
            soundPool = SoundPool.Builder().setMaxStreams(2).build()
            val cid = rawId("chime_complete")
            if (cid != null) {
                chimeLoadedId = soundPool?.load(context, cid, 1) ?: 0
            }
        } catch (_: Exception) {
            soundPool = null
            chimeLoadedId = 0
        }
    }

    fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(targetVolume, targetVolume)
        procedural.setVolume(targetVolume)
    }

    fun play(sound: AmbientSound) {
        playBlocking(sound)
    }

    suspend fun playAsync(sound: AmbientSound) {
        if (sound == AmbientSound.NONE) {
            withContext(Dispatchers.Main) {
                stopAllImmediate()
                currentSound = AmbientSound.NONE
            }
            return
        }
        if (!shouldPlayAudio()) return
        if (sound == currentSound && (mediaPlayer?.isPlaying == true || procedural.isRunning())) {
            return
        }

        val rawName = sound.rawName ?: return
        val localFile = withContext(Dispatchers.IO) { repository.ensureCached(rawName) }

        withContext(Dispatchers.Main) {
            stopAllImmediate()
            currentSound = sound
            if (localFile != null && playFromFile(localFile)) return@withContext
            startProcedural(sound)
        }
    }

    private fun playBlocking(sound: AmbientSound) {
        if (sound == AmbientSound.NONE) {
            stopAllImmediate()
            currentSound = AmbientSound.NONE
            return
        }
        if (!shouldPlayAudio()) return
        if (sound == currentSound && (mediaPlayer?.isPlaying == true || procedural.isRunning())) {
            return
        }

        stopAllImmediate()
        currentSound = sound

        val rawName = sound.rawName
        if (rawName != null) {
            val cached = repository.cachedFile(rawName)
            if (cached.exists() && cached.length() > 1_024L && playFromFile(cached)) return
        }
        startProcedural(sound)
    }

    private fun playFromFile(file: File): Boolean {
        val mp = try {
            MediaPlayer.create(context, Uri.fromFile(file))
        } catch (_: Exception) {
            null
        } ?: return false
        return startMediaPlayer(mp)
    }

    private fun startMediaPlayer(mp: MediaPlayer): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
            }
        } catch (_: Exception) {
        }
        return try {
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.start()
            mediaPlayer = mp
            fadeInMediaPlayer(mp, 1500)
            true
        } catch (_: Exception) {
            mp.release()
            false
        }
    }

    private fun startProcedural(sound: AmbientSound) {
        procedural.start(sound, targetVolume)
    }

    private fun stopAllImmediate() {
        fadeRunnable?.let { mainHandler.removeCallbacks(it) }
        fadeRunnable = null
        procedural.stop()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun fadeOutAndStop(durationMs: Int = 2000, onEnd: () -> Unit = {}) {
        procedural.stop()
        val mp = mediaPlayer
        if (mp == null) {
            currentSound = AmbientSound.NONE
            onEnd()
            return
        }
        fadeOutMediaPlayer(mp, durationMs) {
            try {
                mp.stop()
            } catch (_: Exception) {
            }
            mp.release()
            if (mediaPlayer === mp) mediaPlayer = null
            currentSound = AmbientSound.NONE
            onEnd()
        }
    }

    fun stop() {
        stopAllImmediate()
        currentSound = AmbientSound.NONE
    }

    fun release() {
        stopAllImmediate()
        currentSound = AmbientSound.NONE
        soundPool?.release()
        soundPool = null
    }

    fun playCompletionChime() {
        if (!shouldPlayAudio()) return
        try {
            if (chimeLoadedId > 0) {
                soundPool?.play(chimeLoadedId, 0.6f, 0.6f, 1, 0, 1f)
                return
            }
        } catch (_: Exception) {
        }
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 220)
            mainHandler.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {
                }
            }, 380)
        } catch (_: Exception) {
        }
    }

    private fun shouldPlayAudio(): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.ringerMode != AudioManager.RINGER_MODE_SILENT
        } catch (_: Exception) {
            true
        }
    }

    private fun fadeInMediaPlayer(player: MediaPlayer, durationMs: Int) {
        fadeRunnable?.let { mainHandler.removeCallbacks(it) }
        val steps = 20
        val stepDelay = (durationMs.toLong() / steps).coerceAtLeast(1L)
        var step = 0
        fadeRunnable = object : Runnable {
            override fun run() {
                step++
                val v = (targetVolume * step / steps).coerceIn(0f, targetVolume)
                try {
                    player.setVolume(v, v)
                } catch (_: Exception) {
                }
                if (step < steps) {
                    mainHandler.postDelayed(this, stepDelay)
                }
            }
        }
        mainHandler.post(fadeRunnable!!)
    }

    private fun fadeOutMediaPlayer(player: MediaPlayer, durationMs: Int, onComplete: () -> Unit) {
        fadeRunnable?.let { mainHandler.removeCallbacks(it) }
        val steps = 20
        val stepDelay = (durationMs.toLong() / steps).coerceAtLeast(1L)
        var step = 0
        fadeRunnable = object : Runnable {
            override fun run() {
                step++
                val v = (targetVolume * (1f - step / steps.toFloat())).coerceIn(0f, targetVolume)
                try {
                    player.setVolume(v, v)
                } catch (_: Exception) {
                }
                if (step < steps) {
                    mainHandler.postDelayed(this, stepDelay)
                } else {
                    try {
                        player.setVolume(0f, 0f)
                    } catch (_: Exception) {
                    }
                    onComplete()
                }
            }
        }
        mainHandler.post(fadeRunnable!!)
    }
}
