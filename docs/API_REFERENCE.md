# API Reference

Complete reference for PushPlatform Android SDK v1.0.0.

## PushPlatform

Main SDK singleton façade.

### getInstance()

Get SDK singleton instance.

```kotlin
val sdk = PushPlatform.getInstance()
```

**Returns**: `PushPlatform` singleton instance

---

### configure()

Initialize SDK with configuration.

```kotlin
fun configure(
    context: Context,
    apiKey: String,
    environment: Environment = Environment.PRODUCTION,
    debugMode: Boolean = false
)
```

**Parameters**:
- `context`: Application context (use `applicationContext`)
- `apiKey`: Your PushPlatform API key (e.g., `pk_...`)
- `environment`: `Environment.PRODUCTION` or `Environment.DEVELOPMENT`
- `debugMode`: Enable debug logging (default: `false`)

**Example**:
```kotlin
PushPlatform.getInstance().configure(
    context = applicationContext,
    apiKey = "pk_your_key",
    environment = Environment.PRODUCTION,
    debugMode = BuildConfig.DEBUG
)
```

**Note**: Call once in `Application.onCreate()`. Safe to call multiple times (reconfigures SDK).

---

### isConfigured()

Check if SDK is configured.

```kotlin
fun isConfigured(): Boolean
```

**Returns**: `true` if `configure()` was called, `false` otherwise

**Example**:
```kotlin
if (PushPlatform.getInstance().isConfigured()) {
    // SDK ready
}
```

---

### getInstallationId()

Get current installation ID.

```kotlin
fun getInstallationId(): String?
```

**Returns**: UUID installation ID or `null` if not configured

**Example**:
```kotlin
val installationId = PushPlatform.getInstance().getInstallationId()
// Returns: "550e8400-e29b-41d4-a716-446655440000"
```

**Note**: Installation ID persists across app restarts and reinstalls (if EncryptedSharedPreferences intact).

---

### login()

Associate installation with external user ID.

```kotlin
fun login(
    userId: String,
    callback: (UserManager.Result<Unit>) -> Unit
)
```

**Parameters**:
- `userId`: External user identifier (e.g., email, username, UUID)
- `callback`: Result callback (success or failure)

**Example**:
```kotlin
PushPlatform.getInstance().login("user_123") { result ->
    when (result) {
        is UserManager.Result.Success -> {
            // User associated with installation
        }
        is UserManager.Result.Failure -> {
            // Handle error: result.error
        }
    }
}
```

**Backend**: Sends `PATCH /v1/installations/{id}` with `{"external_user_id": "user_123"}`

**Retry**: Automatic retry with exponential backoff for network errors and 5xx responses

---

### logout()

Clear external user ID association.

```kotlin
fun logout(callback: (UserManager.Result<Unit>) -> Unit)
```

**Parameters**:
- `callback`: Result callback (success or failure)

**Example**:
```kotlin
PushPlatform.getInstance().logout { result ->
    when (result) {
        is UserManager.Result.Success -> {
            // User disassociated from installation
        }
        is UserManager.Result.Failure -> {
            // Handle error: result.error
        }
    }
}
```

**Backend**: Sends `PATCH /v1/installations/{id}` with `{"external_user_id": null}`

**Note**: Installation remains active after logout (can receive broadcasts).

---

### hasNotificationPermission()

Check if notification permission granted.

```kotlin
fun hasNotificationPermission(): Boolean
```

**Returns**: `true` if permission granted, `false` otherwise

**Example**:
```kotlin
if (PushPlatform.getInstance().hasNotificationPermission()) {
    // Can show notifications
} else {
    // Request permission
}
```

**Note**: Always returns `true` on Android < 13 (no runtime permission required).

---

### requestNotificationPermission()

Request notification permission (Android 13+).

```kotlin
fun requestNotificationPermission(activity: Activity)
```

**Parameters**:
- `activity`: Current activity for permission dialog

**Example**:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    PushPlatform.getInstance().requestNotificationPermission(this)
}
```

**Result**: Delivered via `PushPlatformDelegate.onNotificationPermissionResult()`

**Note**: No-op on Android < 13.

---

### onRequestPermissionsResult()

Forward permission result to SDK.

```kotlin
fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray)
```

**Parameters**:
- `requestCode`: Request code from system callback
- `grantResults`: Grant results array from system callback

**Example**:
```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    PushPlatform.getInstance().onRequestPermissionsResult(requestCode, grantResults)
}
```

---

### delegate

Set delegate for SDK callbacks.

```kotlin
var delegate: PushPlatformDelegate?
```

**Example**:
```kotlin
PushPlatform.getInstance().delegate = object : PushPlatformDelegate {
    override fun didInitialize(installationId: String) {
        // SDK initialized
    }
    
    override fun didReceiveNotification(
        notification: ParsedNotification,
        isInForeground: Boolean
    ) {
        // Notification received
    }
}
```

---

## PushPlatformDelegate

Delegate interface for SDK callbacks.

### didInitialize()

Called when SDK initialization completes.

```kotlin
fun didInitialize(installationId: String)
```

**Parameters**:
- `installationId`: Generated installation UUID

**Example**:
```kotlin
override fun didInitialize(installationId: String) {
    Log.d("Push", "SDK ready: $installationId")
    // Send installation ID to your backend
}
```

---

### didUpdateFcmToken()

Called when FCM token registered successfully.

```kotlin
fun didUpdateFcmToken(token: String)
```

**Parameters**:
- `token`: FCM token (first 8 chars only in debug logs)

**Example**:
```kotlin
override fun didUpdateFcmToken(token: String) {
    Log.d("Push", "Token registered")
}
```

**Note**: Optional callback (can be empty).

---

### didFailToRegisterFcmToken()

Called when FCM token registration fails.

```kotlin
fun didFailToRegisterFcmToken(error: SdkError)
```

**Parameters**:
- `error`: Error details (`NetworkError`, `ApiError`, etc.)

**Example**:
```kotlin
override fun didFailToRegisterFcmToken(error: SdkError) {
    when (error) {
        is SdkError.NetworkError -> Log.e("Push", "Network error")
        is SdkError.ApiError -> Log.e("Push", "API error: ${error.statusCode}")
        else -> Log.e("Push", "Error: ${error.message}")
    }
}
```

**Note**: Optional callback (can be empty).

---

### didReceiveNotification()

Called when notification received (foreground or background tap).

```kotlin
fun didReceiveNotification(
    notification: ParsedNotification,
    isInForeground: Boolean
)
```

**Parameters**:
- `notification`: Parsed notification object
- `isInForeground`: `true` if app was in foreground, `false` if background tap

**Example**:
```kotlin
override fun didReceiveNotification(
    notification: ParsedNotification,
    isInForeground: Boolean
) {
    Log.d("Push", "Title: ${notification.title}")
    Log.d("Push", "Body: ${notification.body}")
    
    if (isInForeground) {
        // App was open - show in-app alert
        showInAppNotification(notification)
    } else {
        // User tapped notification - navigate
        notification.deepLink?.let { url ->
            navigateToDeepLink(url)
        }
    }
    
    // Access custom data
    val orderId = notification.customData["order_id"]
}
```

**Note**: Always called for data-only messages. For notification messages, only called in foreground or on tap.

---

### onNotificationPermissionResult()

Called when notification permission request completes (Android 13+).

```kotlin
fun onNotificationPermissionResult(granted: Boolean)
```

**Parameters**:
- `granted`: `true` if permission granted, `false` if denied

**Example**:
```kotlin
override fun onNotificationPermissionResult(granted: Boolean) {
    if (granted) {
        Log.d("Push", "Permission granted")
    } else {
        Log.w("Push", "Permission denied")
        // Show rationale or alternative
    }
}
```

**Note**: Optional callback (can be empty).

---

## ParsedNotification

Notification data object.

```kotlin
data class ParsedNotification(
    val title: String?,
    val body: String?,
    val imageUrl: String?,
    val deepLink: String?,
    val eventId: String?,
    val callId: String?,
    val channelId: String?,
    val tag: String?,
    val customData: Map<String, String>
)
```

**Fields**:
- `title`: Notification title
- `body`: Notification body text
- `imageUrl`: Image URL (downloaded and displayed)
- `deepLink`: Deep link URL (e.g., `myapp://order/123`)
- `eventId`: Unique event identifier (for deduplication)
- `callId`: Call identifier (for high-priority call notifications)
- `channelId`: Android notification channel ID
- `tag`: Notification tag (for grouping)
- `customData`: Additional key-value data

**Example**:
```kotlin
fun handleNotification(notification: ParsedNotification) {
    notification.deepLink?.let { url ->
        when {
            url.startsWith("myapp://order/") -> {
                val orderId = url.removePrefix("myapp://order/")
                openOrder(orderId)
            }
            url.startsWith("myapp://profile/") -> {
                val userId = url.removePrefix("myapp://profile/")
                openProfile(userId)
            }
        }
    }
}
```

---

## SdkError

Error types.

```kotlin
sealed class SdkError : Exception() {
    object NotConfigured : SdkError()
    data class NetworkError(val underlying: Throwable) : SdkError()
    data class ApiError(val statusCode: Int, override val message: String) : SdkError()
    object InvalidToken : SdkError()
    object MaxRetriesExceeded : SdkError()
    object StorageError : SdkError()
}
```

**Types**:
- `NotConfigured`: SDK not initialized (call `configure()` first)
- `NetworkError`: Network failure (no connection, timeout, etc.)
- `ApiError`: API returned error (400, 401, 500, etc.)
- `InvalidToken`: FCM token malformed
- `MaxRetriesExceeded`: Retry limit reached
- `StorageError`: Secure storage access failed

**Example**:
```kotlin
when (error) {
    is SdkError.NotConfigured -> showAlert("SDK not initialized")
    is SdkError.NetworkError -> showAlert("Network error")
    is SdkError.ApiError -> showAlert("API error: ${error.statusCode}")
    else -> showAlert(error.message)
}
```

---

## Environment

SDK environment enum.

```kotlin
enum class Environment {
    DEVELOPMENT,  // https://api-dev.pushplatform.com
    PRODUCTION    // https://api.pushplatform.com
}
```

**Example**:
```kotlin
val environment = if (BuildConfig.DEBUG) {
    Environment.DEVELOPMENT
} else {
    Environment.PRODUCTION
}
```

---

## Best Practices

### 1. Configure Once

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PushPlatform.getInstance().configure(...)
    }
}
```

### 2. Handle All Delegate Callbacks

```kotlin
PushPlatform.getInstance().delegate = object : PushPlatformDelegate {
    override fun didInitialize(installationId: String) { /* Required */ }
    override fun didReceiveNotification(...) { /* Required */ }
    // Optional: didUpdateFcmToken, didFailToRegisterFcmToken, onNotificationPermissionResult
}
```

### 3. Request Permission Early

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (!PushPlatform.getInstance().hasNotificationPermission()) {
        PushPlatform.getInstance().requestNotificationPermission(this)
    }
}
```

### 4. Associate User After Login

```kotlin
fun onUserLogin(userId: String) {
    PushPlatform.getInstance().login(userId) { result ->
        // Handle result
    }
}
```

### 5. Clear User on Logout

```kotlin
fun onUserLogout() {
    PushPlatform.getInstance().logout { result ->
        // Handle result
    }
}
```

---

**Version**: 1.0.0  
**Last Updated**: 2026-09-17
