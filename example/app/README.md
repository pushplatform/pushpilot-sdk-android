# PushPlatform SDK - Example App

Example Android application demonstrating PushPlatform SDK integration.

## Setup

1. **Add Firebase Configuration**:
   - Go to Firebase Console: https://console.firebase.google.com
   - Create a new project or use existing
   - Add an Android app with package name: `com.pushplatform.example`
   - Download `google-services.json`
   - Place it in `example/app/google-services.json`

2. **Configure API Key**:
   - Open `ExampleApplication.kt`
   - Replace `pk_test_example_key_replace_with_real` with your actual API key

3. **Build and Run**:
   ```bash
   ./gradlew :example:app:installDebug
   ```

## Features

### Installation ID
- Automatically generated on first launch
- Displayed in main activity
- Persists across app restarts

### Notification Permission (Android 13+)
- Request runtime permission button
- Permission status displayed
- Automatic on Android < 13

### User Management
- Login with external user ID
- Logout (clears user association)
- Status feedback for operations

### FCM Integration
- Token automatically registered
- Foreground/background notification handling
- Deep link support via notification tap

## Test Scenarios

1. **First Launch**: Installation ID generated
2. **Permission Request**: Android 13+ runtime permission flow
3. **Login**: Associate user with installation
4. **Logout**: Clear user association
5. **FCM Token**: Automatic registration on app start
6. **Foreground Notification**: Receive while app is open
7. **Background Tap**: Tap notification to open app
8. **Data-only Push**: Callback without system notification
9. **Duplicate Suppression**: Same event_id ignored
10. **Offline Retry**: Registration retried when network restored

## Debugging

Enable debug logs in `ExampleApplication.kt`:
```kotlin
PushPlatform.getInstance().configure(
    context = this,
    apiKey = "pk_...",
    environment = Environment.DEVELOPMENT,
    debugMode = true  // Enable debug logs
)
```

View logs:
```bash
adb logcat -s PushPlatform:V
```

## Architecture

- **ExampleApplication**: SDK initialization and delegate
- **MainActivity**: UI for testing SDK features
- **PushPlatformFcmService**: FCM message handling (from SDK)

## Requirements

- Android 5.0+ (API 21)
- Firebase project with FCM enabled
- PushPlatform API key
