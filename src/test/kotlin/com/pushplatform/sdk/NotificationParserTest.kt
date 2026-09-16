package com.pushplatform.sdk

import android.net.Uri
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.notifications.NotificationParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class NotificationParserTest {

    private lateinit var parser: NotificationParser

    @Before
    fun setup() {
        parser = NotificationParser()
    }

    @Test
    fun testParse_notificationObject() {
        val mockMessage = mock<RemoteMessage>()
        val mockNotification = mock<RemoteMessage.Notification>()

        whenever(mockMessage.notification).thenReturn(mockNotification)
        whenever(mockMessage.data).thenReturn(emptyMap())
        whenever(mockNotification.title).thenReturn("Test Title")
        whenever(mockNotification.body).thenReturn("Test Body")
        whenever(mockNotification.imageUrl).thenReturn(Uri.parse("https://example.com/image.png"))

        val parsed = parser.parse(mockMessage)

        assertEquals("Test Title", parsed.title)
        assertEquals("Test Body", parsed.body)
        assertEquals("https://example.com/image.png", parsed.imageUrl)
    }

    @Test
    fun testParse_dataPayload() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "title" to "Data Title",
            "body" to "Data Body",
            "image" to "https://example.com/data.png"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)

        val parsed = parser.parse(mockMessage)

        assertEquals("Data Title", parsed.title)
        assertEquals("Data Body", parsed.body)
        assertEquals("https://example.com/data.png", parsed.imageUrl)
    }

    @Test
    fun testParse_eventIdAndCallId() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "event_id" to "evt_123",
            "call_id" to "call_456"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)

        val parsed = parser.parse(mockMessage)

        assertEquals("evt_123", parsed.eventId)
        assertEquals("call_456", parsed.callId)
    }

    @Test
    fun testParse_channelIdAndTag() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "channel_id" to "high_priority_calls",
            "tag" to "notification_tag_1"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)

        val parsed = parser.parse(mockMessage)

        assertEquals("high_priority_calls", parsed.channelId)
        assertEquals("notification_tag_1", parsed.tag)
    }

    @Test
    fun testParse_customData() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "title" to "Title",
            "custom_field_1" to "value1",
            "custom_field_2" to "value2"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)

        val parsed = parser.parse(mockMessage)

        assertEquals(2, parsed.customData.size)
        assertEquals("value1", parsed.customData["custom_field_1"])
        assertEquals("value2", parsed.customData["custom_field_2"])
    }

    @Test
    fun testParse_customDataFiltersReservedKeys() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "title" to "Title",
            "body" to "Body",
            "event_id" to "evt_123",
            "custom_field" to "custom_value"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)

        val parsed = parser.parse(mockMessage)

        assertEquals(1, parsed.customData.size)
        assertEquals("custom_value", parsed.customData["custom_field"])
        assertFalse(parsed.customData.containsKey("title"))
        assertFalse(parsed.customData.containsKey("body"))
        assertFalse(parsed.customData.containsKey("event_id"))
    }

    @Test
    fun testParse_customDataFiltersGcmKeys() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "gcm.message_id" to "12345",
            "google.sent_time" to "67890",
            "custom_field" to "custom_value"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)

        val parsed = parser.parse(mockMessage)

        assertEquals(1, parsed.customData.size)
        assertEquals("custom_value", parsed.customData["custom_field"])
        assertFalse(parsed.customData.containsKey("gcm.message_id"))
        assertFalse(parsed.customData.containsKey("google.sent_time"))
    }

    @Test
    fun testParse_emptyData() {
        val mockMessage = mock<RemoteMessage>()

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(emptyMap())

        val parsed = parser.parse(mockMessage)

        assertNull(parsed.title)
        assertNull(parsed.body)
        assertNull(parsed.imageUrl)
        assertNull(parsed.eventId)
        assertNull(parsed.callId)
        assertTrue(parsed.customData.isEmpty())
    }

    @Test
    fun testParse_notificationObjectOverridesData() {
        val mockMessage = mock<RemoteMessage>()
        val mockNotification = mock<RemoteMessage.Notification>()
        val data = mapOf(
            "title" to "Data Title",
            "body" to "Data Body"
        )

        whenever(mockMessage.notification).thenReturn(mockNotification)
        whenever(mockMessage.data).thenReturn(data)
        whenever(mockNotification.title).thenReturn("Notification Title")
        whenever(mockNotification.body).thenReturn("Notification Body")

        val parsed = parser.parse(mockMessage)

        assertEquals("Notification Title", parsed.title)
        assertEquals("Notification Body", parsed.body)
    }
}
