# DuolingoApp

A lightweight, privacy-focused Duolingo client for Android built with Jetpack Compose and AndroidX WebKit. 

This app wraps Duolingo's web app into a native experience tailored for Duolingo Super subscribers who want to avoid the standalone mobile app's telemetry, device scanning, and background trackers.

## Features

- **Anti-Nag Script & CSS Injection**:
  - Injects `removeNativeAppBanner = true` at document start before web scripts execute.
  - Injected CSS completely hides top mobile download banners and promo elements.
  - Active `MutationObserver` instantly dismisses mobile app install popups, softwalls, and hardwalls without freezing page scroll.
  - Blocks store redirect schemes (`market://`, `intent://`, and `play.google.com`).
- **Privacy & Anti-Telemetry**:
  - Zero access to phone contacts, storage, SMS, or telephony.
  - Automatic interception and blocking of tracker requests (`excess.duolingo.com/batch`, Adjust, Facebook, Google Analytics).
- **Duolingo Super Readiness**:
  - Full Web Audio and WebRTC audio capture (`RECORD_AUDIO`) support for speaking and listening exercises.
  - Unrestricted audio autoplay for instant pronunciation playback.
  - Persistent login session via cookie and DOM storage management.
- **Native Android Experience**:
  - Modern Jetpack Compose UI with edge-to-edge support.
  - Android back gesture navigation history support.
  - In-app Quick Actions bar with **Reload** and **Settings** dialog.
  - Custom Duolingo Owl launcher icon.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design System**: [Material 3](https://m3.material.io/)
- **Web Engine**: [AndroidX WebKit](https://developer.android.com/reference/androidx/webkit/package-summary)
- **Preferences**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

## Getting Started

### Prerequisites

- Android Studio Jellyfish or newer.
- Android SDK 24 (Android 7.0) or higher.

### Building Locally

```bash
# Clone the repository
git clone https://github.com/eetufin92/DuolingoApp.git

# Build Debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## GitHub Actions & Releases

This repository includes automated CI/CD (`.github/workflows/android.yml`):
- Automatically builds the release APK on every push to `main` and Pull Requests.
- Automatically creates a GitHub Release tagged `v<run_number>` with the release APK attached when pushed to `main`.
- Optional APK signing via GitHub Secrets (`KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

## License

This project is licensed under the MIT License.
