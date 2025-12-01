# Call Monitor - Android App

Automatic call recording app with encrypted storage and server sync.

## Build APK

### Option 1: Android Studio (Recommended)
1. Open this `android/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Go to `Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. APK will be at `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Command Line (requires Android SDK)
```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/android/sdk

# Build debug APK
./gradlew assembleDebug

# APK location
ls -la app/build/outputs/apk/debug/app-debug.apk
```

## Install on Phone
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Setup
1. Install the APK on your OnePlus device
2. Open the app and grant ALL permissions
3. Enter your laptop's IP address (e.g., `http://192.168.1.100:8000`)
4. Tap "Save Server URL"
5. The app will now auto-record all calls!

## Permissions Required
- Microphone (RECORD_AUDIO)
- Phone State (READ_PHONE_STATE)
- Call Log (READ_CALL_LOG)
- Notifications (POST_NOTIFICATIONS)
