# StreamPro - Hybrid IPTV Player


                                                                           

StreamPro is a high-performance, modern IPTV player designed for Android Mobile and Android TV. It utilizes advanced playback technology to ensure maximum compatibility and stability for various stream protocols.

## 🚀 Key Features

*   **Advanced Player Engine**: 
    *   **ExoPlayer (Media3)**: Optimized for HLS (.m3u8), DASH (.mpd), and DRM-protected content (Widevine/ClearKey).
*   **Universal Compatibility**: Support for M3U and M3U8 playlists via Remote URL or Local File import.
*   **EPG Support**: Full XMLTV integration with a beautiful "Panduan TV" interface to track your favorite shows.
*   **Modern UI/UX**: Built entirely with **Jetpack Compose**, featuring a sleek "Universe" dark theme and fluid animations.
*   **Android TV Optimized**: Fully compatible with Leanback navigation and TV remote controls.
*   **Advanced Media Controls**:
    *   Audio Track & Subtitle (CC) selection.
    *   Audio Boost feature for quiet streams.
    *   Manual and Automatic Quality selection.
    *   Zapping Mode for fast channel switching.
*   **Content Management**: Favorites system, watch history, and channel grouping.
*   **Performance Tools**: Hardware acceleration toggle, buffer size customization, and real-time debug information.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Playback**: Media3 (ExoPlayer)
*   **Database**: Room (Offline caching for channels and EPG)
*   **Networking**: OkHttp, Retrofit
*   **Serialization**: Moshi
*   **Image Loading**: Coil

## 📦 Project Structure

```
├── app               # Mobile Application Module
├── stream_pro_tv     # Android TV Application Module
├── core              # Shared Data, Models, and Player Logic
└── gradle            # Version Catalogs (libs.versions.toml)
```

## 🛠 Setup & Installation

1.  Clone the repository.
2.  Open in **Android Studio Ladybug** or newer.
3.  Ensure you have the **NDK** installed (v27+ recommended).
4.  Sync Gradle and run the `:app` or `:stream_pro_tv` module.

## 📄 License

Designed and Developed by **Chesko Team**.
© 2026 StreamPro IPTV. All rights reserved.

---
*Disclaimer: StreamPro does not provide any media content. Users must provide their own playlists.*
