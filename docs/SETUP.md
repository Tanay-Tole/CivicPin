# CIVIC PIN — Setup & Deployment Guide

## Prerequisites
- Android Studio Ladybug or newer.
- Android SDK API 35 (Minimum SDK 26).
- JDK 17.

## Setup Steps

### 1. Maps
The app uses OpenStreetMap through the osmdroid library. No API key, Google Cloud project or billing account is needed. The map needs an internet connection to load tiles.

### 2. Assets Verification
Verify the following assets exist in `app/src/main/assets/`:
- `civicpin_model.tflite`
- `labels.txt`
- `email civic project.txt`

### 3. Build & Run
Run the following Gradle commands:
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew app:assembleDebug
```

The compiled APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Installation on Physical Android Phone
Enable **Developer Options** and **USB Debugging** on your phone, then run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
