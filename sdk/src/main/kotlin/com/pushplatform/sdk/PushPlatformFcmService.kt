package com.pushplatform.sdk

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Default transport. Hosts with another FCM service call handleRemoteMessage themselves. */
class PushPlatformFcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val sdk = PushPlatform.getInstance()
        sdk.restore(applicationContext)
        sdk.onFcmTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        PushPlatform.getInstance().handleRemoteMessage(applicationContext, message)
    }
}
