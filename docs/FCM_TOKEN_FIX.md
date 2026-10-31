# Android SDK 1.0.1 — initial FCM token registration

The 1.0.0 installation callback succeeded without fetching an existing Firebase token. `onNewToken` before configure was dropped; a refresh before the backend installation existed was saved but not replayed. An existing token need not rotate on the next launch. `FcmTokenManager` also forced production and the SDK sent its own package instead of the host package. Android RN/Expo and Flutter wrappers inherited the defect. iOS and Web are unaffected.

1.0.1 retrieves `FirebaseMessaging.getInstance().token` during configure, joins it with successful backend installation registration and forwards early service callbacks. Registration uses the configured environment, host package and current configuration's API client/installation. Token rotations are serialized; stale Firebase/installation callbacks cannot overwrite a newer configuration. Existing public configure/initialize signatures and registration delegate events remain unchanged. Successful initialization means an installation exists; token registration has its separate success/error event. Host applications still configure Firebase and request notification permission normally. No raw token is exposed to JS/Dart and no manual API workaround is required.

## Targeted verification

- The new existing-token test was run against unmodified published 1.0.0 source. It failed at the expected missing backend-token-registration assertion. Modified source was restored automatically.
- Eight Robolectric/MockWebServer tests pass: existing token without onNewToken; service callback before configure; rotation while installation is pending; rotation after setup; serialized in-flight rotation; Firebase failure; backend token rejection; stale callbacks after reconfiguration. Assertions inspect actual installation/token HTTP paths, Authorization, token, development/production, host package and delegate success/failure.
- Existing FcmTokenManager tests pass. These are targeted registration tests, not a claim of physical FCM delivery. No real Firebase credentials, device push, charges or customer DB writes are used.
- Release AAR, POM, module metadata, sources and documentation are built into local Maven staging. Publication is immutable under a new 1.0.1 coordinate. Maven metadata retains old versions.

[Firebase token retrieval contract](https://firebase.google.com/docs/cloud-messaging/android/client#retrieve-the-current-registration-token).

## Published artifacts and consumer

- Source commit: `5e53471`, release tag `v1.0.1`; public Maven distribution commit `153f119`.
- Coordinate: `com.pushplatform:sdk-android:1.0.1`, existing anonymous GitHub Maven endpoint.
- Downloaded public AAR is byte-for-byte equal to the verified candidate; SHA-256 `f3f00ddf817f687a6a2df45848e031b772b5f1e3c7c5e470c0b8aa80d5593e67`.
- One fresh external Android consumer resolved the new public coordinate and compiled/dexed/assembled a debug APK: 33 tasks, BUILD SUCCESSFUL. No local SDK source, Maven Local, GitHub credentials, Google services config or application execution was involved.
- The old 1.0.0 coordinate was not modified. Maven metadata retains both versions.
- Wrappers consuming this fix: RN/Expo 1.0.2 (bundled AAR), Flutter 1.0.2 (public Maven dependency). iOS/Web are unchanged.

