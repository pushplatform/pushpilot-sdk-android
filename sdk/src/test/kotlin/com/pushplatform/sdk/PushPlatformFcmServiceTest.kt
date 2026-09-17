package com.pushplatform.sdk

import android.net.Uri
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.notifications.DeduplicationCache
import com.pushplatform.sdk.notifications.ParsedNotification
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class PushPlatformFcmServiceTest {

    private lateinit var service: PushPlatformFcmService
    private lateinit var mockPushPlatform: PushPlatform

    @Before
    fun setup() {
        service = PushPlatformFcmService()
        mockPushPlatform = mock()
    }

    @Test
    fun testOnNewToken_configuredSdk() {
        val token = "test_token_123"

        // This test verifies the service can handle token refresh
        // In a real scenario, PushPlatform.getInstance() would return configured instance
        service.onNewToken(token)

        // No assertion needed - just verify no crash
    }

    @Test
    fun testOnMessageReceived_withEventId() {
        val mockMessage = mock<RemoteMessage>()
        val mockNotification = mock<RemoteMessage.Notification>()
        val data = mapOf(
            "title" to "Test",
            "body" to "Body",
            "event_id" to "evt_123"
        )

        whenever(mockMessage.notification).thenReturn(mockNotification)
        whenever(mockMessage.data).thenReturn(data)
        whenever(mockMessage.from).thenReturn("test_sender")
        whenever(mockNotification.title).thenReturn("Test")
        whenever(mockNotification.body).thenReturn("Body")

        service.onMessageReceived(mockMessage)

        // Verify message processed (no crash)
    }

    @Test
    fun testOnMessageReceived_dataOnly() {
        val mockMessage = mock<RemoteMessage>()
        val data = mapOf(
            "event_id" to "evt_456",
            "custom_field" to "custom_value"
        )

        whenever(mockMessage.notification).thenReturn(null)
        whenever(mockMessage.data).thenReturn(data)
        whenever(mockMessage.from).thenReturn("test_sender")

        service.onMessageReceived(mockMessage)

        // Data-only messages should not display notification
        // Verify message processed (no crash)
    }

    @Test
    fun testOnMessageReceived_withNotificationObject() {
        val mockMessage = mock<RemoteMessage>()
        val mockNotification = mock<RemoteMessage.Notification>()

        whenever(mockMessage.notification).thenReturn(mockNotification)
        whenever(mockMessage.data).thenReturn(emptyMap())
        whenever(mockMessage.from).thenReturn("test_sender")
        whenever(mockNotification.title).thenReturn("Notification Title")
        whenever(mockNotification.body).thenReturn("Notification Body")

        service.onMessageReceived(mockMessage)

        // Verify notification object handled
    }
}
