# Compatibility — Android SDK 2.0.4

Build requirements and dependencies are defined in `sdk/build.gradle.kts`:

| Setting | Value |
|---------|-------|
| Minimum Android API | 21 |
| Compile SDK | 34 |
| Java/Kotlin bytecode target | 11 |
| Firebase Messaging | 24.1.0 |
| AndroidX Core KTX | 1.12.0 |
| AndroidX Security Crypto | 1.1.0-alpha06 |
| OkHttp | 4.12.0 |

The repository builds with Android Gradle Plugin 8.11.1 and Kotlin 1.9.24.
Wrapper requirements may be stricter: the React Native bridge requires Android API 26.

Applications using Firebase BoM should inspect the resolved Firebase Messaging version
with Gradle's dependency report. The SDK uses the main `firebase-messaging` artifact,
not an unversioned KTX dependency.

Initialize in the application process before forwarding FCM callbacks. An existing
FirebaseMessagingService must forward messages and refreshed tokens; see
[QUICK_START.md](QUICK_START.md). Android 13+ requires notification permission.
Channel settings and manufacturer battery restrictions affect background execution
and presentation. System force-stop requires the user to launch the app again.

Incoming-call UI, full-screen intent permissions, signalling and audio belong to
the host app. Native transport processing does not depend on a JS/Dart runtime.
Test the complete integration on the OS versions and manufacturers supported by
your app; unit tests do not certify every device combination.
