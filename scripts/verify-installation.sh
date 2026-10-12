#!/usr/bin/env bash
set -euo pipefail

# Build a new consumer against the public Maven endpoint, without SDK sources.
root=$(cd "$(dirname "$0")/.." && pwd)
consumer=$(mktemp -d "${TMPDIR:-/tmp}/pushpilot-android-consumer.XXXXXX")
mkdir -p "$consumer/gradle/wrapper" "$consumer/app/src/main/kotlin/example"
cp "$root/gradlew" "$consumer/gradlew"
cp "$root/gradle/wrapper/gradle-wrapper.jar" "$root/gradle/wrapper/gradle-wrapper.properties" "$consumer/gradle/wrapper/"
cat > "$consumer/settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven { url = uri("https://raw.githubusercontent.com/pushplatform/pushpilot-sdk-android/maven/") }
            }
            filter { includeModule("com.pushplatform", "sdk-android") }
        }
    }
}
rootProject.name = "pushpilot-public-maven-consumer"
include(":app")
EOF
cat > "$consumer/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
EOF
cat > "$consumer/gradle.properties" <<'EOF'
android.useAndroidX=true
org.gradle.jvmargs=-Xmx2048m
EOF
cat > "$consumer/app/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "example.pushpilot"
    compileSdk = 34
    defaultConfig {
        applicationId = "example.pushpilot"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}
dependencies { implementation("com.pushplatform:sdk-android:1.0.0") }
EOF
cat > "$consumer/app/src/main/AndroidManifest.xml" <<'EOF'
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="PushPilot installation check" />
</manifest>
EOF
cat > "$consumer/app/src/main/kotlin/example/Consumer.kt" <<'EOF'
package example

import com.pushplatform.sdk.PushPlatform
import com.pushplatform.sdk.Environment

// Compile/link only: no configure(), network request, emulator, or push.
fun sdk() = PushPlatform.getInstance()
fun environment() = Environment.PRODUCTION
EOF
printf 'Consumer directory: %s\n' "$consumer"
"$consumer/gradlew" -p "$consumer" :app:assembleDebug --no-daemon --console=plain
printf 'Verified APK: %s/app/build/outputs/apk/debug/app-debug.apk\n' "$consumer"
