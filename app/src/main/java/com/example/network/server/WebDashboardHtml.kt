package com.example.network.server

object WebDashboardHtml {

    fun getDashboardHtml(cameraName: String, serverIp: String, serverPort: Int, token: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TwinGuard CCTV — $cameraName</title>
    <style>
        :root {
            --bg-color: #090d16;
            --surface-color: #111827;
            --surface-elevated: #1e293b;
            --accent-cyan: #38bdf8;
            --accent-blue: #0284c7;
            --green: #10b981;
            --red: #ef4444;
            --amber: #f59e0b;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --border-color: #24334b;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
        }

        body {
            background-color: var(--bg-color);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        header {
            background-color: var(--surface-color);
            border-bottom: 1px solid var(--border-color);
            padding: 14px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .brand-badge {
            background: linear-gradient(135deg, #0284c7, #38bdf8);
            width: 38px;
            height: 38px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            color: #000;
            font-size: 16px;
            box-shadow: 0 0 16px rgba(56, 189, 248, 0.4);
        }

        .brand-text h1 {
            font-size: 18px;
            font-weight: 700;
            letter-spacing: 0.5px;
        }

        .brand-text p {
            font-size: 12px;
            color: var(--text-secondary);
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 14px;
            border-radius: 20px;
            background: rgba(16, 185, 129, 0.15);
            border: 1px solid rgba(16, 185, 129, 0.3);
            color: var(--green);
            font-size: 13px;
            font-weight: 600;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: var(--green);
            box-shadow: 0 0 8px var(--green);
            animation: pulse 1.5s infinite;
        }

        @keyframes pulse {
            0% { opacity: 0.6; }
            50% { opacity: 1; transform: scale(1.1); }
            100% { opacity: 0.6; }
        }

        .main-container {
            display: flex;
            flex: 1;
            padding: 20px;
            gap: 20px;
        }

        @media (max-width: 900px) {
            .main-container {
                flex-direction: column;
            }
        }

        .video-section {
            flex: 3;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .video-container {
            position: relative;
            background-color: #000;
            border-radius: 16px;
            overflow: hidden;
            border: 1px solid var(--border-color);
            box-shadow: 0 8px 32px rgba(0,0,0,0.6);
            aspect-ratio: 16 / 9;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        #liveFeed {
            width: 100%;
            height: 100%;
            object-fit: contain;
            background-color: #000;
        }

        .rec-indicator {
            position: absolute;
            top: 16px;
            left: 16px;
            display: none;
            align-items: center;
            gap: 6px;
            background: rgba(239, 68, 68, 0.85);
            color: #fff;
            padding: 6px 12px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 700;
            backdrop-filter: blur(4px);
        }

        .hud-overlay {
            position: absolute;
            bottom: 16px;
            left: 16px;
            right: 16px;
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            pointer-events: none;
        }

        .hud-chip {
            background: rgba(15, 23, 42, 0.75);
            backdrop-filter: blur(8px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 8px 14px;
            border-radius: 8px;
            font-size: 12px;
            display: flex;
            gap: 12px;
            pointer-events: auto;
        }

        .hud-item {
            display: flex;
            flex-direction: column;
        }

        .hud-item span.label {
            font-size: 10px;
            color: var(--text-secondary);
            text-transform: uppercase;
        }

        .hud-item span.value {
            font-weight: 700;
            color: var(--accent-cyan);
            font-size: 13px;
        }

        .control-panel {
            background-color: var(--surface-color);
            border: 1px solid var(--border-color);
            border-radius: 14px;
            padding: 16px;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            align-items: center;
        }

        .btn {
            background-color: var(--surface-elevated);
            color: var(--text-primary);
            border: 1px solid var(--border-color);
            padding: 10px 18px;
            border-radius: 10px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.2s ease;
        }

        .btn:hover {
            background-color: #293548;
            border-color: var(--accent-cyan);
        }

        .btn.active {
            background: linear-gradient(135deg, var(--accent-blue), var(--accent-cyan));
            color: #000;
            border: none;
        }

        .btn-danger {
            background-color: rgba(239, 68, 68, 0.2);
            color: var(--red);
            border-color: rgba(239, 68, 68, 0.4);
        }

        .btn-danger:hover {
            background-color: var(--red);
            color: #fff;
        }

        .sidebar {
            flex: 1;
            min-width: 280px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .card {
            background-color: var(--surface-color);
            border: 1px solid var(--border-color);
            border-radius: 14px;
            padding: 18px;
        }

        .card-header {
            font-size: 14px;
            font-weight: 700;
            color: var(--accent-cyan);
            margin-bottom: 12px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }

        .stat-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .stat-box {
            background-color: var(--surface-elevated);
            padding: 10px 12px;
            border-radius: 8px;
            border: 1px solid rgba(255,255,255,0.05);
        }

        .stat-box .title {
            font-size: 11px;
            color: var(--text-secondary);
        }

        .stat-box .num {
            font-size: 16px;
            font-weight: 700;
            margin-top: 4px;
            color: var(--text-primary);
        }

        .motion-alert {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px;
            border-radius: 8px;
            background: rgba(245, 158, 11, 0.1);
            border: 1px solid rgba(245, 158, 11, 0.3);
            color: var(--amber);
            font-size: 12px;
            margin-top: 10px;
        }

        .event-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
            max-height: 240px;
            overflow-y: auto;
        }

        .event-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 8px 10px;
            border-radius: 8px;
            background: var(--surface-elevated);
            font-size: 12px;
        }
    </style>
</head>
<body>
    <header>
        <div class="brand">
            <div class="brand-badge">TG</div>
            <div class="brand-text">
                <h1>$cameraName</h1>
                <p>Offline Local CCTV • Direct LAN</p>
            </div>
        </div>
        <div class="status-pill">
            <div class="status-dot"></div>
            <span id="connState">LIVE STREAMING</span>
        </div>
    </header>

    <div class="main-container">
        <!-- Live Video Area -->
        <div class="video-section">
            <div class="video-container" id="videoWrapper">
                <img id="liveFeed" src="/stream?token=$token" alt="TwinGuard Live Feed" onerror="handleStreamError()" />
                <div class="rec-indicator" id="recBadge">● REC <span id="recTimer">00:00</span></div>

                <div class="hud-overlay">
                    <div class="hud-chip">
                        <div class="hud-item">
                            <span class="label">Latency</span>
                            <span class="value" id="hudLatency">~120 ms</span>
                        </div>
                        <div class="hud-item">
                            <span class="label">FPS</span>
                            <span class="value" id="hudFps">30.0</span>
                        </div>
                        <div class="hud-item">
                            <span class="label">Bitrate</span>
                            <span class="value" id="hudBitrate">2.4 Mbps</span>
                        </div>
                    </div>
                    <div class="hud-chip">
                        <div class="hud-item">
                            <span class="label">Quality</span>
                            <span class="value" id="hudQuality">720p HD</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- CCTV Quick Controls -->
            <div class="control-panel">
                <button class="btn" id="btnTorch" onclick="sendCommand('TOGGLE_TORCH')">
                    🔦 Flashlight
                </button>
                <button class="btn" id="btnFlip" onclick="sendCommand('SWITCH_CAMERA')">
                    🔄 Flip Camera
                </button>
                <button class="btn" id="btnZoomIn" onclick="sendCommand('ZOOM_IN')">
                    🔍 Zoom +
                </button>
                <button class="btn" id="btnZoomOut" onclick="sendCommand('ZOOM_OUT')">
                    🔍 Zoom -
                </button>
                <button class="btn" id="btnSnapshot" onclick="captureSnapshot()">
                    📸 Snapshot
                </button>
                <button class="btn btn-danger" id="btnRec" onclick="toggleRecording()">
                    🔴 Instant Record
                </button>
                <button class="btn" id="btnSiren" onclick="sendCommand('TRIGGER_SIREN')">
                    🚨 Siren Alarm
                </button>
                <button class="btn" onclick="toggleFullScreen()">
                    ⛶ Fullscreen
                </button>
            </div>
        </div>

        <!-- Sidebar Diagnostics & Camera Info -->
        <div class="sidebar">
            <div class="card">
                <div class="card-header">
                    <span>Camera Telemetry</span>
                    <span style="font-size: 11px; color: var(--green);">ONLINE</span>
                </div>
                <div class="stat-grid">
                    <div class="stat-box">
                        <div class="title">Battery</div>
                        <div class="num" id="statBattery">-- %</div>
                    </div>
                    <div class="stat-box">
                        <div class="title">Temperature</div>
                        <div class="num" id="statTemp">-- °C</div>
                    </div>
                    <div class="stat-box">
                        <div class="title">Storage Free</div>
                        <div class="num" id="statStorage">-- GB</div>
                    </div>
                    <div class="stat-box">
                        <div class="title">Viewers</div>
                        <div class="num" id="statViewers">1</div>
                    </div>
                </div>

                <div id="motionAlertBox" class="motion-alert" style="display: none;">
                    ⚠️ <strong>MOTION DETECTED</strong> in monitoring area
                </div>
            </div>

            <div class="card">
                <div class="card-header">Stream Profile</div>
                <div style="display: flex; gap: 8px;">
                    <button class="btn" style="flex:1; padding:8px;" onclick="sendCommand('SET_QUALITY_LOW')">480p</button>
                    <button class="btn active" style="flex:1; padding:8px;" onclick="sendCommand('SET_QUALITY_BALANCED')">720p</button>
                    <button class="btn" style="flex:1; padding:8px;" onclick="sendCommand('SET_QUALITY_HIGH')">1080p</button>
                </div>
            </div>

            <div class="card">
                <div class="card-header">Local Network Info</div>
                <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.6;">
                    <div><strong>Camera IP:</strong> $serverIp:$serverPort</div>
                    <div><strong>Protocol:</strong> HTTP/1.1 Low-Latency Stream</div>
                    <div><strong>Encryption:</strong> Local-Only WPA2/P2P</div>
                    <div><strong>Internet:</strong> Not required (100% Offline)</div>
                </div>
            </div>
        </div>
    </div>

    <script>
        const TOKEN = "$token";
        let isRecording = false;

        function sendCommand(action) {
            fetch('/api/command?token=' + TOKEN, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: action })
            }).then(r => r.json()).then(data => {
                if (data.status === 'ok') {
                    updateTelemetry();
                }
            }).catch(e => console.error("Command error:", e));
        }

        function toggleRecording() {
            if (isRecording) {
                sendCommand('STOP_RECORDING');
            } else {
                sendCommand('START_RECORDING');
            }
        }

        function captureSnapshot() {
            window.open('/snapshot?token=' + TOKEN, '_blank');
        }

        function toggleFullScreen() {
            const elem = document.getElementById("videoWrapper");
            if (!document.fullscreenElement) {
                elem.requestFullscreen().catch(err => alert("Error enabling fullscreen"));
            } else {
                document.exitFullscreen();
            }
        }

        function handleStreamError() {
            document.getElementById("connState").innerText = "RECONNECTING...";
            document.getElementById("connState").style.color = "var(--red)";
            setTimeout(() => {
                document.getElementById("liveFeed").src = "/stream?token=" + TOKEN + "&t=" + Date.now();
            }, 2000);
        }

        function updateTelemetry() {
            const startPing = Date.now();
            fetch('/telemetry?token=' + TOKEN)
                .then(r => r.json())
                .then(data => {
                    const rtt = Date.now() - startPing;
                    document.getElementById("connState").innerText = "LIVE STREAMING";
                    document.getElementById("hudLatency").innerText = rtt + " ms";
                    document.getElementById("hudFps").innerText = data.currentFps.toFixed(1);
                    document.getElementById("hudBitrate").innerText = (data.bitrateKbps / 1000).toFixed(1) + " Mbps";
                    document.getElementById("hudQuality").innerText = data.quality;
                    document.getElementById("statBattery").innerText = data.batteryPercent + "%" + (data.isCharging ? " ⚡" : "");
                    document.getElementById("statTemp").innerText = data.temperatureCelsius + "°C";
                    document.getElementById("statStorage").innerText = (data.freeStorageMb / 1024).toFixed(1) + " GB";
                    document.getElementById("statViewers").innerText = data.connectedClients;

                    // Recording indicator
                    isRecording = data.isRecording;
                    const recBadge = document.getElementById("recBadge");
                    const btnRec = document.getElementById("btnRec");
                    if (data.isRecording) {
                        recBadge.style.display = "flex";
                        const mins = Math.floor(data.recordingDurationSec / 60).toString().padStart(2, '0');
                        const secs = (data.recordingDurationSec % 60).toString().padStart(2, '0');
                        document.getElementById("recTimer").innerText = mins + ":" + secs;
                        btnRec.innerText = "⏹ Stop Record";
                    } else {
                        recBadge.style.display = "none";
                        btnRec.innerText = "🔴 Instant Record";
                    }

                    // Motion alert
                    const motionBox = document.getElementById("motionAlertBox");
                    if (data.isMotionDetected) {
                        motionBox.style.display = "flex";
                    } else {
                        motionBox.style.display = "none";
                    }
                })
                .catch(err => {
                    document.getElementById("connState").innerText = "DISCONNECTED";
                });
        }

        // Poll telemetry every 1.5 seconds
        setInterval(updateTelemetry, 1500);
        updateTelemetry();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
