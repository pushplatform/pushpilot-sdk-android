package com.pushplatform.sdk.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pushplatform.sdk.utils.Logger
import java.net.URL

class NotificationHandler(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun displayNotification(
        parsed: ParsedNotification,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val channelId = parsed.channelId ?: NotificationChannelManager.CHANNEL_ID_DEFAULT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(parsed.title ?: "")
            .setContentText(parsed.body ?: "")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Set large icon (image)
        parsed.imageUrl?.let { imageUrl ->
            try {
                val bitmap = downloadImage(imageUrl)
                bitmap?.let {
                    builder.setLargeIcon(it)
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(it)
                            .bigLargeIcon(null as Bitmap?)
                    )
                }
            } catch (e: Exception) {
                Logger.error("Failed to download notification image: ${e.message}")
            }
        }

        // Set content intent (deep link or default)
        val contentIntent = createContentIntent(parsed)
        builder.setContentIntent(contentIntent)

        // Set tag if provided
        val tag = parsed.tag

        notificationManager.notify(tag, notificationId, builder.build())
        Logger.debug("Notification displayed: id=$notificationId, tag=$tag")
    }

    private fun createContentIntent(parsed: ParsedNotification): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()

        // Add deep link data if present
        parsed.customData["deep_link"]?.let { deepLink ->
            intent.action = Intent.ACTION_VIEW
            intent.data = android.net.Uri.parse(deepLink)
        }

        // Add notification data as extras
        intent.putExtra("event_id", parsed.eventId)
        intent.putExtra("call_id", parsed.callId)
        parsed.customData.forEach { (key, value) ->
            intent.putExtra(key, value)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun downloadImage(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Logger.error("Image download failed: ${e.message}")
            null
        }
    }
}
