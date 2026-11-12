package com.example.network.server

import android.util.Log
import com.example.data.model.CameraTelemetry
import com.example.data.model.RemoteAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class LocalCctvServer(
    private val port: Int = 8080,
    private val authToken: String,
    private val cameraName: String,
    private val serverIp: String,
    private val onCommandReceived: (RemoteAction) -> Unit,
    private val telemetryProvider: () -> CameraTelemetry
) {
    private val tag = "LocalCctvServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO)

    // Bounded streaming clients
    private val activeStreams = ConcurrentHashMap<String, StreamClient>()
    @Volatile private var latestFrame: ByteArray? = null

    class StreamClient(
        val id: String,
        val outputStream: OutputStream,
        val socket: Socket
    ) {
        val frameQueue = ConcurrentLinkedQueue<ByteArray>()
        @Volatile var isAlive = true
    }

    fun start() {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = serverScope.launch {
            try {
                serverSocket = ServerSocket(port)
                serverSocket?.reuseAddress = true
                Log.d(tag, "Local CCTV Server listening on port $port")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch { handleClient(clientSocket) }
                    } catch (e: SocketException) {
                        break
                    } catch (e: Exception) {
                        Log.e(tag, "Error accepting client", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start server on port $port", e)
            }
        }
    }

    fun stop() {
        try {
            serverJob?.cancel()
            serverSocket?.close()
            serverSocket = null
            activeStreams.values.forEach { client ->
                client.isAlive = false
                try { client.socket.close() } catch (_: Exception) {}
            }
            activeStreams.clear()
            Log.d(tag, "Local CCTV Server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping server", e)
        }
    }

    fun broadcastFrame(jpegBytes: ByteArray) {
        latestFrame = jpegBytes
        if (activeStreams.isEmpty()) return

        for ((id, client) in activeStreams) {
            if (!client.isAlive || client.socket.isClosed) {
                activeStreams.remove(id)
                continue
            }
            // Keep queue strictly bounded to 1 frame to ensure ~100-200ms latency without buffering!
            while (client.frameQueue.size >= 1) {
                client.frameQueue.poll() // Drop outdated frame
            }
            client.frameQueue.offer(jpegBytes)
        }
    }

    fun getConnectedClientCount(): Int = activeStreams.size

    private fun handleClient(socket: Socket) {
        val clientId = "${socket.inetAddress.hostAddress}:${socket.port}"
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine() ?: return
            val parts = firstLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")
            val query = if (fullPath.contains("?")) fullPath.substringAfter("?") else ""

            val headers = mutableMapOf<String, String>()
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val colon = line!!.indexOf(":")
                if (colon > 0) {
                    val key = line!!.substring(0, colon).trim().lowercase()
                    val value = line!!.substring(colon + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            // Validate token for secured endpoints (allow dashboard viewing if token matches or root)
            val tokenParam = parseQueryParam(query, "token")
            val isValidToken = authToken.isEmpty() || tokenParam == authToken || headers["x-auth-token"] == authToken

            when {
                path == "/" || path == "/index.html" -> {
                    serveDashboard(socket.getOutputStream(), tokenParam ?: authToken)
                    socket.close()
                }
                path == "/stream" -> {
                    if (!isValidToken && authToken.isNotEmpty()) {
                        sendUnauthorized(socket.getOutputStream())
                        socket.close()
                        return
                    }
                    serveMjpegStream(clientId, socket)
                }
                path == "/snapshot" -> {
                    serveSnapshot(socket.getOutputStream())
                    socket.close()
                }
                path == "/telemetry" -> {
                    serveTelemetry(socket.getOutputStream())
                    socket.close()
                }
                path == "/api/command" && method.equals("POST", ignoreCase = true) -> {
                    val body = CharArray(contentLength)
                    reader.read(body, 0, contentLength)
                    val bodyStr = String(body)
                    handleCommand(bodyStr, socket.getOutputStream())
                    socket.close()
                }
                else -> {
                    sendNotFound(socket.getOutputStream())
                    socket.close()
                }
            }
        } catch (e: Exception) {
            // Client disconnected or timed out
        }
    }

    private fun serveDashboard(out: OutputStream, token: String) {
        val html = WebDashboardHtml.getDashboardHtml(cameraName, serverIp, port, token)
        val bytes = html.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(response.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun serveSnapshot(out: OutputStream) {
        val frame = latestFrame
        if (frame != null) {
            val response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: ${frame.size}\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(response.toByteArray())
            out.write(frame)
        } else {
            val err = "No frame available".toByteArray()
            val response = "HTTP/1.1 503 Service Unavailable\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: ${err.size}\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(response.toByteArray())
            out.write(err)
        }
        out.flush()
    }

    private fun serveTelemetry(out: OutputStream) {
        val telemetry = telemetryProvider().copy(connectedClients = activeStreams.size)
        val json = JSONObject().apply {
            put("batteryPercent", telemetry.batteryPercent)
            put("isCharging", telemetry.isCharging)
            put("temperatureCelsius", telemetry.temperatureCelsius)
            put("freeStorageMb", telemetry.freeStorageMb)
            put("totalStorageMb", telemetry.totalStorageMb)
            put("currentFps", telemetry.currentFps)
            put("bitrateKbps", telemetry.bitrateKbps)
            put("connectedClients", telemetry.connectedClients)
            put("isRecording", telemetry.isRecording)
            put("isTorchOn", telemetry.isTorchOn)
            put("isStrobeActive", telemetry.isStrobeActive)
            put("isFrontCamera", telemetry.isFrontCamera)
            put("isMotionDetected", telemetry.isMotionDetected)
            put("isHumanMotion", telemetry.isHumanMotion)
            put("zoomLevel", telemetry.zoomLevel.toDouble())
            put("isPrivacyMode", telemetry.isPrivacyMode)
            put("recordingDurationSec", telemetry.recordingDurationSec)
            put("quality", telemetry.quality.label)
            put("activeSiren", telemetry.activeSiren?.title ?: "")
        }
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(response.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun handleCommand(body: String, out: OutputStream) {
        try {
            val json = JSONObject(body)
            val actionStr = json.optString("action", "")
            val action = try { RemoteAction.valueOf(actionStr) } catch (_: Exception) { null }

            if (action != null) {
                onCommandReceived(action)
                val resp = """{"status":"ok","action":"$actionStr"}""".toByteArray()
                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: ${resp.size}\r\n\r\n".toByteArray())
                out.write(resp)
            } else {
                val resp = """{"status":"error","message":"unknown action"}""".toByteArray()
                out.write("HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\nContent-Length: ${resp.size}\r\n\r\n".toByteArray())
                out.write(resp)
            }
        } catch (e: Exception) {
            val resp = """{"status":"error","message":"${e.message}"}""".toByteArray()
            out.write("HTTP/1.1 500 Server Error\r\nContent-Type: application/json\r\nContent-Length: ${resp.size}\r\n\r\n".toByteArray())
            out.write(resp)
        }
        out.flush()
    }

    private fun serveMjpegStream(clientId: String, socket: Socket) {
        val out = socket.getOutputStream()
        val boundary = "TWINGUARD_MJPEG_BOUNDARY"
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n" +
                "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                "Pragma: no-cache\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.flush()

        val client = StreamClient(clientId, out, socket)
        activeStreams[clientId] = client

        try {
            while (client.isAlive && !socket.isClosed) {
                val frame = client.frameQueue.poll()
                if (frame != null) {
                    val frameHeader = "--$boundary\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${frame.size}\r\n\r\n"
                    out.write(frameHeader.toByteArray())
                    out.write(frame)
                    out.write("\r\n".toByteArray())
                    out.flush()
                } else {
                    Thread.sleep(15) // ~60fps poll interval
                }
            }
        } catch (e: Exception) {
            // Client closed stream connection
        } finally {
            client.isAlive = false
            activeStreams.remove(clientId)
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendUnauthorized(out: OutputStream) {
        val msg = "401 Unauthorized - Invalid pairing token\r\n".toByteArray()
        out.write("HTTP/1.1 401 Unauthorized\r\nContent-Length: ${msg.size}\r\n\r\n".toByteArray())
        out.write(msg)
        out.flush()
    }

    private fun sendNotFound(out: OutputStream) {
        val msg = "404 Not Found\r\n".toByteArray()
        out.write("HTTP/1.1 404 Not Found\r\nContent-Length: ${msg.size}\r\n\r\n".toByteArray())
        out.write(msg)
        out.flush()
    }

    private fun parseQueryParam(query: String, key: String): String? {
        if (query.isEmpty()) return null
        val pairs = query.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size == 2 && parts[0] == key) {
                return parts[1]
            }
        }
        return null
    }
}
