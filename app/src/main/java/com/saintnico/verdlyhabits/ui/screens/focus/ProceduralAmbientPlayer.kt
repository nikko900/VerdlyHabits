package com.saintnico.verdlyhabits.ui.screens.focus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import com.saintnico.verdlyhabits.ui.screens.focus.AmbientSoundManager.AmbientSound

/**
 * Lightweight looping ambience via [AudioTrack] when no `res/raw` clip is bundled.
 * Add real MP3/OGG under `res/raw` (same names as [AmbientSound.rawName]) for higher fidelity.
 */
internal class ProceduralAmbientPlayer {

    @Volatile
    private var targetVolume = 0.7f

    @Volatile
    private var smoothVolume = 0f

    @Volatile
    private var active = false

    private var worker: Thread? = null

    @Volatile
    private var mode: AmbientSound = AmbientSound.NONE

    fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
    }

    fun isRunning(): Boolean = active

    fun start(sound: AmbientSound, volume: Float) {
        stop()
        if (sound == AmbientSound.NONE) return
        mode = sound
        targetVolume = volume.coerceIn(0f, 1f)
        smoothVolume = 0f
        active = true
        worker = Thread({ renderLoop() }, "verdly-procedural-ambient").also { it.start() }
    }

    fun stop() {
        active = false
        worker?.join(1200)
        worker = null
        mode = AmbientSound.NONE
    }

    private fun renderLoop() {
        val sampleRate = 44_100
        val channelMask = AudioFormat.CHANNEL_OUT_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
        if (minBuf <= 0) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(encoding)
            .setChannelMask(channelMask)
            .build()

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes((minBuf * 2).coerceAtLeast(minBuf + 1024))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return
        }
        track.play()

        val frameCount = 512
        val buf = ShortArray(frameCount * 2)
        val mono = FloatArray(frameCount)

        var brownBrown = 0f
        val pinkOcean = FloatArray(7)
        var rainPhase = 0f
        var oceanPhase = 0.0
        var lofiT = 0.0
        val pinkRain = FloatArray(7)

        while (active) {
            smoothVolume += (targetVolume - smoothVolume) * 0.06f
            val gain = smoothVolume.coerceIn(0f, 1f)

            when (mode) {
                AmbientSound.RAIN_DAY -> {
                    for (i in 0 until frameCount) {
                        var p = 0f
                        for (o in pinkRain.indices) {
                            pinkRain[o] = pinkRain[o] * 0.92f + (Random.nextFloat() * 2f - 1f) * 0.11f
                            p += pinkRain[o] * (1f / (o + 1))
                        }
                        val patter = 0.25f + 0.75f * abs(sin(rainPhase + i * 0.31f))
                        val drop = if (Random.nextFloat() < 0.012f) Random.nextFloat() * 0.35f else 0f
                        mono[i] = (p * 0.14f * patter + drop).coerceIn(-1f, 1f)
                    }
                    rainPhase += 0.18f
                }
                AmbientSound.LOFI_HITS,
                AmbientSound.LOFI_CHILL,
                AmbientSound.LOFI_BACKGROUND,
                AmbientSound.LOFI_PHONK,
                AmbientSound.MOTIVATION_PULSE,
                AmbientSound.MOTIVATION_ON,
                AmbientSound.MOTIVATION_DRIVE,
                AmbientSound.MOTIVATION_RISE,
                -> {
                    val sr = sampleRate.toDouble()
                    for (i in 0 until frameCount) {
                        val t = lofiT + i / sr
                        val n = (Random.nextFloat() * 2f - 1f) * 0.045f
                        val a = sin(t * 2 * PI * 196.0).toFloat() * 0.11f
                        val b = sin(t * 2 * PI * 246.94).toFloat() * 0.09f
                        mono[i] = (a + b + n).coerceIn(-1f, 1f)
                    }
                    lofiT += frameCount / sr
                }
                AmbientSound.CALM_STILLNESS,
                AmbientSound.CALM_RENEWAL,
                -> {
                    for (i in 0 until frameCount) {
                        var p = 0f
                        for (o in pinkOcean.indices) {
                            pinkOcean[o] = pinkOcean[o] * 0.93f + (Random.nextFloat() * 2f - 1f) * 0.08f
                            p += pinkOcean[o] * (1f / (o + 1))
                        }
                        val swell = 0.35f + 0.65f * sin(oceanPhase + i * 0.0018).toFloat()
                        val waveBreak = if (swell > 0.82f && Random.nextFloat() < 0.04f) Random.nextFloat() * 0.2f else 0f
                        mono[i] = (p * 0.11f * swell + waveBreak).coerceIn(-1f, 1f)
                    }
                    oceanPhase += frameCount.toDouble() / sampleRate * 0.28
                }
                else -> {
                    for (i in 0 until frameCount) {
                        val w = Random.nextFloat() * 2f - 1f
                        brownBrown = (brownBrown * 0.985f + w * 0.035f).coerceIn(-1f, 1f)
                        mono[i] = brownBrown * 0.5f
                    }
                }
            }

            var j = 0
            for (i in 0 until frameCount) {
                val s = (mono[i] * gain * Short.MAX_VALUE).toInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                )
                val v = s.toShort()
                buf[j++] = v
                buf[j++] = v
            }

            var offset = 0
            while (offset < buf.size && active) {
                val w = track.write(buf, offset, buf.size - offset)
                if (w <= 0) break
                offset += w
            }
        }
        try {
            track.stop()
        } catch (_: Exception) {
        }
        track.release()
    }
}
