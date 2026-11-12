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
            --md-bg: #0e141b;
            --md-surface: #121922;
            --md-surface-variant: #1d2633;
            --md-primary: #7ed0ff;
            --md-on-primary: #00344f;
            --md-primary-container: #004c70;
            --md-secondary: #62dcb6;
            --md-tertiary: #ffb77c;
            --md-error: #ffb4ab;
            --md-error-container: #93000a;
            --md-outline: #323f52;
            --md-text: #dee3eb;
            --md-text-muted: #8b99ac;
            --green: #10b981;
            --red: #ef4444;
            --amber: #f59e0b;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: system-ui, -apple-system, Roboto, "Segoe UI", sans-serif;
        }

        body {
            background-color: var(--md-bg);
            color: var(--md-text);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        header {
            background-color: var(--md-surface);
            border-bottom: 1px solid var(--md-outline);
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
            background: linear-gradient(135deg, #004c70, #7ed0ff);
            width: 42px;
            height: 42px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            color: #000;
            font-size: 16px;
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 16px;
            border-radius: 24px;
            background: rgba(98, 220, 182, 0.15);
            border: 1px solid var(--md-secondary);
            color: var(--md-secondary);
            font-size: 13px;
            font-weight: 600;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: var(--md-secondary);
            box-shadow: 0 0 8px var(--md-secondary);
            animation: pulse 1.5s infinite;
        }

        @keyframes pulse {
            0% { opacity: 0.6; }
            50% { opacity: 1; transform: scale(1.15); }
            100% { opacity: 0.6; }
        }

        .alert-banner {
            display: none;
            background: linear-gradient(90deg, #93000a, #ba1a1a);
            color: #fff;
            padding: 12px 24px;
            align-items: center;
            justify-content: space-between;
            font-size: 14px;
            font-weight: 700;
            animation: flashAlert 1s infinite alternate;
        }

        @keyframes flashAlert {
            from { background-color: #93000a; }
            to { background-color: #ba1a1a; }
        }

        .main-container {
            display: flex;
            flex: 1;
            padding: 20px;
            gap: 20px;
        }

        @media (max-width: 960px) {
            .main-container { flex-direction: column; }
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
            border-radius: 20px;
            overflow: hidden;
            border: 1px solid var(--md-outline);
            aspect-ratio: 16 / 9;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 12px 36px rgba(0,0,0,0.7);
        }

        #liveFeed {
            width: 100%;
            height: 100%;
            object-fit: contain;
            background-color: #000;
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
            background: rgba(18, 25, 34, 0.85);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 8px 16px;
            border-radius: 14px;
            font-size: 12px;
            display: flex;
            gap: 14px;
            pointer-events: auto;
        }

        .hud-item { display: flex; flex-direction: column; }
        .hud-item .label { font-size: 10px; color: var(--md-text-muted); text-transform: uppercase; }
        .hud-item .value { font-weight: 700; color: var(--md-primary); font-size: 13px; }

        .control-panel {
            background-color: var(--md-surface);
            border: 1px solid var(--md-outline);
            border-radius: 20px;
            padding: 18px;
            display: flex;
            flex-direction: column;
            gap: 14px;
        }

        .btn-row {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            align-items: center;
        }

        .btn {
            background-color: var(--md-surface-variant);
            color: var(--md-text);
            border: 1px solid var(--md-outline);
            padding: 10px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.2s ease;
        }

        .btn:hover {
            background-color: #263345;
            border-color: var(--md-primary);
        }

        .btn-primary {
            background-color: var(--md-primary-container);
            color: var(--md-primary);
            border-color: var(--md-primary);
        }

        .btn-danger {
            background-color: var(--md-error-container);
            color: var(--md-error);
            border-color: var(--red);
        }

        .btn-danger:hover {
            background-color: var(--red);
            color: #fff;
        }

        .sidebar {
            flex: 1;
            min-width: 320px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .card {
            background-color: var(--md-surface);
            border: 1px solid var(--md-outline);
            border-radius: 18px;
            padding: 18px;
        }

        .card-title {
            font-size: 13px;
            font-weight: 700;
            color: var(--md-primary);
            margin-bottom: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .stat-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .stat-box {
            background-color: var(--md-surface-variant);
            padding: 10px 14px;
            border-radius: 12px;
        }

        .stat-box .title { font-size: 11px; color: var(--md-text-muted); }
        .stat-box .num { font-size: 16px; font-weight: 700; margin-top: 4px; color: var(--md-text); }

        .siren-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
        }

        .siren-btn {
            background: var(--md-surface-variant);
            border: 1px solid var(--md-outline);
            color: var(--md-text);
            padding: 8px 10px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            text-align: left;
        }

        .siren-btn:hover {
            background: #2b384c;
            border-color: var(--red);
            color: var(--red);
        }
    </style>
</head>
<body>
    <div id="humanAlertBar" class="alert-banner">
        <span>⚠️ <strong>HUMAN INTRUDER MOTION DETECTED</strong> — 10s Clip Recording & Snapshot Saved</span>
        <button class="btn btn-danger" style="padding: 4px 12px;" onclick="sendCommand('TRIGGER_SIREN_1')">Sound Siren</button>
    </div>

    <header>
        <div class="brand">
            <div class="brand-badge">TG</div>
            <div>
                <h1 style="font-size: 18px;">$cameraName</h1>
                <p style="font-size: 12px; color: var(--md-text-muted);">Material You CCTV Hub • Offline LAN</p>
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
                        <div class="hud-item">
                            <span class="label">Zoom</span>
                            <span class="value" id="hudZoom">1.0x</span>
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

            <!-- Enhanced Controls Panel -->
            <div class="control-panel">
                <div class="btn-row">
                    <button class="btn" onclick="sendCommand('TOGGLE_TORCH')">🔦 Flashlight</button>
                    <button class="btn" onclick="sendCommand('TOGGLE_STROBE')">⚡ Strobe Deterrent</button>
                    <button class="btn" onclick="sendCommand('SWITCH_CAMERA')">🔄 Flip Lens</button>
                    <button class="btn" onclick="sendCommand('CAPTURE_SNAPSHOT')">📸 Snapshot</button>
                    <button class="btn btn-primary" onclick="sendCommand('RECORD_10S_CLIP')">⏱ Record 10s Clip</button>
                    <button class="btn btn-danger" id="btnRec" onclick="toggleRecording()">🔴 Continuous Record</button>
                    <button class="btn" onclick="sendCommand('TOGGLE_PRIVACY_MODE')">🔒 Privacy Mode</button>
                    <button class="btn" onclick="toggleFullScreen()">⛶ Fullscreen</button>
                </div>

                <!-- Zoom Presets -->
                <div class="btn-row" style="align-items: center;">
                    <span style="font-size: 12px; color: var(--md-text-muted); font-weight: 700;">PTZ ZOOM:</span>
                    <button class="btn" style="padding: 6px 12px;" onclick="sendCommand('SET_ZOOM_1X')">1x</button>
                    <button class="btn" style="padding: 6px 12px;" onclick="sendCommand('SET_ZOOM_2X')">2x</button>
                    <button class="btn" style="padding: 6px 12px;" onclick="sendCommand('SET_ZOOM_3X')">3x</button>
                    <button class="btn" style="padding: 6px 12px;" onclick="sendCommand('SET_ZOOM_5X')">5x</button>
                    <button class="btn" style="padding: 6px 12px;" onclick="sendCommand('ZOOM_IN')">Zoom +</button>
                    <button class="btn" style="padding: 6px 12px;" onclick="sendCommand('ZOOM_OUT')">Zoom -</button>
                </div>
            </div>
        </div>

        <!-- Sidebar -->
        <div class="sidebar">
            <!-- 8 Siren Types Deterrent Panel -->
            <div class="card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                    <span class="card-title" style="margin-bottom:0;">🚨 8 Siren Types</span>
                    <button class="btn btn-danger" style="padding: 4px 10px; font-size: 11px;" onclick="sendCommand('STOP_SIREN')">Silence Alarm</button>
                </div>
                <div class="siren-grid">
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_1')">1. Police Wail</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_2')">2. Rapid Yelp</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_3')">3. Hi-Lo Two-Tone</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_4')">4. Piercing Intruder</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_5')">5. Air Raid Rumble</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_6')">6. Cyber Alarm</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_7')">7. Guard Pulse</button>
                    <button class="siren-btn" onclick="sendCommand('TRIGGER_SIREN_8')">8. Ultrasonic Chirp</button>
                </div>
            </div>

            <!-- Camera Telemetry -->
            <div class="card">
                <div class="card-title">Camera Hardware Telemetry</div>
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
                        <div class="title">Free Storage</div>
                        <div class="num" id="statStorage">-- GB</div>
                    </div>
                    <div class="stat-box">
                        <div class="title">Viewers</div>
                        <div class="num" id="statViewers">1</div>
                    </div>
                </div>
            </div>

            <!-- Stream Profile -->
            <div class="card">
                <div class="card-title">Stream Resolution Profile</div>
                <div style="display: flex; gap: 8px;">
                    <button class="btn" style="flex:1; padding:8px;" onclick="sendCommand('SET_QUALITY_LOW')">480p SD</button>
                    <button class="btn btn-primary" style="flex:1; padding:8px;" onclick="sendCommand('SET_QUALITY_BALANCED')">720p HD</button>
                    <button class="btn" style="flex:1; padding:8px;" onclick="sendCommand('SET_QUALITY_HIGH')">1080p FHD</button>
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
                if (data.status === 'ok') updateTelemetry();
            }).catch(e => console.error("Command error:", e));
        }

        function toggleRecording() {
            if (isRecording) {
                sendCommand('STOP_RECORDING');
            } else {
                sendCommand('START_RECORDING');
            }
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
                    document.getElementById("hudZoom").innerText = data.zoomLevel ? data.zoomLevel.toFixed(1) + "x" : "1.0x";
                    document.getElementById("statBattery").innerText = data.batteryPercent + "%" + (data.isCharging ? " ⚡" : "");
                    document.getElementById("statTemp").innerText = data.temperatureCelsius + "°C";
                    document.getElementById("statStorage").innerText = (data.freeStorageMb / 1024).toFixed(1) + " GB";
                    document.getElementById("statViewers").innerText = data.connectedClients;

                    // Recording indicator
                    isRecording = data.isRecording;
                    const btnRec = document.getElementById("btnRec");
                    if (data.isRecording) {
                        btnRec.innerText = "⏹ Stop Record (" + data.recordingDurationSec + "s)";
                    } else {
                        btnRec.innerText = "🔴 Continuous Record";
                    }

                    // Human Motion Alert Banner
                    const alertBar = document.getElementById("humanAlertBar");
                    if (data.isHumanMotion || data.isMotionDetected) {
                        alertBar.style.display = "flex";
                    } else {
                        alertBar.style.display = "none";
                    }
                })
                .catch(err => {
                    document.getElementById("connState").innerText = "DISCONNECTED";
                });
        }

        setInterval(updateTelemetry, 1400);
        updateTelemetry();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
