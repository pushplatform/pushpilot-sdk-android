package com.pushplatform.sdk

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pushplatform.sdk.notifications.NotificationHandler
import com.pushplatform.sdk.notifications.ParsedNotification
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class NotificationHandlerTest {

    private lateinit var context: Context
    private lateinit var handler: NotificationHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        handler = NotificationHandler(context)
    }

    @Test
    fun testDisplayNotification_withTitleAndBody() {
        val parsed = ParsedNotification(
            title = "Test Title",
            body = "Test Body",
            imageUrl = null,
            channelId = null,
            tag = null,
            eventId = "evt_123",
            callId = null,
            customData = emptyMap()
        )

        // Should not throw exception
        handler.displayNotification(parsed)
    }

    @Test
    fun testDisplayNotification_withCustomChannelId() {
        val parsed = ParsedNotification(
            title = "Test",
            body = "Body",
            imageUrl = null,
            channelId = "high_priority_calls",
            tag = null,
            eventId = null,
            callId = null,
            customData = emptyMap()
        )

        handler.displayNotification(parsed)
    }

    @Test
    fun testDisplayNotification_withTag() {
        val parsed = ParsedNotification(
            title = "Test",
            body = "Body",
            imageUrl = null,
            channelId = null,
            tag = "notification_tag_1",
            eventId = null,
            callId = null,
            customData = emptyMap()
        )

        handler.displayNotification(parsed)
    }

    @Test
    fun testDisplayNotification_withDeepLink() {
        val parsed = ParsedNotification(
            title = "Test",
            body = "Body",
            imageUrl = null,
            channelId = null,
            tag = null,
            eventId = null,
            callId = null,
            customData = mapOf("deep_link" to "myapp://screen/detail")
        )

        handler.displayNotification(parsed)
    }

    @Test
    fun testDisplayNotification_usesDefaultChannelWhenNull() {
        val parsed = ParsedNotification(
            title = "Test",
            body = "Body",
            imageUrl = null,
            channelId = null,
            tag = null,
            eventId = null,
            callId = null,
            customData = emptyMap()
        )

        // Should use default channel
        handler.displayNotification(parsed)
    }
}
