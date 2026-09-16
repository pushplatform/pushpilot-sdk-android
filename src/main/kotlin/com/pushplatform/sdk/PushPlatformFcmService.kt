package com.pushplatform.sdk

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.utils.Logger

class PushPlatformFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.debug("FCM token refreshed: ${Logger.tokenMasked(token)}")

        val pushPlatform = PushPlatform.getInstance()
        if (pushPlatform.isConfigured()) {
            pushPlatform.onFcmTokenRefreshed(token)
        } else {
            Logger.debug("SDK not configured, token will be registered on next configure()")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.debug("FCM message received from: ${message.from}")

        val pushPlatform = PushPlatform.getInstance()
        if (pushPlatform.isConfigured()) {
            pushPlatform.onFcmMessageReceived(message)
        } else {
            Logger.debug("SDK not configured, message ignored")
        }
    }
}
