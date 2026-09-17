package com.pushplatform.sdk

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.notifications.DeduplicationCache
import com.pushplatform.sdk.notifications.NotificationHandler
import com.pushplatform.sdk.notifications.NotificationParser
import com.pushplatform.sdk.utils.Logger

class PushPlatformFcmService : FirebaseMessagingService() {

    private val parser = NotificationParser()
    private val deduplicationCache = DeduplicationCache()

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
        if (!pushPlatform.isConfigured()) {
            Logger.debug("SDK not configured, message ignored")
            return
        }

        // Parse notification
        val parsed = parser.parse(message)

        // Check deduplication
        val eventId = parsed.eventId
        if (eventId != null) {
            if (deduplicationCache.contains(eventId)) {
                Logger.debug("Duplicate event_id detected: $eventId, suppressing notification")
                return
            }
            deduplicationCache.add(eventId)
        }

        // Notify delegate
        val isInForeground = pushPlatform.isAppInForeground()
        pushPlatform.onFcmMessageReceived(message, parsed, isInForeground)

        // Display notification if not in foreground or if it has notification payload
        val shouldDisplay = !isInForeground || message.notification != null
        if (shouldDisplay && (parsed.title != null || parsed.body != null)) {
            val handler = NotificationHandler(applicationContext)
            handler.displayNotification(parsed)
        }
    }
}
