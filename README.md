# 🌌 StreamPro - Hybrid IPTV Player

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android_Mobile_%7C_Android_TV-3DDC84.svg?style=flat&logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg?style=flat)](LICENSE)

**StreamPro** is a professional-grade IPTV player built with a modern tech stack. Designed for speed, stability, and a seamless user experience across both handheld devices and the big screen (Android TV).

---

## ✨ Features

### 📺 Superior Playback
*   **Powered by Media3 (ExoPlayer)**: Optimized for HLS, DASH, and SmoothStreaming.
*   **Wide Format Support**: Handles `.m3u8`, `.mpd`, `.ts`, `.mp4`, and more.
*   **Adaptive Bitrate**: Automatic quality switching based on internet speed.
*   **Zapping Mode**: Lightning-fast channel switching (Channel Up/Down).

### 🎨 Modern Experience
*   **Glassmorphism UI**: Beautiful dark theme with translucent elements and fluid Compose animations.
*   **Dual-Interface**: Native layouts for **Mobile** (Touch) and **Android TV** (D-Pad/Remote).
*   **Multi-Playlist**: Import multiple M3U sources via URL or local storage.
*   **EPG Engine**: Full XMLTV support with automatic caching and program reminders.

### 🛠 Advanced Tools
*   **Audio Boost**: Increase volume up to 200% for quiet streams.
*   **Subtitle Support**: External and embedded SRT/VTT support.
*   **Device ID Binding**: Keep your settings safe with local device identification.
*   **Cast Support**: Stream content from your phone to Chromecast-enabled devices.

---

## 🏗 Architecture & Tech Stack

The project follows a **Multi-Module Clean Architecture** pattern:

*   **`:app`**: Mobile-specific UI and features.
*   **`:stream_pro_tv`**: Optimized TV interface (Leanback-style navigation).
*   **`:core`**: Shared business logic, database (Room), networking (Retrofit), and the player engine.

| Dependency | Purpose |
| :--- | :--- |
| **Jetpack Compose** | Declarative UI for both Mobile & TV |
| **Media3 / ExoPlayer** | The core playback engine |
| **Room** | Local storage for channels, EPG, and history |
| **Hilt (Dagger)** | Dependency injection (to be fully implemented) |
| **Retrofit/OkHttp** | Efficient network handling |
| **Coil** | Image loading for channel logos |

---

## 🚀 Getting Started

### Prerequisites
*   Android Studio Ladybug (2024.2.1) or newer.
*   JDK 17+.
*   Android SDK Level 26 (Android 8.0) minimum.

### Installation
1.  **Clone the repo**:
    ```bash
    git clone https://github.com/username/IPTV_Player.git
    ```
2.  **Open in Android Studio**: Wait for Gradle sync to complete.
3.  **Run**: Select `app` for Mobile or `stream_pro_tv` for Android TV and click **Run**.

---

## 🔒 Privacy & Safety
StreamPro values user privacy. We do not collect personal data on our servers. All data (playlists, history) remains on your local device. 
See our full [Privacy Policy](PRIVACY_POLICY.md) for more details.

---

## ⚠️ Disclaimer
**StreamPro does not provide any media content.** Users must provide their own content (playlists). We do not endorse the streaming of copyright-protected material without permission from the copyright holder.

---
Developed with by **Chesko Team**
[Official Website](https://chesko-25.vercel.app) | [Contact Developer](mailto:your-email@example.com)
