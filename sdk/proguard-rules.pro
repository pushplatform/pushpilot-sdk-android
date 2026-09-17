# PushPlatform SDK - Consumer ProGuard Rules
# These rules are automatically applied to apps that use the SDK

# Public API - Keep all public SDK classes
-keep class com.pushplatform.sdk.PushPlatform { *; }
-keep interface com.pushplatform.sdk.PushPlatformDelegate { *; }
-keep class com.pushplatform.sdk.PushConfiguration { *; }
-keep class com.pushplatform.sdk.Environment { *; }

# Models - Keep all model classes (used in public API)
-keep class com.pushplatform.sdk.models.** { *; }

# Notifications - Keep notification data classes
-keep class com.pushplatform.sdk.notifications.ParsedNotification { *; }

# User Manager - Keep result types
-keep class com.pushplatform.sdk.core.UserManager$Result { *; }
-keep class com.pushplatform.sdk.core.UserManager$Result$Success { *; }
-keep class com.pushplatform.sdk.core.UserManager$Result$Failure { *; }

# FirebaseMessagingService - Must be discoverable by Firebase
-keep class com.pushplatform.sdk.PushPlatformFcmService { *; }

# Preserve annotations for reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# EncryptedSharedPreferences compatibility
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class * extends androidx.security.crypto.EncryptedSharedPreferences {
    <init>(...);
}

# OkHttp (if not already included by app)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Firebase (if not already included by app)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Kotlin Coroutines (if used in future)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# R8 full mode compatibility
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Debugging - Remove in production
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

