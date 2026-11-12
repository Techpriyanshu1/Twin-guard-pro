package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioStreamManager
import com.example.data.model.RemoteAction
import com.example.data.model.SirenType
import com.example.network.client.MonitorClient
import com.example.ui.components.CctvTopBar
import com.example.ui.components.HudOverlay
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

@Composable
fun MonitorScreen(
    serverIp: String,
    serverPort: Int,
    authToken: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val monitorClient = remember(serverIp, serverPort, authToken) {
        MonitorClient(serverIp, serverPort, authToken)
    }
    val audioManager = remember { AudioStreamManager(context) }

    val latestFrame by monitorClient.latestFrame.collectAsState()
    val isConnected by monitorClient.isConnected.collectAsState()
    val isConnecting by monitorClient.isConnecting.collectAsState()
    val connStatus by monitorClient.connectionStatus.collectAsState()
    val telemetry by monitorClient.telemetry.collectAsState()
    val latencyMs by monitorClient.measuredLatencyMs.collectAsState()
    val receivedFps by monitorClient.receivedFps.collectAsState()

    var isMicTransmitting by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showSirenSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        monitorClient.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            monitorClient.disconnect()
            audioManager.release()
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            if (!isFullScreen) {
                CctvTopBar(
                    title = "Monitor Mode",
                    subtitle = "$serverIp:$serverPort",
                    onBack = onNavigateBack,
                    actions = {
                        StatusBadge(
                            text = if (isConnected) "LIVE" else if (isConnecting) "CONNECTING" else "OFFLINE",
                            color = if (isConnected) SecurityGreen else if (isConnecting) WarningAmber else SecurityAlertRed
                        )
                        IconButton(
                            onClick = { showSirenSheet = true },
                            modifier = Modifier.testTag("monitor_sirens_btn")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "8 Sirens", tint = WarningAmber)
                        }
                        IconButton(
                            onClick = { isFullScreen = true },
                            modifier = Modifier.testTag("monitor_fullscreen_button")
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) 0.dp else padding.calculateTopPadding())
                .background(Color.Black)
        ) {
            // Live Video Feed
            if (latestFrame != null) {
                Image(
                    bitmap = latestFrame!!.asImageBitmap(),
                    contentDescription = "Live Remote CCTV Feed",
                    contentScale = if (isFullScreen) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = connStatus, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Connecting via local Wi-Fi / Hotspot...", color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Real-Time HUD Overlay
            HudOverlay(
                telemetry = telemetry.copy(
                    latencyMs = latencyMs,
                    currentFps = receivedFps.toInt()
                ),
                isRecording = telemetry.isRecording,
                recordingDurationSec = telemetry.recordingDurationSec,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Human Motion Alert Banner
            AnimatedVisibility(
                visible = telemetry.isHumanMotion || telemetry.isMotionDetected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
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
                            text = if (telemetry.isHumanMotion) "⚠️ HUMAN INTRUDER DETECTED" else "⚠️ MOTION DETECTED",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable {
                                    monitorClient.sendRemoteCommand(RemoteAction.TRIGGER_SIREN_1)
                                    Toast.makeText(context, "Siren triggered on camera!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = "SIREN",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Exit Fullscreen Floating Button
            if (isFullScreen) {
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurface.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Exit Fullscreen", tint = Color.White)
                }
            }

            // Material You Floating Remote Island
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
                        listOf(
                            1.0f to RemoteAction.SET_ZOOM_1X,
                            2.0f to RemoteAction.SET_ZOOM_2X,
                            3.0f to RemoteAction.SET_ZOOM_3X,
                            5.0f to RemoteAction.SET_ZOOM_5X
                        ).forEach { (level, action) ->
                            val label = "${level.toInt()}x"
                            val isSelected = telemetry.zoomLevel == level
                            Surface(
                                color = if (isSelected) AccentCyan else DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable {
                                    monitorClient.sendRemoteCommand(action)
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remote Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Remote Torch
                        IconButton(
                            onClick = { monitorClient.sendRemoteCommand(RemoteAction.TOGGLE_TORCH) },
                            modifier = Modifier.testTag("remote_torch_btn")
                        ) {
                            Icon(
                                imageVector = if (telemetry.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Remote Torch",
                                tint = if (telemetry.isTorchOn) WarningAmber else Color.White
                            )
                        }

                        // Remote Flip Lens
                        IconButton(
                            onClick = { monitorClient.sendRemoteCommand(RemoteAction.SWITCH_CAMERA) },
                            modifier = Modifier.testTag("remote_switch_cam_btn")
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Flip", tint = Color.White)
                        }

                        // Walkie-Talkie Push-to-Talk
                        IconButton(
                            onClick = {
                                if (!isMicTransmitting) {
                                    isMicTransmitting = true
                                    audioManager.startRecording {}
                                    Toast.makeText(context, "Walkie-Talkie Transmitting", Toast.LENGTH_SHORT).show()
                                } else {
                                    isMicTransmitting = false
                                    audioManager.stopRecording()
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isMicTransmitting) AccentCyan else DarkSurfaceElevated)
                                .testTag("remote_ptt_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Talk",
                                tint = if (isMicTransmitting) Color.Black else Color.White
                            )
                        }

                        // Remote 10s Record / Continuous
                        IconButton(
                            onClick = {
                                if (telemetry.isRecording) {
                                    monitorClient.sendRemoteCommand(RemoteAction.STOP_RECORDING)
                                } else {
                                    monitorClient.sendRemoteCommand(RemoteAction.RECORD_10S_CLIP)
                                    Toast.makeText(context, "Started 10s clip recording", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("remote_record_btn")
                        ) {
                            Icon(
                                imageVector = if (telemetry.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = "Record",
                                tint = if (telemetry.isRecording) SecurityAlertRed else Color.White
                            )
                        }

                        // 8 Sirens Dialog Button
                        IconButton(
                            onClick = { showSirenSheet = true },
                            modifier = Modifier.testTag("remote_sirens_sheet_btn")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Sirens", tint = SecurityAlertRed)
                        }

                        // Remote Snapshot
                        IconButton(
                            onClick = {
                                monitorClient.sendRemoteCommand(RemoteAction.CAPTURE_SNAPSHOT)
                                Toast.makeText(context, "Snapshot captured", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("remote_snap_btn")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Snapshot", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Siren Selector Sheet for Remote Camera Trigger
    if (showSirenSheet) {
        SirenSelectorBottomSheet(
            onSelectSiren = { siren ->
                val action = when (siren.id) {
                    1 -> RemoteAction.TRIGGER_SIREN_1
                    2 -> RemoteAction.TRIGGER_SIREN_2
                    3 -> RemoteAction.TRIGGER_SIREN_3
                    4 -> RemoteAction.TRIGGER_SIREN_4
                    5 -> RemoteAction.TRIGGER_SIREN_5
                    6 -> RemoteAction.TRIGGER_SIREN_6
                    7 -> RemoteAction.TRIGGER_SIREN_7
                    8 -> RemoteAction.TRIGGER_SIREN_8
                    else -> RemoteAction.TRIGGER_SIREN_1
                }
                monitorClient.sendRemoteCommand(action)
                Toast.makeText(context, "Triggered ${siren.title} on camera", Toast.LENGTH_SHORT).show()
                showSirenSheet = false
            },
            onStopSiren = {
                monitorClient.sendRemoteCommand(RemoteAction.STOP_SIREN)
            },
            onDismiss = { showSirenSheet = false }
        )
    }
}
