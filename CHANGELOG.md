# Changelog

All notable changes to PushPlatform Android SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-17

### Added

#### Core SDK
- **Installation Management**: UUID-based installation ID with EncryptedSharedPreferences (API 23+)
- **Configuration**: Single `configure()` call with API key, environment, and debug mode
- **Singleton Pattern**: Thread-safe `PushPlatform.getInstance()` access
- **Secure Storage**: EncryptedSharedPreferences with plain SharedPreferences fallback (API 21-22)
- **Debug Logging**: Token-masked logging (first 8 characters only)

#### FCM Integration
- **Token Registration**: Automatic FCM token registration with backend
- **Token Lifecycle**: `onNewToken()` handling with change detection
- **Retry Logic**: Exponential backoff (2^attempt, max 60s, 5 retries)
- **Error Handling**: Retryable (5xx, 429, network) vs permanent (4xx, 401) failures
- **FirebaseMessagingService**: Custom `PushPlatformFcmService` implementation

#### Notification Handling
- **Notification Parser**: Parses FCM `notification` object and `data` payload
- **Deduplication Cache**: LRU cache with 24h TTL (max 100 entries, thread-safe)
- **Foreground Detection**: `AppLifecycleTracker` with ActivityLifecycleCallbacks
- **Notification Display**: NotificationCompat with image download (5s timeout)
- **Deep Links**: Intent.ACTION_VIEW support via notification tap
- **Channel Management**: Default and high-priority channels (Android 8+)

#### Permission Management
- **Runtime Permissions**: Android 13+ POST_NOTIFICATIONS support
- **Permission Checker**: `hasNotificationPermission()` and `requestNotificationPermission()`
- **Activity Integration**: `onRequestPermissionsResult()` forwarding
- **Backward Compatibility**: Automatic grant on Android < 13

#### User Management
- **Login**: Associate installation with external user ID
- **Logout**: Clear user association (installation remains active)
- **API Integration**: `PATCH /v1/installations/{id}` with retry logic
- **Async Callbacks**: `UserManager.Result<Unit>` with Success/Failure

#### REST API Client
- **HTTP Client**: OkHttp 4.12.0 with 30s timeout
- **Endpoints**: POST /v1/installations, PATCH /v1/installations/{id}, POST /v1/installations/{id}/subscriptions
- **Authorization**: Bearer token authentication
- **Error Mapping**: HTTP status codes → SdkError types

#### Delegate Pattern
- **PushPlatformDelegate**: Protocol-based callback interface
- **Callbacks**: didInitialize, didUpdateFcmToken, didFailToRegisterFcmToken, didReceiveNotification, onNotificationPermissionResult
- **Thread-Safe**: All callbacks on main thread

#### Models
- **ParsedNotification**: title, body, imageUrl, deepLink, eventId, callId, channelId, tag, customData
- **SdkError**: NotConfigured, NetworkError, ApiError, InvalidToken, MaxRetriesExceeded, StorageError
- **Environment**: DEVELOPMENT, PRODUCTION

#### ProGuard/R8
- **Consumer Rules**: Auto-applied via consumerProguardFiles
- **Public API Preservation**: PushPlatform, PushPlatformDelegate, models
- **Full R8 Compatibility**: Tested with R8 full mode

### Testing
- **150 Unit Tests**: Comprehensive coverage (1.63:1 test/prod ratio)
- **Integration Tests**: 8 end-to-end SDK flow tests
- **Robolectric**: Android API mocking for unit tests
- **MockWebServer**: HTTP client testing
- **Example App**: Complete reference implementation

### Documentation
- **README.md**: Overview, quick start, features (150+ lines)
- **QUICK_START.md**: 15-minute integration guide (430+ lines)
- **API_REFERENCE.md**: Complete API documentation (750+ lines)
- **TROUBLESHOOTING.md**: 15+ common issues with solutions (450+ lines)
- **COMPATIBILITY_MATRIX.md**: Supported versions and 35+ tested devices (500+ lines)
- **Example App README**: Setup guide with 10 test scenarios (80+ lines)

### Infrastructure
- **Build System**: Gradle 8.2 with Kotlin DSL
- **Kotlin**: 1.9.20
- **Android Gradle Plugin**: 8.2.0
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 21 (Android 5.0)
- **Firebase BOM**: 32.7.0
- **OkHttp**: 4.12.0

### Architecture
- **Package Structure**: core/, notifications/, models/, utils/
- **Dependency Injection**: Constructor-based, testable
- **Thread Safety**: Synchronized singletons, AtomicInteger, ConcurrentHashMap
- **Error Handling**: Sealed class hierarchy, exhaustive when expressions
- **Async Operations**: Callback-based with CountDownLatch for tests

### Security
- **EncryptedSharedPreferences**: AES256-GCM encryption (API 23+)
- **Token Masking**: Only first 8 characters logged
- **No Plaintext Storage**: No tokens or secrets in plain SharedPreferences
- **HTTPS Only**: All API calls over TLS

### Performance
- **Initialization**: < 100ms on modern devices
- **Memory Footprint**: < 5MB
- **Binary Size**: < 500KB AAR
- **Network**: 30s timeout, exponential backoff
- **Cache**: LRU with 24h TTL, max 100 entries

### Compatibility
- **Android API 21-34**: Full support
- **Firebase BOM 32.0.0+**: Tested
- **Kotlin 1.8.0+**: Compatible
- **AGP 8.0.0+**: Compatible
- **ProGuard/R8**: Full compatibility

### Known Limitations
- **FCM Only**: Google Play Services required (no HMS support)
- **Foreground Service**: Not implemented (future enhancement)
- **Custom Sounds**: Not supported in v1.0.0
- **Notification Actions**: Not supported in v1.0.0
- **Message History**: Not persisted locally

### Migration Notes
- **From OneSignal**: Dual registration supported, deduplication by event_id
- **Clean Install**: Installation ID generated on first launch
- **Reinstall**: New installation ID unless EncryptedSharedPreferences intact

---

## [Unreleased]

### Planned for v1.1.0
- Notification action buttons
- Custom notification sounds
- Foreground service for background reliability
- Message history persistence
- Rich media support (audio, video)

### Planned for v2.0.0
- HMS (Huawei Mobile Services) support
- WebSocket real-time connection
- Offline message queue
- Analytics integration
- A/B testing support

---

## Version Support

- **v1.0.x**: Full support (current)
- **v0.x.x**: Not released

---

**Release Date**: 2026-09-17  
**Git Tag**: v1.0.0  
**Build**: 1000
