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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import com.example.data.model.MotionAlertEvent
import com.example.data.model.QrPairingPayload
import com.example.data.model.RemoteAction
import com.example.data.model.SirenType
import com.example.motion.MotionDetectionEngine
import com.example.network.NetworkUtils
import com.example.network.pairing.QrPairingManager
import com.example.network.server.LocalCctvServer
import com.example.recording.LocalRecorder
import com.example.ui.components.CctvTopBar
import com.example.ui.components.HudOverlay
import com.example.ui.components.QrBitmapImage
import com.example.ui.components.SirenSelectorBottomSheet
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
import kotlinx.coroutines.Job
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

    // Camera & Stream States
    var isTorchOn by remember { mutableStateOf(false) }
    var isStrobeOn by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableLongStateOf(0L) }
    var isMotionDetected by remember { mutableStateOf(false) }
    var isHumanMotion by remember { mutableStateOf(false) }
    var autoClipCountdown by remember { mutableIntStateOf(0) }
    var currentQuality by remember { mutableStateOf(CameraQuality.BALANCED) }
    var isStealthScreenOff by remember { mutableStateOf(false) }
    var isPrivacyMode by remember { mutableStateOf(false) }
    var isNightVision by remember { mutableStateOf(false) }

    var showQrSheet by remember { mutableStateOf(false) }
    var showSirenSheet by remember { mutableStateOf(false) }
    var connectedClients by remember { mutableIntStateOf(0) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraManagerRef by remember { mutableStateOf<CameraManager?>(null) }
    var localServerRef by remember { mutableStateOf<LocalCctvServer?>(null) }

    var currentRecordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    var tenSecRecordJob by remember { mutableStateOf<Job?>(null) }

    // Human motion 10-second automatic clip recording trigger
    fun trigger10SecondClip(isHuman: Boolean) {
        tenSecRecordJob?.cancel()
        tenSecRecordJob = scope.launch {
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(localRecorder.recordingsDir, "CLIP10S_${ts}.mp4")
            currentRecordingFile = file

            for (sec in 10 downTo 1) {
                autoClipCountdown = sec
                delay(1000)
            }
            autoClipCountdown = 0
            isRecording = false
            localRecorder.registerRecording(
                file = file,
                title = if (isHuman) "Human Intruder Clip" else "Motion Event Clip",
                durationSeconds = 10,
                isMotionEvent = true,
                resolution = currentQuality.label
            )
        }
    }

    // Motion Detection Engine with Human Silhouette Detection
    val motionEngine = remember {
        MotionDetectionEngine { score, zone, isHuman, snapshotBytes ->
            isMotionDetected = true
            isHumanMotion = isHuman

            scope.launch {
                localRecorder.recordMotionEvent(snapshotBytes, score, zone)
                trigger10SecondClip(isHuman)
                delay(4000)
                isMotionDetected = false
                isHumanMotion = false
            }
        }
    }

    // Telemetry Provider
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
            isStrobeActive = isStrobeOn,
            isFrontCamera = isFrontCamera,
            isMotionDetected = isMotionDetected,
            isHumanMotion = isHumanMotion,
            recordingDurationSec = recordingDurationSec,
            autoClipCountdownSec = autoClipCountdown,
            cameraName = cameraName,
            quality = currentQuality,
            zoomLevel = currentZoom,
            activeSiren = audioManager.currentSirenType,
            isPrivacyMode = isPrivacyMode,
            isNightVision = isNightVision,
            serverIp = localIp,
            serverPort = serverPort
        )
    }

    // Initialize Local Server & 24/7 Foreground Service
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
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
                                    isStrobeOn = false
                                }
                            }
                            RemoteAction.TOGGLE_STROBE -> {
                                cameraManagerRef?.let {
                                    isStrobeOn = it.toggleStrobe()
                                    isTorchOn = false
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
                            RemoteAction.SET_ZOOM_1X -> {
                                cameraManagerRef?.setZoom(1.0f)
                                currentZoom = 1.0f
                            }
                            RemoteAction.SET_ZOOM_2X -> {
                                cameraManagerRef?.setZoom(2.0f)
                                currentZoom = 2.0f
                            }
                            RemoteAction.SET_ZOOM_3X -> {
                                cameraManagerRef?.setZoom(3.0f)
                                currentZoom = 3.0f
                            }
                            RemoteAction.SET_ZOOM_5X -> {
                                cameraManagerRef?.setZoom(5.0f)
                                currentZoom = 5.0f
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
                            RemoteAction.RECORD_10S_CLIP -> {
                                trigger10SecondClip(isHuman = false)
                            }
                            RemoteAction.CAPTURE_SNAPSHOT -> {
                                Toast.makeText(context, "Snapshot captured", Toast.LENGTH_SHORT).show()
                            }
                            RemoteAction.TRIGGER_SIREN_1 -> audioManager.playSiren(SirenType.POLICE_WAIL, 5)
                            RemoteAction.TRIGGER_SIREN_2 -> audioManager.playSiren(SirenType.RAPID_YELP, 5)
                            RemoteAction.TRIGGER_SIREN_3 -> audioManager.playSiren(SirenType.AMBULANCE_TWO_TONE, 5)
                            RemoteAction.TRIGGER_SIREN_4 -> audioManager.playSiren(SirenType.PIERCING_INTRUDER, 5)
                            RemoteAction.TRIGGER_SIREN_5 -> audioManager.playSiren(SirenType.AIR_RAID_RUMBLE, 5)
                            RemoteAction.TRIGGER_SIREN_6 -> audioManager.playSiren(SirenType.CYBER_STROBE, 5)
                            RemoteAction.TRIGGER_SIREN_7 -> audioManager.playSiren(SirenType.GUARD_DOG_BURST, 5)
                            RemoteAction.TRIGGER_SIREN_8 -> audioManager.playSiren(SirenType.ULTRASONIC_DETERRENT, 5)
                            RemoteAction.STOP_SIREN -> audioManager.stopSiren()
                            RemoteAction.TOGGLE_PRIVACY_MODE -> {
                                isPrivacyMode = cameraManagerRef?.togglePrivacyMode() ?: false
                            }
                            RemoteAction.TOGGLE_NIGHT_VISION -> {
                                isNightVision = cameraManagerRef?.toggleNightVision() ?: false
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

            while (isActive) {
                connectedClients = server.getConnectedClientCount()
                if (isRecording && autoClipCountdown == 0) {
                    recordingDurationSec = (System.currentTimeMillis() - recordingStartTime) / 1000
                }
                delay(1000)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            localServerRef?.stop()
            cameraManagerRef?.release()
            audioManager.release()
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
            if (!isStealthScreenOff) {
                CctvTopBar(
                    title = "Camera Mode",
                    subtitle = "http://$localIp:$serverPort",
                    onBack = onNavigateBack,
                    actions = {
                        StatusBadge(
                            text = if (connectedClients > 0) "$connectedClients ONLINE" else "READY",
                            color = if (connectedClients > 0) SecurityGreen else AccentCyan
                        )
                        IconButton(
                            onClick = { showSirenSheet = true },
                            modifier = Modifier.testTag("open_siren_sheet")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "8 Sirens", tint = WarningAmber)
                        }
                        IconButton(
                            onClick = { showQrSheet = true },
                            modifier = Modifier.testTag("camera_qr_button")
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isStealthScreenOff) PaddingValues(0.dp) else padding)
        ) {
            if (hasPermissions) {
                // Live Viewfinder
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

                // Human Motion Detected Banner
                AnimatedVisibility(
                    visible = isHumanMotion,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        color = SecurityAlertRed,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color.White)
                            Text(
                                text = "HUMAN MOTION DETECTED • RECORDING 10S CLIP ($autoClipCountdown)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // HUD Overlay
                if (!isStealthScreenOff) {
                    HudOverlay(
                        telemetry = telemetryProvider(),
                        isRecording = isRecording,
                        recordingDurationSec = if (autoClipCountdown > 0) autoClipCountdown.toLong() else recordingDurationSec,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Material You Expressive Floating Control Island
                if (!isStealthScreenOff) {
                    Surface(
                        color = DarkSurface.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Zoom Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1.0f to "1x", 2.0f to "2x", 3.0f to "3x", 5.0f to "5x").forEach { (zoom, label) ->
                                    Surface(
                                        color = if (currentZoom == zoom) AccentCyan else DarkSurfaceElevated,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .clickable {
                                                cameraManagerRef?.setZoom(zoom)
                                                currentZoom = zoom
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentZoom == zoom) Color.Black else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Main Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Torch & Strobe
                                IconButton(
                                    onClick = {
                                        cameraManagerRef?.let {
                                            isTorchOn = it.toggleTorch()
                                            isStrobeOn = false
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

                                // Switch Camera
                                IconButton(
                                    onClick = {
                                        cameraManagerRef?.let {
                                            isFrontCamera = it.switchCamera(lifecycleOwner, previewViewRef)
                                        }
                                    },
                                    modifier = Modifier.testTag("btn_switch_cam")
                                ) {
                                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip", tint = Color.White)
                                }

                                // Central Hero Record Button
                                IconButton(
                                    onClick = {
                                        if (isRecording) {
                                            isRecording = false
                                            tenSecRecordJob?.cancel()
                                        } else {
                                            trigger10SecondClip(isHuman = false)
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
                                        contentDescription = "Record",
                                        tint = if (isRecording) Color.White else Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // 8 Sirens Sheet Button
                                IconButton(
                                    onClick = { showSirenSheet = true },
                                    modifier = Modifier.testTag("btn_sirens")
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Sirens", tint = WarningAmber)
                                }

                                // 24/7 Stealth Screen-Off Mode
                                IconButton(
                                    onClick = { isStealthScreenOff = true },
                                    modifier = Modifier.testTag("btn_stealth_mode")
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = "Stealth Mode", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // Stealth Screen-Off Overlay (OLED black, running 24/7 in background without burning screen)
                if (isStealthScreenOff) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable { isStealthScreenOff = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "24/7 STEALTH STREAM ACTIVE",
                                color = AccentCyan.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap anywhere to wake display",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Siren Selector Bottom Sheet
    if (showSirenSheet) {
        SirenSelectorBottomSheet(
            onSelectSiren = { siren ->
                audioManager.playSiren(siren, 6)
                showSirenSheet = false
            },
            onStopSiren = { audioManager.stopSiren() },
            onDismiss = { showSirenSheet = false }
        )
    }

    // Dynamic QR Modal Sheet
    if (showQrSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val payload = remember {
            QrPairingPayload(
                deviceId = deviceId,
                deviceName = cameraName,
                ip = localIp,
                port = serverPort,
                token = sessionToken,
                expiresAt = System.currentTimeMillis() + (30 * 60 * 1000)
            )
        }
        val qrString = remember { QrPairingManager.createPayloadString(payload) }
        val qrBitmap = remember { QrPairingManager.generateQrBitmap(qrString, 512) }

        ModalBottomSheet(
            onDismissRequest = { showQrSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                Spacer(modifier = Modifier.height(14.dp))
                if (qrBitmap != null) {
                    QrBitmapImage(bitmap = qrBitmap)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("DIRECT LAPTOP URL", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "http://$localIp:$serverPort?token=$sessionToken",
                            fontSize = 13.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showQrSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = TextPrimary)
                }
            }
        }
    }
}
