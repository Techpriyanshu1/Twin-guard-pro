package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TwinGuardApp
import com.example.network.NetworkUtils
import com.example.ui.components.CctvTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SecurityAlertRed
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToLaptopGuide: () -> Unit,
    onNavigateToRecordings: () -> Unit,
    onNavigateToMotionEvents: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    val networkType = remember { NetworkUtils.getNetworkTypeName(context) }
    val wifiSsid = remember { NetworkUtils.getWifiSsid(context) }
    val batteryPct = TwinGuardApp.batteryPercent
    val isCharging = TwinGuardApp.isCharging

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "TwinGuard CCTV",
                subtitle = "Offline Security Camera System v2.0",
                actions = {
                    IconButton(
                        onClick = onNavigateToDiagnostics,
                        modifier = Modifier.testTag("home_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Diagnostics",
                            tint = AccentCyan
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Network & Status Banner
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Network",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$wifiSsid ($networkType)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "IP: $localIp",
                                    fontSize = 12.sp,
                                    color = AccentCyan
                                )
                            }
                        }

                        StatusBadge(
                            text = if (batteryPct > 20) "$batteryPct% ${if (isCharging) "⚡" else ""}" else "LOW $batteryPct%",
                            color = if (batteryPct > 20) SecurityGreen else SecurityAlertRed,
                            isPulsing = false
                        )
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = "SELECT OPERATING MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // 1. CAMERA MODE
            item {
                ModeCard(
                    title = "CAMERA MODE",
                    subtitle = "Transform this Android phone into a standalone wireless CCTV camera.",
                    tagline = "Local streaming • On-device motion detection • Continuous MP4 recording",
                    icon = Icons.Default.CameraAlt,
                    accentColor = AccentCyan,
                    badgeText = "CAMERA HOST",
                    testTag = "btn_camera_mode",
                    onClick = onNavigateToCamera
                )
            }

            // 2. MONITOR MODE
            item {
                ModeCard(
                    title = "MONITOR MODE",
                    subtitle = "Connect to another phone and watch its live camera stream in real time.",
                    tagline = "QR auto-pairing • Remote PTZ controls • Two-way audio • PiP",
                    icon = Icons.Default.Videocam,
                    accentColor = SecurityGreen,
                    badgeText = "CCTV VIEWER",
                    testTag = "btn_monitor_mode",
                    onClick = onNavigateToMonitor
                )
            }

            // 3. LAPTOP VIEWER
            item {
                ModeCard(
                    title = "LAPTOP VIEWER",
                    subtitle = "Monitor the camera live through Chrome or Edge on any laptop or desktop.",
                    tagline = "Direct LAN web dashboard • Zero software install • Full control panel",
                    icon = Icons.Default.Laptop,
                    accentColor = WarningAmber,
                    badgeText = "WEB BROWSER",
                    testTag = "btn_laptop_mode",
                    onClick = onNavigateToLaptopGuide
                )
            }

            // Section Header
            item {
                Text(
                    text = "SURVEILLANCE VAULT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Quick Access Cards: Recordings & Motion Events
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VaultCard(
                        title = "Recordings",
                        subtitle = "Saved MP4 clips",
                        icon = Icons.Default.VideoLibrary,
                        testTag = "vault_recordings_button",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRecordings
                    )
                    VaultCard(
                        title = "Motion Events",
                        subtitle = "Trigger logs & snaps",
                        icon = Icons.Default.MotionPhotosOn,
                        testTag = "vault_motion_button",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMotionEvents
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    tagline: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                StatusBadge(text = badgeText, color = accentColor, isPulsing = false)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun VaultCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
