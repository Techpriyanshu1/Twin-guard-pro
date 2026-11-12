package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraQuality
import com.example.ui.components.CctvTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SecurityAlertRed
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("twinguard_settings", Context.MODE_PRIVATE) }

    var sensitivity by remember { mutableFloatStateOf(prefs.getInt("motion_sensitivity", 50).toFloat()) }
    var autoRecord by remember { mutableStateOf(prefs.getBoolean("motion_auto_record", true)) }
    var alertSound by remember { mutableStateOf(prefs.getBoolean("motion_alert_sound", false)) }
    var selectedZone by remember { mutableStateOf(prefs.getString("motion_zone", "FULL_FRAME") ?: "FULL_FRAME") }
    var defaultQuality by remember { mutableStateOf(prefs.getString("default_quality", "BALANCED") ?: "BALANCED") }

    fun saveSettings() {
        prefs.edit()
            .putInt("motion_sensitivity", sensitivity.toInt())
            .putBoolean("motion_auto_record", autoRecord)
            .putBoolean("motion_alert_sound", alertSound)
            .putString("motion_zone", selectedZone)
            .putString("default_quality", defaultQuality)
            .apply()
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "Settings",
                subtitle = "Detection, Streaming & Security",
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
            // Motion Detection Section
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OFFLINE SMART MOTION DETECTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sensitivity
                        Text(
                            text = "Sensitivity: ${sensitivity.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Slider(
                            value = sensitivity,
                            onValueChange = {
                                sensitivity = it
                                saveSettings()
                            },
                            valueRange = 10f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("slider_sensitivity")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Auto Record Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto-Record on Motion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Captures timestamped MP4 clip upon detection", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = autoRecord,
                                onCheckedChange = {
                                    autoRecord = it
                                    saveSettings()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("switch_auto_record")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Alert Sound Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Audible Siren on Motion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Emits security beep on camera speaker", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = alertSound,
                                onCheckedChange = {
                                    alertSound = it
                                    saveSettings()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // Detection Zones
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ACTIVE DETECTION ZONE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val zones = listOf(
                            "FULL_FRAME" to "Full Screen Surveillance",
                            "CENTER" to "Center Focus Zone",
                            "ENTRYWAY" to "Lower Doorway / Entryway"
                        )

                        zones.forEach { (zoneKey, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedZone == zoneKey,
                                    onClick = {
                                        selectedZone = zoneKey
                                        saveSettings()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = if (selectedZone == zoneKey) AccentCyan else TextPrimary,
                                    fontWeight = if (selectedZone == zoneKey) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Default Stream Quality
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DEFAULT STREAM QUALITY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val qualities = listOf(
                            "LOW" to "480p SD (24 FPS, Low Bitrate / Minimal Heat)",
                            "BALANCED" to "720p HD (30 FPS, Recommended)",
                            "HIGH" to "1080p Full HD (30 FPS, Sharpest Detail)"
                        )

                        qualities.forEach { (qKey, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = defaultQuality == qKey,
                                    onClick = {
                                        defaultQuality = qKey
                                        saveSettings()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                                )
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = if (defaultQuality == qKey) AccentCyan else TextPrimary,
                                    fontWeight = if (defaultQuality == qKey) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Reset Token
            item {
                Button(
                    onClick = {
                        val newToken = com.example.network.NetworkUtils.generateSessionToken()
                        Toast.makeText(context, "New session token generated: $newToken", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Regenerate Pairing Token", color = TextPrimary)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
