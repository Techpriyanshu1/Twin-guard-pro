package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
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
    private var recordJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.IO)

    val isRecording = AtomicBoolean(false)
    val isPlaying = AtomicBoolean(false)
    val isMicMuted = AtomicBoolean(false)
    val isSpeakerMuted = AtomicBoolean(false)

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

            // Enable hardware echo cancellation and noise suppressor if available
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
        } catch (e: SecurityException) {
            Log.e(tag, "Microphone permission not granted", e)
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
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    audioFormat
                )
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
     * Plays a loud warning siren through the phone speaker using synthetic tone generator.
     */
    fun playSirenSound(durationSeconds: Int = 3) {
        audioScope.launch {
            try {
                val rate = 44100
                val totalSamples = rate * durationSeconds
                val buffer = ShortArray(totalSamples)

                // Frequency sweeps between 600Hz and 1400Hz (classic security alarm warble)
                for (i in 0 until totalSamples) {
                    val cycle = (i.toDouble() / rate) * 3.0 // 3 warbles per second
                    val freq = 800.0 + 500.0 * Math.sin(cycle * 2.0 * Math.PI)
                    val sample = (Math.sin(2.0 * Math.PI * i * freq / rate) * Short.MAX_VALUE * 0.8).toInt()
                    buffer[i] = sample.toShort()
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
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, totalSamples)
                track.play()
            } catch (e: Exception) {
                Log.e(tag, "Error playing siren", e)
            }
        }
    }

    fun release() {
        stopRecording()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
