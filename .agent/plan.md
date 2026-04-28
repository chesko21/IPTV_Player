# Project Plan

IPTV_Player: An Android application that allows users to watch IPTV channels from M3U playlists. 
The app should support:
- Parsing M3U playlists (including metadata like tvg-logo, group-title, tvg-country).
- Playing various stream formats like HLS (.m3u8) and DASH (.mpd).
- Support for DRM (Widevine, ClearKey) as seen in the sample data.
- UI to list categories (groups) and channels.
- Full-screen video player with playback controls.
- Adaptive app icon and Material Design 3.
- Edge-to-edge display.

Sample M3U link provided: https://raw.githubusercontent.com/chesko21/tv-online-m3u/refs/heads/my-repo/playlist3%20(SFILE.MOBI).m3u
Sample data provided in the prompt shows usage of #EXTINF, #EXTVLCOPT, #KODIPROP (for license keys).

## Project Brief

# IPTV Player

A modern, high-performance Android application designed to stream IPTV content using M3U playlists. The app focuses on a seamless user experience, robust playback capabilities, and a vibrant Material Design 3 interface.

## Features

- **Advanced M3U Parsing**: Automatically parses M3U playlists to extract channel metadata, including logos (`tvg-logo`), categories (`group-title`), and country information.
- **Universal Stream Support**: High-performance playback for various streaming formats, specifically optimized for HLS (`.m3u8`) and DASH (`.mpd`).
- **DRM Content Decryption**: Built-in support for Widevine and ClearKey DRM, ensuring compatibility with protected premium content.
- **Categorized Channel UI**: An intuitive interface that organizes channels into groups for easy navigation and discovery.
- **Immersive Full-Screen Player**: A feature-rich video player with custom playback controls and full edge-to-edge display support.

## High-Level Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Media Engine**: Android Media3 / ExoPlayer (HLS, DASH, and DRM extensions)
- **Concurrency**: Kotlin Coroutines & Flow
- **Networking**: OkHttp & Retrofit (for playlist fetching)
- **Image Loading**: Coil (for channel logos)
- **Code Generation**: KSP (Kotlin Symbol Processing)

## Implementation Steps

### Task_1_DataLayer: Setup project dependencies for Media3 and implement the M3U parser and networking layer to fetch and process the playlist.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Media3 (exoplayer, dash, hls, ui) dependencies added to libs.versions.toml and build.gradle.kts
  - M3U parser correctly handles #EXTINF (logo, group) and DRM properties (#KODIPROP)
  - Retrofit/OkHttp fetches the sample playlist link successfully
  - Playlist data is mapped to domain models (Channel, Group)
- **StartTime:** 2026-04-17 02:06:25 ICT

### Task_2_BrowsingUI: Implement the channel browsing interface using Jetpack Compose and Material 3.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Home screen displays categories (groups)
  - Channel list displays names and logos using Coil
  - Navigation between category list and channel list implemented
  - UI follows Material Design 3 guidelines and supports Edge-to-Edge

### Task_3_VideoPlayer: Integrate Media3 ExoPlayer for stream playback and build the immersive full-screen player UI.
- **Status:** PENDING
- **Acceptance Criteria:**
  - ExoPlayer configured for HLS, DASH, and DRM (Widevine/ClearKey)
  - Full-screen player screen with custom playback controls
  - Edge-to-edge display and orientation handling for playback
  - Successful playback of sample streams from the M3U playlist

### Task_4_FinalRefinement: Refine the app's theme, create an adaptive icon, and perform final verification.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Vibrant Material 3 color scheme applied for Light and Dark modes
  - Adaptive app icon matching the IPTV theme created
  - Project builds successfully, 'app does not crash' and 'make sure all existing tests pass'
  - Critic agent verifies application stability and alignment with requirements

