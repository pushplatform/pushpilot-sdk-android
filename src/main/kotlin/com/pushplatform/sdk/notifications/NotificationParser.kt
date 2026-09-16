package com.pushplatform.sdk.notifications

import com.google.firebase.messaging.RemoteMessage

data class ParsedNotification(
    val title: String?,
    val body: String?,
    val imageUrl: String?,
    val channelId: String?,
    val tag: String?,
    val eventId: String?,
    val callId: String?,
    val customData: Map<String, String>
)

class NotificationParser {

    fun parse(message: RemoteMessage): ParsedNotification {
        val notification = message.notification
        val data = message.data

        val title = notification?.title ?: data["title"]
        val body = notification?.body ?: data["body"]
        val imageUrl = notification?.imageUrl?.toString() ?: data["image"]

        val channelId = data["channel_id"]
        val tag = data["tag"]
        val eventId = data["event_id"]
        val callId = data["call_id"]

        val customData = extractCustomData(data)

        return ParsedNotification(
            title = title,
            body = body,
            imageUrl = imageUrl,
            channelId = channelId,
            tag = tag,
            eventId = eventId,
            callId = callId,
            customData = customData
        )
    }

    private fun extractCustomData(data: Map<String, String>): Map<String, String> {
        val reservedKeys = setOf(
            "title", "body", "image",
            "channel_id", "tag", "event_id", "call_id"
        )

        return data.filterKeys { key ->
            !reservedKeys.contains(key) && !key.startsWith("gcm.") && !key.startsWith("google.")
        }
    }
}
