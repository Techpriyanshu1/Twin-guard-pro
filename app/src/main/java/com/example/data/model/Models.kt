package com.example.data.model

enum class CameraQuality(val label: String, val width: Int, val height: Int, val targetFps: Int) {
    LOW("480p SD", 640, 480, 24),
    BALANCED("720p HD", 1280, 720, 30),
    HIGH("1080p FHD", 1920, 1080, 30)
}

enum class ConnectionType(val label: String) {
    LOCAL_WIFI("Local Wi-Fi Router"),
    HOTSPOT("Mobile Hotspot (No Router)"),
    WIFI_DIRECT("Wi-Fi Direct (P2P)"),
    MANUAL_IP("Manual LAN IP")
}

data class CameraTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 28.5f,
    val freeStorageMb: Long = 1024,
    val totalStorageMb: Long = 64000,
    val currentFps: Int = 30,
    val latencyMs: Long = 180,
    val bitrateKbps: Int = 2400,
    val connectedClients: Int = 0,
    val isRecording: Boolean = false,
    val isTorchOn: Boolean = false,
    val isFrontCamera: Boolean = false,
    val isMotionDetected: Boolean = false,
    val motionScore: Int = 0,
    val recordingDurationSec: Long = 0L,
    val cameraName: String = "TwinGuard Cam 1",
    val quality: CameraQuality = CameraQuality.BALANCED,
    val serverIp: String = "127.0.0.1",
    val serverPort: Int = 8080
)

data class QrPairingPayload(
    val v: Int = 2,
    val deviceId: String,
    val deviceName: String,
    val ip: String,
    val port: Int,
    val token: String,
    val expiresAt: Long,
    val transport: String = "LOCAL_WIFI"
)

data class MotionZoneConfig(
    val sensitivity: Int = 50, // 1 to 100
    val autoRecord: Boolean = true,
    val cooldownSeconds: Int = 10,
    val alertSound: Boolean = false,
    val activeZone: String = "FULL_FRAME" // "FULL_FRAME", "CENTER", "ENTRYWAY"
)

enum class RemoteAction {
    TOGGLE_TORCH,
    SWITCH_CAMERA,
    ZOOM_IN,
    ZOOM_OUT,
    START_RECORDING,
    STOP_RECORDING,
    CAPTURE_SNAPSHOT,
    SET_QUALITY_LOW,
    SET_QUALITY_BALANCED,
    SET_QUALITY_HIGH,
    TRIGGER_SIREN
}
