# Compatibility Matrix

Supported versions and tested devices for PushPlatform Android SDK v1.0.0.

## Android Versions

| Version | API Level | Support Status | Notes |
|---------|-----------|----------------|-------|
| Android 14 | 34 | ✅ Fully Supported | Target SDK |
| Android 13 | 33 | ✅ Fully Supported | Runtime notification permission |
| Android 12L | 32 | ✅ Fully Supported | |
| Android 12 | 31 | ✅ Fully Supported | |
| Android 11 | 30 | ✅ Fully Supported | |
| Android 10 | 29 | ✅ Fully Supported | |
| Android 9 Pie | 28 | ✅ Fully Supported | |
| Android 8.1 Oreo | 27 | ✅ Fully Supported | Notification channels |
| Android 8.0 Oreo | 26 | ✅ Fully Supported | Notification channels |
| Android 7.1 Nougat | 25 | ✅ Fully Supported | |
| Android 7.0 Nougat | 24 | ✅ Fully Supported | |
| Android 6.0 Marshmallow | 23 | ✅ Fully Supported | EncryptedSharedPreferences |
| Android 5.1 Lollipop | 22 | ✅ Supported | Plain SharedPreferences fallback |
| Android 5.0 Lollipop | 21 | ✅ Supported | Plain SharedPreferences fallback |
| Android 4.4 KitKat | 19-20 | ❌ Not Supported | Minimum API 21 |

**Recommended**: API 23+ for EncryptedSharedPreferences support

---

## Firebase SDK

| Firebase BOM | firebase-messaging | Support Status | Notes |
|--------------|-------------------|----------------|-------|
| 33.0.0 | 23.4.0 | ✅ Tested | Latest |
| 32.8.0 | 23.4.0 | ✅ Tested | |
| 32.7.0 | 23.4.0 | ✅ Tested | Recommended |
| 32.6.0 | 23.3.1 | ✅ Compatible | |
| 32.5.0 | 23.3.0 | ✅ Compatible | |
| 32.0.0+ | 23.0.0+ | ✅ Compatible | |
| 31.x.x | 22.x.x | ⚠️ Untested | Should work |
| 30.x.x | 21.x.x | ❌ Not Supported | Too old |

**Recommended**: Firebase BOM 32.7.0+

---

## Kotlin

| Version | Support Status | Notes |
|---------|----------------|-------|
| 1.9.20+ | ✅ Tested | SDK built with 1.9.20 |
| 1.9.0+ | ✅ Compatible | |
| 1.8.0+ | ✅ Compatible | |
| 1.7.x | ⚠️ Untested | Should work |
| 1.6.x | ❌ Not Supported | Too old |

---

## Gradle & AGP

| Android Gradle Plugin | Gradle | Support Status |
|----------------------|--------|----------------|
| 8.2.0+ | 8.2+ | ✅ Tested |
| 8.1.0+ | 8.0+ | ✅ Compatible |
| 8.0.0+ | 8.0+ | ✅ Compatible |
| 7.4.0+ | 7.5+ | ⚠️ Untested |
| 7.3.x | 7.4+ | ❌ Not Supported |

**Recommended**: AGP 8.2.0 + Gradle 8.2

---

## AndroidX Libraries

| Library | Version | Required |
|---------|---------|----------|
| androidx.core:core-ktx | 1.12.0+ | Yes |
| androidx.appcompat:appcompat | 1.6.0+ | Yes |
| androidx.security:security-crypto | 1.1.0-alpha06+ | Yes (API 23+) |
| com.google.android.material:material | 1.9.0+ | Recommended |

---

## Tested Devices

### Google Pixel

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| Pixel 8 Pro | 14 (API 34) | ✅ Tested | |
| Pixel 7 | 13 (API 33) | ✅ Tested | Runtime permission |
| Pixel 6 | 12 (API 31) | ✅ Tested | |
| Pixel 5 | 11 (API 30) | ✅ Tested | |
| Pixel 4 | 10 (API 29) | ✅ Tested | |
| Pixel 3 | 9 (API 28) | ✅ Tested | |
| Pixel 2 | 8.1 (API 27) | ✅ Tested | |

### Samsung Galaxy

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| Galaxy S23 | 13 (API 33) | ✅ Tested | OneUI 5.1 |
| Galaxy S22 | 12 (API 31) | ✅ Tested | OneUI 4.1 |
| Galaxy S21 | 11 (API 30) | ✅ Tested | OneUI 3.1 |
| Galaxy S20 | 10 (API 29) | ✅ Tested | OneUI 2.5 |
| Galaxy S10 | 9 (API 28) | ✅ Tested | OneUI 1.5 |
| Galaxy S9 | 8.0 (API 26) | ✅ Tested | Samsung Experience 9.0 |

### OnePlus

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| OnePlus 11 | 13 (API 33) | ✅ Tested | OxygenOS 13 |
| OnePlus 10 Pro | 12 (API 31) | ✅ Tested | OxygenOS 12 |
| OnePlus 9 | 11 (API 30) | ✅ Tested | OxygenOS 11 |
| OnePlus 8T | 11 (API 30) | ✅ Tested | |

### Xiaomi

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| Xiaomi 13 | 13 (API 33) | ✅ Tested | MIUI 14 |
| Xiaomi 12 | 12 (API 31) | ✅ Tested | MIUI 13 |
| Xiaomi 11 | 11 (API 30) | ✅ Tested | MIUI 12.5 |

**Note**: MIUI requires battery optimization whitelist for reliable background notifications

### Oppo

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| Oppo Find X5 | 12 (API 31) | ✅ Tested | ColorOS 12 |
| Oppo Reno 8 | 12 (API 31) | ✅ Tested | |

### Motorola

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| Moto G Power | 11 (API 30) | ✅ Tested | Stock Android |
| Moto Edge | 10 (API 29) | ✅ Tested | |

### Nokia

| Device | Android Version | Status | Notes |
|--------|----------------|--------|-------|
| Nokia 8.3 | 11 (API 30) | ✅ Tested | Android One |
| Nokia 7.2 | 10 (API 29) | ✅ Tested | Android One |

---

## Emulators

| Emulator | Android Version | Status | Notes |
|----------|----------------|--------|-------|
| Android Studio AVD | 14 (API 34) | ✅ Tested | Google Play |
| Android Studio AVD | 13 (API 33) | ✅ Tested | Google Play |
| Android Studio AVD | 12 (API 31) | ✅ Tested | Google Play |
| Android Studio AVD | 11 (API 30) | ✅ Tested | Google Play |
| Android Studio AVD | 10 (API 29) | ✅ Tested | Google Play |
| Android Studio AVD | 9 (API 28) | ✅ Tested | Google Play |
| Android Studio AVD | 8.0 (API 26) | ✅ Tested | Google Play |
| Android Studio AVD | 5.0 (API 21) | ✅ Tested | Google APIs |

**Note**: FCM requires Google Play Services. Use "Google Play" system image.

---

## Manufacturer-Specific Notes

### Samsung

- **OneUI**: Fully supported
- **Battery Optimization**: May delay background notifications
- **Secure Folder**: Notifications work in main profile only
- **Bixby Routines**: No conflicts

### Xiaomi / MIUI

- **Battery Saver**: Very aggressive - whitelist required
- **Autostart Permission**: Must be enabled
- **Notification Settings**: Check per-app notification permissions
- **Recommendation**: Add to battery whitelist during onboarding

### Huawei (HMS)

- **GMS Required**: SDK requires Google Play Services (FCM)
- **HMS Devices**: Not supported (no Google Play Services)
- **Workaround**: None (FCM only)

### Oppo / ColorOS

- **Battery Optimization**: Moderate - whitelist recommended
- **App Standby**: May affect background delivery
- **Recommendation**: Test background scenarios thoroughly

### OnePlus / OxygenOS

- **Battery Optimization**: Standard Android behavior
- **Oxygen Updater**: No conflicts
- **Performance**: Excellent

### Vivo / Funtouch OS

- **Battery Saver**: Aggressive - similar to MIUI
- **iManager**: May kill background services
- **Recommendation**: Whitelist during onboarding

---

## Known Issues

### Android 12+: Splash Screen

**Issue**: SDK initialization during splash screen may delay first notification

**Workaround**: Call `configure()` in `Application.onCreate()`, not Activity

---

### Android 13: Permission Timing

**Issue**: If permission denied initially, notifications silently suppressed

**Workaround**: Show rationale, allow re-request later

---

### Samsung: DeX Mode

**Issue**: Notifications may not show in DeX mode

**Workaround**: None (Samsung limitation)

---

### MIUI: Background Restrictions

**Issue**: Notifications delayed or not delivered in background

**Workaround**: Guide users to whitelist app in battery settings

---

## Testing Recommendations

### Minimum Test Matrix

1. **Android 14 (API 34)**: Latest features
2. **Android 13 (API 33)**: Runtime permission
3. **Android 10 (API 29)**: Common baseline
4. **Android 8.0 (API 26)**: Notification channels
5. **Android 5.0 (API 21)**: Minimum supported

### Device Categories

1. **Google Pixel**: Stock Android reference
2. **Samsung Galaxy**: OneUI (largest market share)
3. **Xiaomi**: MIUI (aggressive power management)
4. **OnePlus**: OxygenOS (performance-oriented)

### Test Scenarios

- [ ] Fresh install (first launch)
- [ ] App in foreground (notification received)
- [ ] App in background (notification tap)
- [ ] App terminated (cold start)
- [ ] Permission denied (graceful degradation)
- [ ] Airplane mode (offline retry)
- [ ] Battery saver enabled
- [ ] Low memory conditions

---

## Version Support Policy

- **Current**: Full support, active development
- **Current - 1**: Bug fixes only
- **Current - 2**: Security fixes only
- **Older**: No support

**Example**: If v2.0.0 released, v1.x.x gets bug fixes, v0.x.x unsupported

---

## Reporting Issues

Include these details:

- SDK version: `1.0.0`
- Android version: `API 33`
- Device: `Pixel 7`
- Firebase BOM: `32.7.0`
- Kotlin: `1.9.20`
- AGP: `8.2.0`
- Logcat output
- Reproduction steps

---

**Last Updated**: 2026-09-17  
**SDK Version**: 1.0.0
