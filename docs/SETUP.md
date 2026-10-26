# CIVIC PIN — Setup & Deployment Guide

## Prerequisites
- Android Studio Ladybug or newer.
- Android SDK API 35 (Minimum SDK 26).
- JDK 17.
- Google Maps API Key for Android.

## Setup Steps

### 1. Google Maps API Key Setup
Place your Google Maps API Key in `app/build.gradle.kts`:
```kotlin
manifestPlaceholders["MAPS_API_KEY"] = "YOUR_GOOGLE_MAPS_API_KEY"
```
Ensure that the Google Maps SDK for Android is enabled in your Google Cloud Console project.

### 2. Assets Verification
Verify the following assets exist in `app/src/main/assets/`:
- `model_unquant.tflite`
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
