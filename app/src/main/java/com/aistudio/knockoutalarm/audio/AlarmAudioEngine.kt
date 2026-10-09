package com.aistudio.knockoutalarm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.sin

/**
 * Production-ready Android Audio Engine for KnockOut Alarm.
 *
 * Provides:
 * 1. Looping synthetic alarm siren with dynamic ducking (35-45% during active punches).
 * 2. Punch impact SFX (thump + snap transient) with offline synthesis.
 * 3. Boxing knockout round-ending chime bell.
 * 4. Queued announcer cues ("20 LEFT", "10 MORE", "5 MORE", "FINAL PUNCH!") via TTS / synthesized audio.
 */
class AlarmAudioEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val sampleRate = 22050
    private var isPlaying = false
    private var isDucked = false

    private var sirenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var sirenTrack: AudioTrack? = null
    private var sfxTrack: AudioTrack? = null

    @Volatile
    private var currentSirenVolume = 1.0f
    private val duckedVolume = 0.38f

    private var lastPunchTimeMs = 0L
    private var unduckRunnable: Runnable? = null

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val announcerQueue = ConcurrentLinkedQueue<String>()
    private var isAnnouncing = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            // Graceful fallback if TTS is unavailable
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(1.15f)
            isTtsReady = true
        }
    }

    /**
     * Starts continuous looping boxing siren audio.
     */
    fun startAlarm() {
        if (isPlaying) return
        isPlaying = true
        currentSirenVolume = 1.0f
        isDucked = false

        sirenJob = scope.launch {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            sirenTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            sirenTrack?.play()

            val pcmBuffer = ShortArray(bufferSize / 2)
            var phase = 0.0
            var sirenPhase = 0.0

            while (isActive && isPlaying) {
                for (i in pcmBuffer.indices) {
                    // Two-tone rising and falling siren frequency (550Hz to 950Hz)
                    sirenPhase += 2.0 * PI * 1.5 / sampleRate // 1.5 Hz siren cycle
                    val freq = 750.0 + 200.0 * sin(sirenPhase)
                    phase += 2.0 * PI * freq / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI

                    val sample = sin(phase) * currentSirenVolume * 0.75
                    pcmBuffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                sirenTrack?.write(pcmBuffer, 0, pcmBuffer.size)
            }
        }
    }

    /**
     * Call when a punch occurs: plays impact SFX, ducks background siren,
     * and schedules restoring full volume 2 seconds after punching ceases.
     */
    fun onPunchLanded() {
        playPunchImpactSFX()

        // Duck siren volume
        currentSirenVolume = duckedVolume
        isDucked = true
        lastPunchTimeMs = System.currentTimeMillis()

        // Cancel previous unduck schedule and reset 2-second timer
        unduckRunnable?.let { mainHandler.removeCallbacks(it) }
        val restoreRunnable = Runnable {
            if (System.currentTimeMillis() - lastPunchTimeMs >= 1900L) {
                currentSirenVolume = 1.0f
                isDucked = false
            }
        }
        unduckRunnable = restoreRunnable
        mainHandler.postDelayed(restoreRunnable, 2000L)
    }

    /**
     * Plays a snappy boxing glove punch impact sound effect.
     */
    private fun playPunchImpactSFX() {
        scope.launch {
            try {
                val durationMs = 120
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    // Pitch drops rapidly from 220Hz to 45Hz for heavy glove impact
                    val freq = 220.0 * (1.0 - t * 0.8)
                    phase += 2.0 * PI * freq / sampleRate
                    val envelope = (1.0 - t) * (1.0 - t) // Quadratic decay
                    val sample = sin(phase) * envelope * 0.95
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs.toLong() + 50L)
                track.release()
            } catch (e: Exception) {
                // Ignore transient audio write error
            }
        }
    }

    /**
     * Enqueues an announcer vocal cue to avoid overlapping audio.
     */
    fun enqueueAnnouncerCue(text: String) {
        announcerQueue.offer(text)
        processAnnouncerQueue()
    }

    private fun processAnnouncerQueue() {
        if (isAnnouncing) return
        val nextCue = announcerQueue.poll() ?: return

        isAnnouncing = true
        if (isTtsReady && tts != null) {
            tts?.speak(nextCue, TextToSpeech.QUEUE_FLUSH, null, "ANN_${System.currentTimeMillis()}")
            mainHandler.postDelayed({
                isAnnouncing = false
                processAnnouncerQueue()
            }, 1200L)
        } else {
            // Tone chime fallback
            playChimeTone()
            mainHandler.postDelayed({
                isAnnouncing = false
                processAnnouncerQueue()
            }, 600L)
        }
    }

    private fun playChimeTone() {
        scope.launch {
            try {
                val numSamples = (sampleRate * 200) / 1000
                val buffer = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    phase += 2.0 * PI * 880.0 / sampleRate
                    val sample = sin(phase) * (1.0 - t) * 0.8
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                delay(250L)
                track.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Plays triumphant boxing knockout bell (3 resonant dings).
     */
    fun playKnockoutBell() {
        stopAlarm()
        scope.launch {
            for (strike in 1..3) {
                playSingleBellChime()
                delay(350L)
            }
        }
    }

    private suspend fun playSingleBellChime() {
        try {
            val durationMs = 500
            val numSamples = (sampleRate * durationMs) / 1000
            val buffer = ShortArray(numSamples)

            var phase1 = 0.0
            var phase2 = 0.0
            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                // Bell fundamental ~1320Hz with harmonic ~2640Hz
                phase1 += 2.0 * PI * 1320.0 / sampleRate
                phase2 += 2.0 * PI * 2640.0 / sampleRate
                val envelope = Math.exp(-4.0 * t) // Natural exponential decay
                val sample = (sin(phase1) * 0.7 + sin(phase2) * 0.3) * envelope
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            delay(durationMs.toLong())
            track.release()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Completely silences alarm audio and releases streaming tracks.
     */
    fun stopAlarm() {
        isPlaying = false
        sirenJob?.cancel()
        sirenJob = null
        unduckRunnable?.let { mainHandler.removeCallbacks(it) }

        try {
            sirenTrack?.stop()
            sirenTrack?.release()
        } catch (e: Exception) {
            // Safe ignore
        }
        sirenTrack = null
    }

    fun release() {
        stopAlarm()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
        tts = null
    }
}
