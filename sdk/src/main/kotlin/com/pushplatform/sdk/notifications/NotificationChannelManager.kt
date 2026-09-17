package com.pushplatform.sdk.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.pushplatform.sdk.utils.Logger

class NotificationChannelManager(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun createDefaultChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDefaultChannel()
            createHighPriorityChannel()
        } else {
            Logger.debug("Notification channels not supported on API < 26")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createDefaultChannel() {
        val channelId = CHANNEL_ID_DEFAULT
        val existing = notificationManager.getNotificationChannel(channelId)

        if (existing != null) {
            Logger.debug("Default notification channel already exists")
            return
        }

        val channel = NotificationChannel(
            channelId,
            "Push Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Default push notifications"
            enableVibration(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(channel)
        Logger.info("Created default notification channel: $channelId")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createHighPriorityChannel() {
        val channelId = CHANNEL_ID_HIGH_PRIORITY
        val existing = notificationManager.getNotificationChannel(channelId)

        if (existing != null) {
            Logger.debug("High-priority notification channel already exists")
            return
        }

        val channel = NotificationChannel(
            channelId,
            "High Priority Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority call notifications"
            enableVibration(true)
            setShowBadge(true)

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            setSound(ringtoneUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }

        notificationManager.createNotificationChannel(channel)
        Logger.info("Created high-priority notification channel: $channelId")
    }

    fun channelExists(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }

        return notificationManager.getNotificationChannel(channelId) != null
    }

    companion object {
        const val CHANNEL_ID_DEFAULT = "push_notifications"
        const val CHANNEL_ID_HIGH_PRIORITY = "high_priority_calls"
    }
}
