# TwinGuard CCTV — Smart Offline Security Camera (v2.0)

[![Build and Release](https://github.com/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/)
[![Android API](https://img.shields.io/badge/API-24%20to%2036%2B-blue.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-green.svg)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Network-100%25%20Offline-orange.svg)]()

**TwinGuard CCTV** transforms Android smartphones into an offline wireless surveillance system, featuring camera broadcasting, phone-to-phone live monitoring, and laptop browser viewing without requiring internet access, mobile data, cloud infrastructure, or external servers.

---

## 🌟 Key Features

### 1. Camera Mode (Autonomous CCTV)
- **CameraX Engine**: Real-time camera feed with front/rear lens switching, autofocus, digital zoom (1.0x to 5.0x), and flashlight/torch control.
- **Embedded Local HTTP Server**: Multi-client asynchronous server running on port `8080`.
- **Bounded Frame Queue**: Zero-lag low latency (~120–180ms) by discarding stale frames.
- **Offline Smart Motion Detection**: Multi-zone algorithmic frame-differential detection (Full Frame, Center, Entryway) with automatic snapshot capture.
- **Persistent Android 14/15 Service**: Foreground service with `camera`, `microphone`, and `connectedDevice` types and persistent notification controls.

### 2. Monitor Mode (Remote CCTV Monitor)
- **Live Video Streaming**: Real-time video player with automatic reconnection handling.
- **Diagnostics HUD**: On-screen overlay displaying measured latency (ms), FPS, bitrate (kbps), battery percentage, and temperature.
- **Remote Controls**: Remotely toggle torch, flip cameras, trigger snapshots, start/stop MP4 recording, and sound the emergency siren.
- **Two-Way Audio**: Push-to-talk walkie-talkie with hardware Acoustic Echo Cancellation (AEC).

### 3. Laptop Viewer Mode
- **Zero Software Install**: Direct browser monitoring in Chrome, Edge, Safari, or Firefox via `http://<camera-phone-ip>:8080`.
- **Responsive Cyber Dashboard**: Dark-mode CCTV dashboard with live MJPEG feed, telemetry meters, recording controls, snapshot downloads, and motion alerts.

### 4. Dynamic QR Code Auto-Pairing
- Dynamic QR code generation containing device ID, LAN IPv4, port, authentication nonce, and a 30-minute expiration window.
- Integrated camera QR scanner for instant one-tap pairing.
- Manual LAN IP and Wi-Fi Direct (P2P) fallbacks.

---

## 📱 How to Release the APK on GitHub

### Option A: Using AI Studio Direct Export (Easiest)
1. In the **AI Studio header**, click the **Export** menu or **Settings** button.
2. Select **"Push to GitHub"** to export the entire repository to your GitHub account.
3. Select **"Download APK"** or **"Generate APK"** to download the pre-built `app-debug.apk`.
4. Go to your GitHub repository:
   - Navigate to **Releases** > **Draft a new release**.
   - Set a tag (e.g. `v2.0.0`).
   - Drag and drop `app-debug.apk` into the release binaries section.
   - Click **Publish release**.

### Option B: Automated GitHub Actions CI/CD
This repository includes `.github/workflows/build-and-release.yml`:
1. Push your code to GitHub.
2. Push a release tag:
   ```bash
   git tag v2.0.0
   git push origin v2.0.0
   ```
3. GitHub Actions will automatically compile the APK and publish it as a downloadable release asset under your repository's Releases tab!

---

## 🛠 Local Build Instructions

```bash
# Clone the repository
git clone https://github.com/<your-username>/twinguard-cctv.git
cd twinguard-cctv

# Build the APK via Gradle
gradle assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
