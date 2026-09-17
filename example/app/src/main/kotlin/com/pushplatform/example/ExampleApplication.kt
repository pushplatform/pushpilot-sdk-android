package com.pushplatform.example

import android.app.Application
import com.pushplatform.sdk.Environment
import com.pushplatform.sdk.PushPlatform
import com.pushplatform.sdk.PushPlatformDelegate
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.notifications.ParsedNotification
import com.pushplatform.sdk.utils.Logger

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Logger.debug("ExampleApplication: initializing PushPlatform SDK")

        PushPlatform.getInstance().configure(
            context = this,
            apiKey = "pk_test_example_key_replace_with_real",
            environment = Environment.DEVELOPMENT,
            debugMode = true
        )

        PushPlatform.getInstance().delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) {
                Logger.info("SDK initialized with installation ID: $installationId")
            }

            override fun didUpdateFcmToken(token: String) {
                Logger.info("FCM token updated: ${token.take(8)}...")
            }

            override fun didFailToRegisterFcmToken(error: SdkError) {
                Logger.error("Failed to register FCM token: ${error.message}")
            }

            override fun didReceiveNotification(notification: ParsedNotification, isInForeground: Boolean) {
                Logger.info("Received notification: ${notification.title}, foreground=$isInForeground")
            }

            override fun onNotificationPermissionResult(granted: Boolean) {
                Logger.info("Notification permission result: granted=$granted")
            }
        }

        val installationId = PushPlatform.getInstance().getInstallationId()
        Logger.info("Current installation ID: $installationId")
    }
}
