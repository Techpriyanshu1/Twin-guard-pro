package com.example.network.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.model.CameraTelemetry
import com.example.data.model.RemoteAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MonitorClient(
    val serverIp: String,
    val serverPort: Int = 8080,
    val authToken: String = ""
) {
    private val tag = "MonitorClient"
    private val clientScope = CoroutineScope(Dispatchers.IO)
    private var streamJob: Job? = null
    private var pingJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for continuous stream
        .build()

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame = _latestFrame.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting = _isConnecting.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(CameraTelemetry())
    val telemetry = _telemetry.asStateFlow()

    private val _measuredLatencyMs = MutableStateFlow(0L)
    val measuredLatencyMs = _measuredLatencyMs.asStateFlow()

    private val _receivedFps = MutableStateFlow(0f)
    val receivedFps = _receivedFps.asStateFlow()

    private var frameCounter = 0
    private var lastFpsTime = System.currentTimeMillis()

    fun connect() {
        if (streamJob?.isActive == true) return
        _isConnecting.value = true
        _connectionStatus.value = "Connecting to $serverIp:$serverPort..."

        streamJob = clientScope.launch {
            while (isActive) {
                try {
                    val url = "http://$serverIp:$serverPort/stream?token=$authToken"
                    val request = Request.Builder().url(url).build()

                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _connectionStatus.value = "Connection rejected: ${response.code}"
                            _isConnected.value = false
                            _isConnecting.value = false
                            delay(3000)
                            return@use
                        }

                        _isConnected.value = true
                        _isConnecting.value = false
                        _connectionStatus.value = "Live Connected"
                        startTelemetryPing()

                        val inputStream = response.body?.byteStream()
                        if (inputStream != null) {
                            readMjpegStream(inputStream)
                        }
                    }
                } catch (e: Exception) {
                    _isConnected.value = false
                    _connectionStatus.value = "Connection lost: Reconnecting..."
                    Log.w(tag, "Stream error, auto-reconnecting in 2s", e)
                    delay(2000)
                }
            }
        }
    }

    private fun readMjpegStream(inputStream: InputStream) {
        val boundary = "TWINGUARD_MJPEG_BOUNDARY"
        val buffer = ByteArray(4096)
        var streamBuffer = ByteArrayOutputStream()

        while (clientScope.isActive && _isConnected.value) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            streamBuffer.write(buffer, 0, read)

            val currentBytes = streamBuffer.toByteArray()
            // Look for JPEG start (0xFF, 0xD8) and end (0xFF, 0xD9)
            val soi = findByteSequence(currentBytes, byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
            val eoi = findByteSequence(currentBytes, byteArrayOf(0xFF.toByte(), 0xD9.toByte()))

            if (soi != -1 && eoi != -1 && eoi > soi) {
                val jpegSize = (eoi + 2) - soi
                val jpegBytes = ByteArray(jpegSize)
                System.arraycopy(currentBytes, soi, jpegBytes, 0, jpegSize)

                try {
                    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                    if (bitmap != null) {
                        _latestFrame.value = bitmap

                        // Calculate received FPS
                        frameCounter++
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTime >= 1000) {
                            _receivedFps.value = (frameCounter * 1000f) / (now - lastFpsTime)
                            frameCounter = 0
                            lastFpsTime = now
                        }
                    }
                } catch (_: Exception) {}

                // Retain remaining unparsed bytes
                val remaining = currentBytes.size - (eoi + 2)
                streamBuffer = ByteArrayOutputStream()
                if (remaining > 0) {
                    streamBuffer.write(currentBytes, eoi + 2, remaining)
                }
            }
        }
    }

    private fun findByteSequence(data: ByteArray, pattern: ByteArray): Int {
        if (pattern.isEmpty() || data.size < pattern.size) return -1
        for (i in 0..data.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun startTelemetryPing() {
        pingJob?.cancel()
        pingJob = clientScope.launch {
            while (isActive && _isConnected.value) {
                try {
                    val start = System.currentTimeMillis()
                    val url = "http://$serverIp:$serverPort/telemetry?token=$authToken"
                    val req = Request.Builder().url(url).build()

                    okHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val rtt = System.currentTimeMillis() - start
                            _measuredLatencyMs.value = rtt
                            val bodyStr = resp.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            _telemetry.value = _telemetry.value.copy(
                                batteryPercent = json.optInt("batteryPercent", 100),
                                isCharging = json.optBoolean("isCharging", false),
                                temperatureCelsius = json.optDouble("temperatureCelsius", 28.0).toFloat(),
                                freeStorageMb = json.optLong("freeStorageMb", 1024),
                                currentFps = json.optDouble("currentFps", 30.0).toInt(),
                                bitrateKbps = json.optInt("bitrateKbps", 2400),
                                isRecording = json.optBoolean("isRecording", false),
                                isTorchOn = json.optBoolean("isTorchOn", false),
                                isFrontCamera = json.optBoolean("isFrontCamera", false),
                                isMotionDetected = json.optBoolean("isMotionDetected", false),
                                recordingDurationSec = json.optLong("recordingDurationSec", 0)
                            )
                        }
                    }
                } catch (_: Exception) {}
                delay(1200)
            }
        }
    }

    fun sendRemoteCommand(action: RemoteAction, onResult: (Boolean) -> Unit = {}) {
        clientScope.launch {
            try {
                val url = "http://$serverIp:$serverPort/api/command?token=$authToken"
                val json = JSONObject().apply { put("action", action.name) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url(url).post(body).build()

                okHttpClient.newCall(req).execute().use { resp ->
                    onResult(resp.isSuccessful)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to send command $action", e)
                onResult(false)
            }
        }
    }

    fun disconnect() {
        _isConnected.value = false
        _isConnecting.value = false
        _connectionStatus.value = "Disconnected"
        streamJob?.cancel()
        streamJob = null
        pingJob?.cancel()
        pingJob = null
        _latestFrame.value = null
    }
}
