# PushPlatform Android SDK

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Native Android SDK for PushPlatform - a modern, production-ready push notification platform.

## Features

- **FCM Integration**: Seamless Firebase Cloud Messaging support
- **Installation Management**: Automatic installation ID generation and persistence
- **User Authentication**: Associate installations with external user IDs
- **Notification Handling**: Foreground, background, and terminated state support
- **Permission Management**: Android 13+ runtime notification permissions
- **Deduplication**: Event-based notification deduplication (24h TTL)
- **Offline Support**: Automatic retry with exponential backoff
- **Debug Logging**: Token-masked debug output (no leaks)
- **ProGuard/R8**: Full obfuscation support with consumer rules

## Requirements

- **Android API 21+** (Android 5.0 Lollipop)
- **Target SDK 34** (Android 14)
- **Kotlin 1.9+**
- **Firebase Cloud Messaging** (via Firebase BOM 32.7.0+)

## Quick Start

### 1. Add Dependencies

```kotlin
dependencies {
    implementation("com.pushplatform:sdk-android:1.0.0")
    
    // Firebase (if not already included)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

### 2. Initialize SDK

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        PushPlatform.getInstance().configure(
            context = this,
            apiKey = "pk_your_api_key",
            environment = Environment.PRODUCTION,
            debugMode = BuildConfig.DEBUG
        )
        
        PushPlatform.getInstance().delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) {
                Log.d("Push", "Initialized: $installationId")
            }
            
            override fun didReceiveNotification(
                notification: ParsedNotification,
                isInForeground: Boolean
            ) {
                Log.d("Push", "Notification: ${notification.title}")
            }
        }
    }
}
```

### 3. Request Permissions (Android 13+)

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    PushPlatform.getInstance().requestNotificationPermission(this)
}
```

### 4. Associate User

```kotlin
PushPlatform.getInstance().login("user_123") { result ->
    when (result) {
        is UserManager.Result.Success -> Log.d("Push", "Logged in")
        is UserManager.Result.Failure -> Log.e("Push", "Login failed: ${result.error}")
    }
}
```

## Documentation

- [Quick Start Guide](docs/QUICK_START.md) - Step-by-step integration
- [API Reference](docs/API_REFERENCE.md) - Complete API documentation
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions
- [Compatibility Matrix](docs/COMPATIBILITY_MATRIX.md) - Supported versions

## Architecture

```
sdk-android/
├── core/                  # Installation, API client, storage
├── notifications/         # FCM service, parsing, deduplication
├── models/                # Data models and errors
└── utils/                 # Logging, permissions, lifecycle
```

**Key Components**:
- `PushPlatform`: Singleton SDK façade
- `InstallationManager`: UUID-based installation identity
- `ApiClient`: REST API v1 client (OkHttp)
- `PushPlatformFcmService`: Firebase message handler
- `NotificationHandler`: System notification display
- `UserManager`: External user ID management

## Testing

- **150+ Unit Tests**: Comprehensive test coverage
- **Integration Tests**: End-to-end SDK flows
- **Example App**: Complete reference implementation

```bash
./gradlew sdk-android:testDebugUnitTest
./gradlew sdk-android:build
```

## ProGuard/R8

Consumer rules are automatically applied. No manual configuration needed.

```proguard
-keep class com.pushplatform.sdk.PushPlatform { *; }
-keep interface com.pushplatform.sdk.PushPlatformDelegate { *; }
```

## Security

- **Encrypted Storage**: EncryptedSharedPreferences for Installation ID (API 23+)
- **Token Masking**: Only first 8 characters logged in debug mode
- **No Plaintext Secrets**: API keys transmitted via HTTPS only

## Support

- **Issues**: [GitHub Issues](https://github.com/pushplatform/sdk-android/issues)
- **Email**: support@pushplatform.com
- **Docs**: https://docs.pushplatform.com

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

---

**Current Version**: 1.0.0  
**Last Updated**: 2026-09-17
