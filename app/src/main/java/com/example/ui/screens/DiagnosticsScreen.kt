package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.TwinGuardApp
import com.example.camera.CctvCameraService
import com.example.network.NetworkUtils
import com.example.recording.LocalRecorder
import com.example.ui.components.CctvTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SecurityAlertRed
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    val networkType = remember { NetworkUtils.getNetworkTypeName(context) }
    val wifiSsid = remember { NetworkUtils.getWifiSsid(context) }
    val localRecorder = remember { LocalRecorder(context) }
    val (freeMb, totalMb) = remember { localRecorder.getStorageStats() }

    val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val isServiceRunning = CctvCameraService.isRunning

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "Technical Diagnostics",
                subtitle = "Hardware, Network & Stream Telemetry",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Local Network Telemetry
            item {
                DiagnosticCard(title = "LOCAL NETWORK & CONNECTIVITY") {
                    DiagRow(label = "Wi-Fi SSID", value = wifiSsid, statusColor = AccentCyan)
                    DiagRow(label = "Interface Type", value = networkType, statusColor = TextPrimary)
                    DiagRow(label = "Assigned Local IPv4", value = localIp, statusColor = AccentCyan)
                    DiagRow(label = "Broadcast Port", value = "8080 (HTTP/1.1)", statusColor = TextPrimary)
                    DiagRow(label = "Internet Dependency", value = "None (100% Offline LAN)", statusColor = SecurityGreen)
                }
            }

            // Stream Engine & Server Status
            item {
                DiagnosticCard(title = "CCTV STREAMING PIPELINE") {
                    DiagRow(
                        label = "CCTV Server Daemon",
                        value = if (isServiceRunning) "RUNNING (Active)" else "STANDBY",
                        statusColor = if (isServiceRunning) SecurityGreen else WarningAmber
                    )
                    DiagRow(label = "Streaming Protocol", value = "Low-Latency MJPEG / HTTP", statusColor = TextPrimary)
                    DiagRow(label = "Video Frame Pipeline", value = "CameraX RGBA -> JPEG Stream", statusColor = TextPrimary)
                    DiagRow(label = "Target Frame Rate", value = "30.0 FPS Adaptive", statusColor = AccentCyan)
                    DiagRow(label = "Buffer Queue Policy", value = "Bounded (1 Frame) - Zero Lag", statusColor = SecurityGreen)
                    DiagRow(label = "Estimated End-to-End Latency", value = "~120 - 180 ms (Direct LAN)", statusColor = AccentCyan)
                }
            }

            // Hardware, Thermal & Battery Telemetry
            item {
                DiagnosticCard(title = "DEVICE TELEMETRY & HARDWARE HEALTH") {
                    DiagRow(
                        label = "Battery Charge",
                        value = "${TwinGuardApp.batteryPercent}% (${if (TwinGuardApp.isCharging) "Charging ⚡" else "Discharging"})",
                        statusColor = if (TwinGuardApp.batteryPercent > 20) SecurityGreen else SecurityAlertRed
                    )
                    DiagRow(
                        label = "Battery Temperature",
                        value = "${TwinGuardApp.batteryTemperature}°C",
                        statusColor = if (TwinGuardApp.batteryTemperature < 42f) SecurityGreen else WarningAmber
                    )
                    DiagRow(
                        label = "Storage Available",
                        value = "${freeMb / 1024} GB free of ${totalMb / 1024} GB",
                        statusColor = if (freeMb > 500) SecurityGreen else SecurityAlertRed
                    )
                    DiagRow(
                        label = "Camera Hardware Access",
                        value = if (hasCam) "GRANTED" else "DENIED",
                        statusColor = if (hasCam) SecurityGreen else SecurityAlertRed
                    )
                    DiagRow(
                        label = "Microphone Access",
                        value = if (hasMic) "GRANTED" else "DENIED",
                        statusColor = if (hasMic) SecurityGreen else SecurityAlertRed
                    )
                }
            }

            // Security & Pairing Status
            item {
                DiagnosticCard(title = "PAIRING & PRIVACY ARCHITECTURE") {
                    DiagRow(label = "QR Token Expiry Policy", value = "30-minute Short-Lived Nonce", statusColor = TextPrimary)
                    DiagRow(label = "Cloud Uploads", value = "DISABLED (Zero External Traffic)", statusColor = SecurityGreen)
                    DiagRow(label = "Remote Access Gate", value = "Local Auth Token Required", statusColor = SecurityGreen)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun DiagnosticCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun DiagRow(
    label: String,
    value: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
}
