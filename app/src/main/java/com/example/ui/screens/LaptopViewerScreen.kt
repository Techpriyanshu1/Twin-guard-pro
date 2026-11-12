package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.NetworkUtils
import com.example.network.pairing.QrPairingManager
import com.example.ui.components.CctvTopBar
import com.example.ui.components.QrBitmapImage
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun LaptopViewerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    val port = 8080
    val sessionToken = remember { NetworkUtils.generateSessionToken() }
    val url = "http://$localIp:$port"
    val fullUrl = "$url?token=$sessionToken"
    val qrBitmap = remember { QrPairingManager.generateQrBitmap(fullUrl, 400) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "Laptop Browser Viewer",
                subtitle = "Local Web CCTV Dashboard",
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
            // Hero card with URL
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LAPTOP DASHBOARD URL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                letterSpacing = 1.sp
                            )
                            StatusBadge(text = "NO INTERNET NEEDED", color = SecurityGreen, isPulsing = false)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fullUrl,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("CCTV URL", fullUrl))
                                        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy URL",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Open Chrome, Edge, Safari, or Firefox on your laptop and type this exact address.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // QR Code for quick laptop browser opening
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SCAN FROM ANOTHER BROWSER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (qrBitmap != null) {
                            QrBitmapImage(bitmap = qrBitmap)
                        }
                    }
                }
            }

            // Connection Scenarios
            item {
                Text(
                    text = "CONNECTION SETUP OPTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                SetupOptionCard(
                    number = "1",
                    title = "Option A: Mobile Hotspot (Recommended Offline)",
                    desc = "Turn ON Hotspot on this phone. Connect your laptop to this hotspot Wi-Fi. No internet or router required!",
                    icon = Icons.Default.WifiTethering,
                    color = AccentCyan
                )
            }

            item {
                SetupOptionCard(
                    number = "2",
                    title = "Option B: Same Wi-Fi Router",
                    desc = "Connect both this phone and your laptop to the same Wi-Fi router (even without public internet connection).",
                    icon = Icons.Default.Router,
                    color = SecurityGreen
                )
            }

            item {
                SetupOptionCard(
                    number = "3",
                    title = "Option C: Wi-Fi Direct",
                    desc = "Direct device-to-device wireless link. Ideal for outdoor surveillance with zero infrastructure.",
                    icon = Icons.Default.Laptop,
                    color = WarningAmber
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SetupOptionCard(
    number: String,
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
