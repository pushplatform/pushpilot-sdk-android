# PushPlatform SDK - Public API
-keep class com.pushplatform.sdk.PushPlatform { *; }
-keep interface com.pushplatform.sdk.PushPlatformDelegate { *; }
-keep class com.pushplatform.sdk.models.** { *; }
-keep class com.pushplatform.sdk.PushConfiguration { *; }

# Preserve annotations
-keepattributes Signature
-keepattributes *Annotation*

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }
