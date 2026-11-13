# Pocket Media Server for Android

A lightweight HTTP and DLNA / UPnP Media Server Android application designed to run seamlessly in the background and stream your phone's videos, music, and photos directly to your Smart TV, PC, VLC, Kodi, or web browser.

## Features

- **DLNA / UPnP Media Server**:
  - Auto-discovery via SSDP (Simple Service Discovery Protocol) on multicast `239.255.255.250:1900`.
  - UPnP A/V ContentDirectory:1 service with hierarchical folder browsing and media metadata (DIDL-Lite).
  - Compatible with Samsung Tizen, LG webOS, Sony Android TV / Google TV, TCL Roku, Apple TV, VLC, Kodi, and Windows Media Player.
  - Implements DLNA seek headers (`DLNA.ORG_OP=01`) enabling byte range seeking & video scrubbing on TVs.
- **HTTP Streaming & Web Interface**:
  - Serves direct HTTP streaming with `HTTP/1.1 206 Partial Content` Range requests.
  - Modern, responsive HTML5 web file browser accessible from any browser at `http://<phone-ip>:8080/`.
  - Built-in video and audio playback directly in any web browser.
- **Persistent Background Operation**:
  - Android Foreground Service with an ongoing notification.
  - PowerManager `PARTIAL_WAKE_LOCK` and WifiManager `WIFI_MODE_FULL_HIGH_PERF` prevent Android from putting the CPU or Wi-Fi to sleep during TV streaming.
  - Notification action allowing one-tap server shutdown.
- **Start on Boot Option**:
  - Auto-starts server immediately when phone reboots (`RECEIVE_BOOT_COMPLETED`), with easy toggle in settings.
- **Customizable Configuration**:
  - Configurable HTTP port (default `8080`).
  - Customizable friendly name displayed on TV devices.
  - Selectable shared media folder (defaults to entire internal storage `/storage/emulated/0`, with quick presets for Movies, Music, Download, DCIM, or custom folders).
- **Live Activity Logs**:
  - Real-time terminal log viewer in app displaying connected TVs, browse actions, and active video streaming requests.

## APK Location

The debug APK has been built and is located at:
- [app-debug.apk](file:///workspace/media-server/app/build/outputs/apk/debug/app-debug.apk)

## How to Access from Your Smart TV

1. Connect your phone and TV to the same Wi-Fi network.
2. Open **Pocket Media Server** on your phone and tap **START SERVER**.
3. On your Smart TV:
   - **Samsung**: Open **Sources** or **Media Player** and select "Pocket Media Server".
   - **LG**: Open **Home Dashboard** or **Media Player** -> "Pocket Media Server".
   - **Sony / Android TV / Fire TV**: Open **VLC**, **Kodi**, or default **Media Player** app -> **Local Network / UPnP**.
   - **PC / Web Browser**: Navigate to `http://<phone-ip>:8080/`.
