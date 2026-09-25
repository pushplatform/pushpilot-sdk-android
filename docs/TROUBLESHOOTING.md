# Troubleshooting Guide

Common issues and solutions for PushPlatform Android SDK.

## Installation & Setup

### SDK not initializing

**Symptom**: No logs, callbacks not firing

**Solutions**:
1. ✅ Check `configure()` called in `Application.onCreate()`
2. ✅ Verify Application class registered in `AndroidManifest.xml`
3. ✅ Check API key format (starts with `pk_`)
4. ✅ Enable debug mode: `debugMode = true`
5. ✅ Check logcat: `adb logcat -s PushPlatform:V`

**Example**:
```kotlin
// ❌ Wrong - called in Activity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        PushPlatform.getInstance().configure(...)  // Too late!
    }
}

// ✅ Correct - called in Application
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PushPlatform.getInstance().configure(...)
    }
}
```

---

### "SDK not configured" error

**Symptom**: `SdkError.NotConfigured` on `login()` or other calls

**Solution**: Call `configure()` before any other SDK methods

```kotlin
if (!PushPlatform.getInstance().isConfigured()) {
    PushPlatform.getInstance().configure(...)
}
```

---

### Missing google-services.json

**Symptom**: Build error "File google-services.json is missing"

**Solutions**:
1. ✅ Download `google-services.json` from Firebase Console
2. ✅ Place in `app/` directory (not `app/src/main/`)
3. ✅ Verify package name matches
4. ✅ Check `google-services` plugin applied in `build.gradle.kts`

```kotlin
plugins {
    id("com.google.gms.google-services")  // Add this
}
```

---

## FCM Integration

### FCM token not registering

**Symptom**: No `didUpdateFcmToken()` callback, no token in logs

**Solutions**:
1. ✅ Check Firebase project configured correctly
2. ✅ Verify `google-services.json` matches package name
3. ✅ Enable Cloud Messaging API in Firebase Console
4. ✅ Check internet permission: `<uses-permission android:name="android.permission.INTERNET" />`
5. ✅ Wait 30-60 seconds after first launch (FCM token generation delay)

**Debug**:
```bash
adb logcat -s FirebaseMessaging:V PushPlatform:V
```

---

### FCM token registration fails with 401

**Symptom**: `ApiError(401, "Unauthorized")`

**Solutions**:
1. ✅ Verify API key is correct
2. ✅ Check API key has `device-tokens:write` permission
3. ✅ Ensure environment matches API key (dev vs prod)

---

### FCM service not receiving messages

**Symptom**: No `onMessageReceived()` calls

**Solutions**:
1. ✅ Verify `PushPlatformFcmService` declared in manifest:
```xml
<service
    android:name="com.pushplatform.sdk.PushPlatformFcmService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

2. ✅ Check no custom `FirebaseMessagingService` conflicts
3. ✅ Test with app in foreground first
4. ✅ Verify notification contains valid data payload

---

## Notifications

### Notifications not displayed

**Symptom**: `didReceiveNotification()` called but no system notification

**Solutions**:
1. ✅ Check notification permission granted (Android 13+)
2. ✅ Verify notification channels created (Android 8+)
3. ✅ Check app not in battery optimization (kills background)
4. ✅ Test with app in background (foreground notifications suppressed by design)
5. ✅ Check notification payload includes `title` and `body`

**Debug**:
```kotlin
override fun didReceiveNotification(
    notification: ParsedNotification,
    isInForeground: Boolean
) {
    Log.d("Push", "Received: $notification, foreground=$isInForeground")
}
```

---

### Android 13+ permission not granted

**Symptom**: `hasNotificationPermission()` returns `false`

**Solutions**:
1. ✅ Request permission explicitly:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    PushPlatform.getInstance().requestNotificationPermission(this)
}
```

2. ✅ Forward result to SDK:
```kotlin
override fun onRequestPermissionsResult(...) {
    super.onRequestPermissionsResult(...)
    PushPlatform.getInstance().onRequestPermissionsResult(requestCode, grantResults)
}
```

3. ✅ Check permission in manifest:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

### Notification images not loading

**Symptom**: Notification shows without image

**Solutions**:
1. ✅ Check image URL valid and accessible
2. ✅ Verify image under 1MB (Android limit)
3. ✅ Use HTTPS (not HTTP)
4. ✅ Check 5s download timeout sufficient
5. ✅ Test with smaller image first

---

## User Management

### Login fails with network error

**Symptom**: `NetworkError` on `login()`

**Solutions**:
1. ✅ Check internet connectivity
2. ✅ Verify INTERNET permission in manifest
3. ✅ Check device not in airplane mode
4. ✅ Test API endpoint reachable: `curl https://api.pushplatform.com/health`
5. ✅ SDK retries automatically - wait for callback

---

### Login succeeds but notifications not targeted

**Symptom**: Notifications sent to user ID not received

**Solutions**:
1. ✅ Verify `login()` callback returned `Success`
2. ✅ Check installation ID logged on backend
3. ✅ Wait 5-10 seconds for backend sync
4. ✅ Test with broadcast first (no user targeting)

---

## Performance

### SDK initialization slow

**Symptom**: App startup delayed

**Solutions**:
1. ✅ Call `configure()` in `Application.onCreate()` (not Activity)
2. ✅ Disable `debugMode` in production
3. ✅ Check EncryptedSharedPreferences initialization (one-time cost on first launch)

**Normal**: < 100ms initialization time

---

### High memory usage

**Symptom**: SDK using > 5MB memory

**Solutions**:
1. ✅ Check deduplication cache size (max 100 entries)
2. ✅ Verify no memory leaks from delegate callbacks
3. ✅ Use weak references for Activity contexts

**Normal**: < 5MB memory footprint

---

## ProGuard/R8

### SDK methods not found after obfuscation

**Symptom**: `NoSuchMethodError` in release build

**Solutions**:
1. ✅ Consumer ProGuard rules automatically applied
2. ✅ Verify `consumerProguardFiles` in SDK `build.gradle.kts`
3. ✅ Check no manual `-dontwarn` rules for SDK classes

**Manual rules** (if needed):
```proguard
-keep class com.pushplatform.sdk.** { *; }
```

---

### Crash in release build with R8 full mode

**Symptom**: App crashes only with R8 full mode enabled

**Solution**: SDK fully compatible with R8 full mode. If issues persist:
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false  // Try disabling first
        }
    }
}
```

---

## Debugging

### Enable debug logs

```kotlin
PushPlatform.getInstance().configure(
    context = this,
    apiKey = "pk_...",
    debugMode = true  // Enable all logs
)
```

### View SDK logs

```bash
# All SDK logs
adb logcat -s PushPlatform:V

# SDK + FCM logs
adb logcat -s PushPlatform:V FirebaseMessaging:V

# Clear and watch
adb logcat -c && adb logcat -s PushPlatform:V
```

### Check installation ID

```kotlin
val installationId = PushPlatform.getInstance().getInstallationId()
Log.d("Debug", "Installation ID: $installationId")
```

### Verify FCM token

```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    Log.d("Debug", "FCM Token: ${token.take(8)}...")
}
```

### Test notification manually

```bash
curl -X POST https://api.pushplatform.com/v1/notifications \
  -H "Authorization: Bearer $PUSHPLATFORM_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "installation_ids": ["your-installation-id"],
    "notification": {
      "title": "Test",
      "body": "Debug notification"
    }
  }'
```

---

## Common Error Messages

### "Installation ID is null"

**Cause**: SDK not configured or storage access failed

**Solution**: Call `configure()` first, check storage permissions

---

### "FCM token is empty"

**Cause**: Firebase not initialized or network issue

**Solution**: Check `google-services.json`, wait 30-60s, retry

---

### "API request failed: 400"

**Cause**: Invalid request payload

**Solution**: Check API key format, verify request parameters

---

### "API request failed: 401"

**Cause**: Invalid or expired API key

**Solution**: Regenerate API key in dashboard, update in code

---

### "API request failed: 429"

**Cause**: Rate limit exceeded

**Solution**: SDK retries automatically, reduce request frequency

---

### "API request failed: 500"

**Cause**: Server error

**Solution**: SDK retries automatically, check status page

---

## Device-Specific Issues

### Samsung: Notifications delayed

**Cause**: Aggressive battery optimization

**Solution**: Add app to battery optimization whitelist

---

### Xiaomi: Background service killed

**Cause**: MIUI battery saver

**Solution**: Enable autostart permission, disable battery optimization

---

### Huawei: Notifications not received

**Cause**: HMS (Huawei Mobile Services) instead of GMS

**Solution**: SDK requires Google Play Services (FCM)

---

## Still Having Issues?

1. Check [Compatibility Matrix](COMPATIBILITY_MATRIX.md) for supported versions
2. Search [GitHub Issues](https://github.com/pushplatform/sdk-android/issues)
3. Email support: support@pushplatform.com
4. Include: SDK version, Android version, device model, logcat output

---

**Last Updated**: 2026-09-17
