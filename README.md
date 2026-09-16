# Android SDK

Push notification SDK for Android (API 21+)

## Installation

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.pushplatform:sdk-android:1.0.0")
}
```

## Quick Start

```kotlin
// Initialize SDK in Application.onCreate()
PushPlatform.getInstance().configure(
    context = this,
    apiKey = "pk_your_api_key",
    environment = Environment.PRODUCTION,
    debugMode = BuildConfig.DEBUG
)

// Get installation ID
val installationId = PushPlatform.getInstance().getInstallationId()
```

## Requirements

- Android 5.0 (API 21) or higher
- Target SDK 34
- Kotlin 1.8+

## Features

- ✅ Installation ID management (EncryptedSharedPreferences)
- ✅ Secure token storage
- ✅ Debug logging with token masking
- ⏳ FCM token registration (coming soon)
- ⏳ Notification handling (coming soon)
- ⏳ VoIP push support (coming soon)

## Documentation

See `docs/` for complete documentation.

## License

Apache 2.0
