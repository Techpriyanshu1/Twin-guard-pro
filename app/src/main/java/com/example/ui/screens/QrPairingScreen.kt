package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.network.p2p.WifiDirectManager
import com.example.network.pairing.QrPairingManager
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
import java.util.concurrent.Executors

@Composable
fun QrPairingScreen(
    onPairSuccess: (ip: String, port: Int, token: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("QR Scanner", "Manual IP", "Wi-Fi Direct")

    // Manual input states
    var manualIp by remember { mutableStateOf("192.168.") }
    var manualPort by remember { mutableStateOf("8080") }
    var manualToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Wi-Fi Direct Manager
    val p2pManager = remember { WifiDirectManager(context) }
    val p2pPeers by p2pManager.peers.collectAsState()
    val p2pStatus by p2pManager.statusMessage.collectAsState()

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 2) {
            p2pManager.initialize()
            p2pManager.discoverPeers()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            p2pManager.unregister()
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "Connect to Camera",
                subtitle = "Pairing & Device Discovery",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = AccentCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentCyan
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) AccentCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // QR Scanner with live CameraX & ZXing
                    QrScannerView(
                        onQrDetected = { qrString ->
                            val (payload, error) = QrPairingManager.parsePayload(qrString)
                            if (payload != null) {
                                Toast.makeText(context, "Pairing with ${payload.deviceName}...", Toast.LENGTH_SHORT).show()
                                onPairSuccess(payload.ip, payload.port, payload.token)
                            } else {
                                errorMessage = error
                            }
                        }
                    )
                }
                1 -> {
                    // Manual IP & Token form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "MANUAL LAN CONNECTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Connect directly by entering the camera phone's IP address and pairing token shown on its screen.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Camera IP Address (e.g. 192.168.1.5)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_manual_ip"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port (Default: 8080)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_manual_port"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = manualToken,
                            onValueChange = { manualToken = it },
                            label = { Text("Session Token (Optional / From QR)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_manual_token"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = SecurityAlertRed,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                val portInt = manualPort.toIntOrNull() ?: 8080
                                if (manualIp.isNotBlank()) {
                                    onPairSuccess(manualIp.trim(), portInt, manualToken.trim())
                                } else {
                                    errorMessage = "Please enter a valid IP address"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_connect_manual")
                        ) {
                            Text("Connect to Camera", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                2 -> {
                    // Wi-Fi Direct Peer List
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "WI-FI DIRECT (P2P) DISCOVERY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            color = DarkSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Status: $p2pStatus",
                                color = SecurityGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Button(
                            onClick = { p2pManager.discoverPeers() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.WifiFind, contentDescription = "Scan", tint = AccentCyan)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Refresh P2P Devices", color = TextPrimary)
                        }

                        if (p2pPeers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Wi-Fi Direct devices found yet.\nMake sure Phone A has Wi-Fi turned on.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            p2pPeers.forEach { device ->
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(device.deviceName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(device.deviceAddress, fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Button(
                                            onClick = {
                                                p2pManager.connectToDevice(device) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                                        ) {
                                            Text("Pair", color = Color.Black, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrScannerView(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        val executor = Executors.newSingleThreadExecutor()
                        var scanCooldown = 0L

                        imageAnalysis.setAnalyzer(executor) { proxy ->
                            val now = System.currentTimeMillis()
                            if (now - scanCooldown > 800) {
                                try {
                                    val bitmap = proxy.toBitmap()
                                    val decoded = QrPairingManager.decodeBitmap(bitmap)
                                    if (decoded != null) {
                                        scanCooldown = now
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onQrDetected(decoded)
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            proxy.close()
                        }

                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Reticle Overlay
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center)
                    .border(2.dp, AccentCyan, RoundedCornerShape(20.dp))
            )

            Surface(
                color = DarkSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Point camera at Phone A's QR Code",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission required to scan QR", color = TextSecondary)
        }
    }
}
