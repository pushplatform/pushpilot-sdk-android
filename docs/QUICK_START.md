# Quick Start Guide

Complete step-by-step guide to integrate PushPlatform Android SDK in 15 minutes.

## Prerequisites

- Android Studio Arctic Fox or later
- Android project with `minSdk 21+`
- Firebase project with FCM enabled
- PushPlatform API key

## Step 1: Firebase Setup (5 minutes)

### 1.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project" or select existing project
3. Add Android app with your package name
4. Download `google-services.json`
5. Place it in `app/` directory

### 1.2 Add Firebase Dependencies

In your project-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

In your app-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")  // Add this
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

## Step 2: Install SDK (2 minutes)

### 2.1 Add SDK Dependency

```kotlin
dependencies {
    implementation("com.pushplatform:sdk-android:1.0.0")
}
```

### 2.2 Add Permissions

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Step 3: Initialize SDK (3 minutes)

### 3.1 Create Application Class

```kotlin
package com.example.myapp

import android.app.Application
import com.pushplatform.sdk.Environment
import com.pushplatform.sdk.PushPlatform
import com.pushplatform.sdk.PushPlatformDelegate
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.notifications.ParsedNotification

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize SDK
        PushPlatform.getInstance().configure(
            context = this,
            apiKey = "pk_your_api_key_here",  // Replace with your API key
            environment = Environment.PRODUCTION,
            debugMode = BuildConfig.DEBUG
        )
        
        // Set delegate for callbacks
        PushPlatform.getInstance().delegate = object : PushPlatformDelegate {
            
            override fun didInitialize(installationId: String) {
                // SDK initialized successfully
                android.util.Log.d("PushPlatform", "Installation ID: $installationId")
            }
            
            override fun didUpdateFcmToken() {
                // FCM token registered successfully
                android.util.Log.d("PushPlatform", "FCM token registered successfully")
            }
            
            override fun didFailToRegisterFcmToken(error: SdkError) {
                // Token registration failed
                android.util.Log.e("PushPlatform", "Token failed: ${error.message}")
            }
            
            override fun didReceiveNotification(
                notification: ParsedNotification,
                isInForeground: Boolean
            ) {
                // Notification received
                android.util.Log.d("PushPlatform", 
                    "Notification: ${notification.title} (foreground=$isInForeground)")
                
                // Handle notification tap, deep link, etc.
                notification.deepLink?.let { url ->
                    // Navigate to deep link
                }
            }
            
            override fun onNotificationPermissionResult(granted: Boolean) {
                // Permission request result (Android 13+)
                android.util.Log.d("PushPlatform", "Permission granted: $granted")
            }
        }
    }
}
```

### 3.2 Register Application Class

In `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...>
```

## Step 4: Request Permissions (2 minutes)

### 4.1 Add Permission Request

In your main Activity:

```kotlin
class MainActivity : AppCompatActivity() {
    
    private val PERMISSION_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Request notification permission (Android 13+)
        requestNotificationPermission()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PushPlatform.getInstance().hasNotificationPermission()) {
                PushPlatform.getInstance().requestNotificationPermission(this)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PushPlatform.getInstance().onRequestPermissionsResult(requestCode, grantResults)
    }
}
```

## Step 5: Associate User (Optional, 3 minutes)

### 5.1 Login User

```kotlin
fun loginUser(userId: String) {
    PushPlatform.getInstance().login(userId) { result ->
        when (result) {
            is UserManager.Result.Success -> {
                Log.d("Push", "User logged in: $userId")
                // Update UI
            }
            is UserManager.Result.Failure -> {
                Log.e("Push", "Login failed: ${result.error.message}")
                // Show error
            }
        }
    }
}
```

### 5.2 Logout User

```kotlin
fun logoutUser() {
    PushPlatform.getInstance().logout { result ->
        when (result) {
            is UserManager.Result.Success -> {
                Log.d("Push", "User logged out")
                // Clear user session
            }
            is UserManager.Result.Failure -> {
                Log.e("Push", "Logout failed: ${result.error.message}")
            }
        }
    }
}
```

## Step 6: Test Integration

### 6.1 Run App

```bash
./gradlew installDebug
```

### 6.2 Check Logs

```bash
adb logcat -s PushPlatform:V
```

Expected output:
```
D/PushPlatform: SDK configured with installation ID: abc123...
D/PushPlatform: FCM token updated: eE4jB7kL...
```

### 6.3 Send Test Notification

Use PushPlatform dashboard or API:

```bash
curl -X POST https://api.pushplatform.com/v1/notifications \
  -H "Authorization: Bearer pk_your_api_key" \
  -H "Content-Type: application/json" \
  -d '{
    "installation_ids": ["abc123..."],
    "notification": {
      "title": "Test",
      "body": "Hello from PushPlatform!"
    }
  }'
```

## Troubleshooting

### SDK not initializing

- ✅ Check API key is correct
- ✅ Verify `google-services.json` is in `app/` directory
- ✅ Check internet permission in manifest

### FCM token not registering

- ✅ Verify Firebase project is configured correctly
- ✅ Check `google-services.json` matches package name
- ✅ Enable FCM API in Firebase Console

### Notifications not received

- ✅ Check notification permission granted (Android 13+)
- ✅ Verify FCM token registered in logs
- ✅ Test with app in foreground first
- ✅ Check battery optimization settings

## Next Steps

- [API Reference](API_REFERENCE.md) - Complete API documentation
- [Troubleshooting Guide](TROUBLESHOOTING.md) - Common issues
- [Example App](../example/app/README.md) - Reference implementation

## Support

- Email: support@pushplatform.com
- Docs: https://docs.pushplatform.com
- Issues: https://github.com/pushplatform/sdk-android/issues

---

**Estimated Time**: 15 minutes  
**Difficulty**: Beginner
