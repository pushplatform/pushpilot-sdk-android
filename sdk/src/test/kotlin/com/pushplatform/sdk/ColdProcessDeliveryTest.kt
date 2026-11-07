package com.pushplatform.sdk

import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.pushplatform.sdk.notifications.NotificationParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ColdProcessDeliveryTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun reset() {
        PushPlatform::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        context.getSharedPreferences("com.pushplatform.bootstrap", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `cold process displays a recognized alert without waiting for JavaScript or network`() {
        val message = mock<RemoteMessage>()
        whenever(message.data).thenReturn(mapOf("notification_id" to "test-message", "title" to "Title", "body" to "Body", "opaque" to "value"))
        assertTrue(PushPlatform.getInstance().handleRemoteMessage(context, message))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notices = shadowOf(manager).allNotifications
        assertEquals(1, notices.size)
        assertEquals("Title", notices.single().extras.getString("android.title"))
        assertTrue(PushPlatform.getInstance().handleRemoteMessage(context, message))
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test fun `foreign FCM messages remain owned by the application`() {
        val message = mock<RemoteMessage>()
        whenever(message.data).thenReturn(mapOf("type" to "anything"))
        assertFalse(PushPlatform.getInstance().handleRemoteMessage(context, message))
    }

    @Test fun `host can handle its call UI while SDK processes transport`() {
        val message = mock<RemoteMessage>()
        whenever(message.data).thenReturn(mapOf("notification_id" to "call", "title" to "Caller", "callId" to "id"))
        assertTrue(PushPlatform.getInstance().handleRemoteMessage(context, message, displayNotification = false))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test fun `two notifications retain independent click intents and warm opens are delivered once`() {
        val sdk = PushPlatform.getInstance()
        for (id in listOf("first", "second")) {
            val message = mock<RemoteMessage>()
            whenever(message.data).thenReturn(mapOf("notification_id" to id, "title" to id))
            sdk.handleRemoteMessage(context, message)
        }
        val notices = shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).allNotifications
        assertEquals(2, notices.size)
        val intents = notices.map { shadowOf(it.contentIntent).savedIntent }
        assertEquals(setOf("first", "second"), intents.map { it.getStringExtra(NotificationParser.NOTIFICATION_ID) }.toSet())
        assertNotEquals(notices[0].contentIntent, notices[1].contentIntent)
        val opened = mutableListOf<String?>()
        sdk.delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) {}
            override fun didOpenNotification(notification: com.pushplatform.sdk.notifications.ParsedNotification) { opened.add(notification.notificationId) }
        }
        assertTrue(sdk.handleNotificationIntent(intents[0]))
        assertTrue(sdk.handleNotificationIntent(intents[0]))
        assertTrue(sdk.handleNotificationIntent(intents[1]))
        assertEquals(2, opened.size)
        assertEquals(setOf("first", "second"), opened.toSet())
        assertFalse(sdk.handleNotificationIntent(Intent("host-only")))
    }

    @Test fun `bootstrap settings persist separately from credentials and preserve production scope`() {
        val original = PushConfiguration(apiKey = "must-not-be-persisted", applicationId = "app", apiBaseUrl = "https://customer.example", reportedEnvironment = null)
        val store = RuntimeConfigurationStore(context)
        store.save(original)
        val restored = store.load()!!
        assertEquals("", restored.apiKey)
        assertEquals(original.applicationId, restored.applicationId)
        assertEquals(original.apiBaseUrl, restored.apiBaseUrl)
        assertNull(restored.reportedEnvironment)
    }
}
