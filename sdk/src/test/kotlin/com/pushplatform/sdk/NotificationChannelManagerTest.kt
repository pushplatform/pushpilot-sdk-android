package com.pushplatform.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.pushplatform.sdk.notifications.NotificationChannelManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class NotificationChannelManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var channelManager: NotificationChannelManager

    @Before
    fun setup() {
        mockContext = mock()
        mockNotificationManager = mock()

        whenever(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)

        channelManager = NotificationChannelManager(mockContext)
    }

    @Test
    fun testCreateDefaultChannels_createsDefaultChannel() {
        whenever(mockNotificationManager.getNotificationChannel(any())).thenReturn(null)

        channelManager.createDefaultChannels()

        verify(mockNotificationManager).createNotificationChannel(argThat { channel ->
            channel.id == NotificationChannelManager.CHANNEL_ID_DEFAULT &&
            channel.importance == NotificationManager.IMPORTANCE_DEFAULT
        })
    }

    @Test
    fun testCreateDefaultChannels_createsHighPriorityChannel() {
        whenever(mockNotificationManager.getNotificationChannel(any())).thenReturn(null)

        channelManager.createDefaultChannels()

        verify(mockNotificationManager).createNotificationChannel(argThat { channel ->
            channel.id == NotificationChannelManager.CHANNEL_ID_HIGH_PRIORITY &&
            channel.importance == NotificationManager.IMPORTANCE_HIGH
        })
    }

    @Test
    fun testCreateDefaultChannels_idempotent() {
        val existingChannel = mock<NotificationChannel>()
        whenever(mockNotificationManager.getNotificationChannel(any())).thenReturn(existingChannel)

        channelManager.createDefaultChannels()

        verify(mockNotificationManager, never()).createNotificationChannel(any())
    }

    @Test
    fun testChannelExists_returnsTrue() {
        val existingChannel = mock<NotificationChannel>()
        whenever(mockNotificationManager.getNotificationChannel("test_channel"))
            .thenReturn(existingChannel)

        val exists = channelManager.channelExists("test_channel")

        assertTrue(exists)
    }

    @Test
    fun testChannelExists_returnsFalse() {
        whenever(mockNotificationManager.getNotificationChannel("test_channel"))
            .thenReturn(null)

        val exists = channelManager.channelExists("test_channel")

        assertFalse(exists)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
class NotificationChannelManagerPreOreoTest {

    private lateinit var mockContext: Context
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var channelManager: NotificationChannelManager

    @Before
    fun setup() {
        mockContext = mock()
        mockNotificationManager = mock()

        whenever(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)

        channelManager = NotificationChannelManager(mockContext)
    }

    @Test
    fun testCreateDefaultChannels_preOreo_noOp() {
        // Should not throw exception on pre-Oreo devices
        channelManager.createDefaultChannels()
        // Cannot verify createNotificationChannel was not called because
        // the method doesn't exist on API < 26
    }

    @Test
    fun testChannelExists_preOreo_alwaysTrue() {
        val exists = channelManager.channelExists("any_channel")

        assertTrue(exists)
    }
}
