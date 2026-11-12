package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.TwinGuardApp
import com.example.audio.AudioStreamManager
import com.example.camera.CameraManager
import com.example.camera.CctvCameraService
import com.example.data.model.CameraQuality
import com.example.data.model.CameraTelemetry
import com.example.data.model.QrPairingPayload
import com.example.data.model.RemoteAction
import com.example.motion.MotionDetectionEngine
import com.example.network.NetworkUtils
import com.example.network.pairing.QrPairingManager
import com.example.network.server.LocalCctvServer
import com.example.recording.LocalRecorder
import com.example.ui.components.CctvTopBar
import com.example.ui.components.HudOverlay
import com.example.ui.components.QrBitmapImage
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SecurityAlertRed
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val localRecorder = remember { LocalRecorder(context) }
    val audioManager = remember { AudioStreamManager(context) }

    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    val serverPort = 8080
    val sessionToken = remember { NetworkUtils.generateSessionToken() }
    val deviceId = remember { NetworkUtils.getDeviceId(context) }
    val cameraName = "TwinGuard Cam #1"

    // Permissions check
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermissions = perms[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // State
    var isTorchOn by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableLongStateOf(0L) }
    var isMotionDetected by remember { mutableStateOf(false) }
    var currentQuality by remember { mutableStateOf(CameraQuality.BALANCED) }
    var showQrSheet by remember { mutableStateOf(false) }
    var connectedClients by remember { mutableIntStateOf(0) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraManagerRef by remember { mutableStateOf<CameraManager?>(null) }
    var localServerRef by remember { mutableStateOf<LocalCctvServer?>(null) }

    // Recording file tracking
    var currentRecordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }

    // Motion Detection Engine
    val motionEngine = remember {
        MotionDetectionEngine { score, zone, snapshotBytes ->
            isMotionDetected = true
            scope.launch {
                localRecorder.recordMotionEvent(snapshotBytes, score, zone)
                delay(3000)
                isMotionDetected = false
            }
        }
    }

    // Telemetry Provider lambda
    val telemetryProvider = {
        val (freeMb, totalMb) = localRecorder.getStorageStats()
        CameraTelemetry(
            batteryPercent = TwinGuardApp.batteryPercent,
            isCharging = TwinGuardApp.isCharging,
            temperatureCelsius = TwinGuardApp.batteryTemperature,
            freeStorageMb = freeMb,
            totalStorageMb = totalMb,
            currentFps = cameraManagerRef?.currentFps?.toInt() ?: 30,
            latencyMs = 120,
            bitrateKbps = when (currentQuality) {
                CameraQuality.LOW -> 1200
                CameraQuality.BALANCED -> 2400
                CameraQuality.HIGH -> 4500
            },
            connectedClients = connectedClients,
            isRecording = isRecording,
            isTorchOn = isTorchOn,
            isFrontCamera = isFrontCamera,
            isMotionDetected = isMotionDetected,
            recordingDurationSec = recordingDurationSec,
            cameraName = cameraName,
            quality = currentQuality,
            serverIp = localIp,
            serverPort = serverPort
        )
    }

    // Start Local Server & Foreground Service
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            // Start Foreground Service
            val serviceIntent = Intent(context, CctvCameraService::class.java).apply {
                putExtra("camera_name", cameraName)
                putExtra("server_ip", localIp)
                putExtra("server_port", serverPort)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Start Local HTTP/MJPEG Server
            val server = LocalCctvServer(
                port = serverPort,
                authToken = sessionToken,
                cameraName = cameraName,
                serverIp = localIp,
                onCommandReceived = { action ->
                    scope.launch {
                        when (action) {
                            RemoteAction.TOGGLE_TORCH -> {
                                cameraManagerRef?.let {
                                    isTorchOn = it.toggleTorch()
                                }
                            }
                            RemoteAction.SWITCH_CAMERA -> {
                                cameraManagerRef?.let {
                                    isFrontCamera = it.switchCamera(lifecycleOwner, previewViewRef)
                                }
                            }
                            RemoteAction.ZOOM_IN -> {
                                cameraManagerRef?.zoomIn()
                                currentZoom = cameraManagerRef?.getZoomRatio() ?: 1f
                            }
                            RemoteAction.ZOOM_OUT -> {
                                cameraManagerRef?.zoomOut()
                                currentZoom = cameraManagerRef?.getZoomRatio() ?: 1f
                            }
                            RemoteAction.START_RECORDING -> {
                                if (!isRecording) {
                                    isRecording = true
                                    recordingStartTime = System.currentTimeMillis()
                                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    currentRecordingFile = File(localRecorder.recordingsDir, "CCTV_${ts}.mp4")
                                }
                            }
                            RemoteAction.STOP_RECORDING -> {
                                if (isRecording) {
                                    isRecording = false
                                    currentRecordingFile?.let { file ->
                                        // Finalize recording metadata
                                        localRecorder.registerRecording(
                                            file = file,
                                            title = "Manual Recording",
                                            durationSeconds = recordingDurationSec,
                                            resolution = currentQuality.label
                                        )
                                    }
                                    recordingDurationSec = 0L
                                }
                            }
                            RemoteAction.CAPTURE_SNAPSHOT -> {
                                Toast.makeText(context, "Snapshot captured by remote viewer", Toast.LENGTH_SHORT).show()
                            }
                            RemoteAction.TRIGGER_SIREN -> {
                                audioManager.playSirenSound(3)
                            }
                            RemoteAction.SET_QUALITY_LOW -> {
                                currentQuality = CameraQuality.LOW
                                cameraManagerRef?.setQuality(currentQuality, lifecycleOwner, previewViewRef)
                            }
                            RemoteAction.SET_QUALITY_BALANCED -> {
                                currentQuality = CameraQuality.BALANCED
                                cameraManagerRef?.setQuality(currentQuality, lifecycleOwner, previewViewRef)
                            }
                            RemoteAction.SET_QUALITY_HIGH -> {
                                currentQuality = CameraQuality.HIGH
                                cameraManagerRef?.setQuality(currentQuality, lifecycleOwner, previewViewRef)
                            }
                        }
                    }
                },
                telemetryProvider = telemetryProvider
            )
            server.start()
            localServerRef = server

            // Polling connected client count & recording timer
            while (isActive) {
                connectedClients = server.getConnectedClientCount()
                if (isRecording) {
                    recordingDurationSec = (System.currentTimeMillis() - recordingStartTime) / 1000
                }
                delay(1000)
            }
        }
    }

    // Cleanup when leaving
    DisposableEffect(Unit) {
        onDispose {
            localServerRef?.stop()
            cameraManagerRef?.release()
            audioManager.release()
            // Stop foreground service
            val stopIntent = Intent(context, CctvCameraService::class.java).apply {
                action = CctvCameraService.ACTION_STOP
            }
            context.startService(stopIntent)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "Camera Mode — Active",
                subtitle = "http://$localIp:$serverPort",
                onBack = onNavigateBack,
                actions = {
                    StatusBadge(
                        text = if (connectedClients > 0) "$connectedClients ONLINE" else "READY",
                        color = if (connectedClients > 0) SecurityGreen else AccentCyan
                    )
                    IconButton(
                        onClick = { showQrSheet = true },
                        modifier = Modifier.testTag("camera_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasPermissions) {
                // Camera Viewfinder
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        previewViewRef = previewView

                        val manager = CameraManager(
                            context = ctx,
                            onFrameEncoded = { jpegBytes ->
                                localServerRef?.broadcastFrame(jpegBytes)
                            },
                            motionEngine = motionEngine
                        )
                        cameraManagerRef = manager
                        manager.initialize(lifecycleOwner, previewView)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // HUD Overlay (Diagnostics, Recording Indicator, Motion Alert)
                HudOverlay(
                    telemetry = telemetryProvider(),
                    isRecording = isRecording,
                    recordingDurationSec = recordingDurationSec,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Laptop URL quick indicator banner
                Surface(
                    color = DarkSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                ) {
                    Text(
                        text = "Laptop Viewer: http://$localIp:$serverPort?token=$sessionToken",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // On-Screen Camera Controls Bar
                Surface(
                    color = DarkSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Torch Toggle
                        IconButton(
                            onClick = {
                                cameraManagerRef?.let {
                                    isTorchOn = it.toggleTorch()
                                }
                            },
                            modifier = Modifier.testTag("btn_torch")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Torch",
                                tint = if (isTorchOn) WarningAmber else Color.White
                            )
                        }

                        // Switch Camera Lens
                        IconButton(
                            onClick = {
                                cameraManagerRef?.let {
                                    isFrontCamera = it.switchCamera(lifecycleOwner, previewViewRef)
                                }
                            },
                            modifier = Modifier.testTag("btn_switch_cam")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }

                        // Record Button (Hero)
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    isRecording = false
                                    Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
                                } else {
                                    isRecording = true
                                    recordingStartTime = System.currentTimeMillis()
                                    Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) SecurityAlertRed else AccentCyan)
                                .testTag("btn_record")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecording) "Stop Record" else "Start Record",
                                tint = if (isRecording) Color.White else Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Manual Snapshot
                        IconButton(
                            onClick = {
                                scope.launch {
                                    Toast.makeText(context, "Snapshot saved to Screenshots", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("btn_snapshot")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Take Snapshot",
                                tint = Color.White
                            )
                        }

                        // Siren Alarm
                        IconButton(
                            onClick = {
                                audioManager.playSirenSound(3)
                                Toast.makeText(context, "Siren triggered", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_siren")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Trigger Siren",
                                tint = SecurityAlertRed
                            )
                        }
                    }
                }
            } else {
                // Permission Denied UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera & Microphone Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TwinGuard CCTV requires camera and audio permissions to stream locally without internet.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        modifier = Modifier.testTag("request_camera_perm_button")
                    ) {
                        Text("Grant Permissions", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Dynamic QR Code Pairing
    if (showQrSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val payload = remember {
            QrPairingPayload(
                deviceId = deviceId,
                deviceName = cameraName,
                ip = localIp,
                port = serverPort,
                token = sessionToken,
                expiresAt = System.currentTimeMillis() + (30 * 60 * 1000) // 30 mins
            )
        }
        val qrString = remember { QrPairingManager.createPayloadString(payload) }
        val qrBitmap = remember { QrPairingManager.generateQrBitmap(qrString, 512) }

        ModalBottomSheet(
            onDismissRequest = { showQrSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAIR WITH MONITOR OR LAPTOP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan this QR with Phone B in Monitor Mode, or enter the URL below in Chrome on your laptop.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (qrBitmap != null) {
                    QrBitmapImage(bitmap = qrBitmap)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DIRECT BROWSER URL", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "http://$localIp:$serverPort?token=$sessionToken",
                            fontSize = 13.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Token: $sessionToken • Valid 30 mins",
                            fontSize = 11.sp,
                            color = WarningAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showQrSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Pairing Code", color = TextPrimary)
                }
            }
        }
    }
}
