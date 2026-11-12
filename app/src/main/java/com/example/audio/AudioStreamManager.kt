package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.example.data.model.SirenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioStreamManager(private val context: Context) {
    private val tag = "AudioStreamManager"
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var sirenTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private var sirenJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.IO)

    val isRecording = AtomicBoolean(false)
    val isPlaying = AtomicBoolean(false)
    val isMicMuted = AtomicBoolean(false)
    val isSpeakerMuted = AtomicBoolean(false)
    val isSirenPlaying = AtomicBoolean(false)
    var currentSirenType: SirenType? = null
        private set

    fun startRecording(onAudioChunk: (ByteArray) -> Unit) {
        if (isRecording.get()) return

        try {
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = Math.max(minBuf, 2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            val audioSessionId = audioRecord?.audioSessionId ?: 0
            if (audioSessionId != 0) {
                if (AcousticEchoCanceler.isAvailable()) {
                    AcousticEchoCanceler.create(audioSessionId)?.enabled = true
                }
                if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(audioSessionId)?.enabled = true
                }
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordJob = audioScope.launch {
                val buffer = ByteArray(1024)
                while (isActive && isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0 && !isMicMuted.get()) {
                        val chunk = buffer.copyOf(read)
                        onAudioChunk(chunk)
                    }
                }
            }
            Log.d(tag, "Audio recording started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start audio recording", e)
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun playAudioChunk(chunk: ByteArray) {
        if (isSpeakerMuted.get()) return

        try {
            if (audioTrack == null) {
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat)
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(Math.max(minBuf, chunk.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack?.play()
            }
            audioTrack?.write(chunk, 0, chunk.size)
        } catch (e: Exception) {
            Log.e(tag, "Error playing audio chunk", e)
        }
    }

    /**
     * Plays any of the 8 synthesized sirens with duration in seconds.
     */
    fun playSiren(type: SirenType, durationSeconds: Int = 4) {
        stopSiren()
        isSirenPlaying.set(true)
        currentSirenType = type

        sirenJob = audioScope.launch {
            try {
                val rate = 44100
                val totalSamples = rate * durationSeconds
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val timeSec = i.toDouble() / rate
                    val sampleValue: Double = when (type) {
                        SirenType.POLICE_WAIL -> {
                            // 600Hz to 1400Hz frequency modulation sweep (2.5 cycles/sec)
                            val cycle = timeSec * 2.5
                            val freq = 1000.0 + 400.0 * Math.sin(cycle * 2.0 * Math.PI)
                            Math.sin(2.0 * Math.PI * i * freq / rate)
                        }
                        SirenType.RAPID_YELP -> {
                            // Rapid 700Hz to 1600Hz sweep (6.0 cycles/sec)
                            val cycle = (timeSec * 6.0) % 1.0
                            val freq = 700.0 + cycle * 900.0
                            Math.sin(2.0 * Math.PI * i * freq / rate)
                        }
                        SirenType.AMBULANCE_TWO_TONE -> {
                            // European High-Low 960Hz / 720Hz alternating every 350ms
                            val isHigh = ((timeSec / 0.35).toInt() % 2) == 0
                            val freq = if (isHigh) 960.0 else 720.0
                            Math.sin(2.0 * Math.PI * i * freq / rate)
                        }
                        SirenType.PIERCING_INTRUDER -> {
                            // Piercing 2200Hz pulsed horn burst (120ms on, 60ms off)
                            val cycleMs = (timeSec * 1000).toInt() % 180
                            if (cycleMs < 120) {
                                Math.sin(2.0 * Math.PI * i * 2200.0 / rate)
                            } else 0.0
                        }
                        SirenType.AIR_RAID_RUMBLE -> {
                            // Deep rising and falling disaster rumble (300Hz to 800Hz, 0.8 cycles/sec)
                            val cycle = timeSec * 0.8
                            val freq = 550.0 + 250.0 * Math.sin(cycle * 2.0 * Math.PI)
                            Math.sin(2.0 * Math.PI * i * freq / rate)
                        }
                        SirenType.CYBER_STROBE -> {
                            // Rapid frequency stepping 1200Hz, 1800Hz, 2400Hz
                            val step = ((timeSec * 20.0).toInt() % 3)
                            val freq = when (step) {
                                0 -> 1200.0
                                1 -> 1800.0
                                else -> 2400.0
                            }
                            Math.sin(2.0 * Math.PI * i * freq / rate)
                        }
                        SirenType.GUARD_DOG_BURST -> {
                            // Low-frequency aggressive warning pulse (150Hz square wave thump, 4 pulses/sec)
                            val cycleMs = (timeSec * 1000).toInt() % 250
                            if (cycleMs < 140) {
                                Math.signum(Math.sin(2.0 * Math.PI * i * 160.0 / rate)) * 0.8
                            } else 0.0
                        }
                        SirenType.ULTRASONIC_DETERRENT -> {
                            // 3200Hz high decibel sharp deterrent pulses
                            val cycleMs = (timeSec * 1000).toInt() % 100
                            if (cycleMs < 70) {
                                Math.sin(2.0 * Math.PI * i * 3200.0 / rate)
                            } else 0.0
                        }
                    }
                    buffer[i] = (sampleValue * Short.MAX_VALUE * 0.85).toInt().toShort()
                }

                sirenTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                sirenTrack?.write(buffer, 0, totalSamples)
                sirenTrack?.play()
            } catch (e: Exception) {
                Log.e(tag, "Error playing siren", e)
            } finally {
                isSirenPlaying.set(false)
                currentSirenType = null
            }
        }
    }

    fun stopSiren() {
        isSirenPlaying.set(false)
        currentSirenType = null
        sirenJob?.cancel()
        sirenJob = null
        try {
            sirenTrack?.stop()
            sirenTrack?.release()
        } catch (_: Exception) {}
        sirenTrack = null
    }

    fun release() {
        stopRecording()
        stopSiren()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
