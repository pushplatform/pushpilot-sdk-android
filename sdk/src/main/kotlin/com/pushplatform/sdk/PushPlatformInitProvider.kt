package com.pushplatform.sdk

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.pushplatform.sdk.notifications.NotificationParser
import com.pushplatform.sdk.notifications.ParsedNotification

/** Starts click delivery without a custom Application class. */
class PushPlatformInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val application = context?.applicationContext as? Application ?: return true
        PushPlatform.getInstance().restore(application)
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { deliver(activity.intent) }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) { deliver(activity.intent) }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    internal companion object {
        const val MARKER = "pushplatform_notification"
        private const val DELIVERED = "pushplatform_opened"

        /**
         * A click on the SDK's own notification carries MARKER. A notification drawn by
         * the system while the app was in background carries the FCM data as extras.
         */
        fun deliver(intent: Intent?): Boolean {
            if (intent == null) return false
            if (intent.getBooleanExtra(DELIVERED, false)) return true
            val ours = intent.getBooleanExtra(MARKER, false)
            val notificationId = intent.getStringExtra(NotificationParser.NOTIFICATION_ID)
            val fromFcm = notificationId != null && intent.hasExtra("google.message_id")
            if (!ours && !fromFcm) return false
            intent.removeExtra(MARKER)
            intent.putExtra(DELIVERED, true)
            val data = intent.extras?.keySet().orEmpty()
                .filter { it !in RESERVED && !it.startsWith("google.") && !it.startsWith("gcm.") }
                .associateWith { key -> intent.getStringExtra(key) ?: intent.extras?.get(key)?.toString().orEmpty() }
            PushPlatform.getInstance().onNotificationOpened(
                ParsedNotification(
                    title = intent.getStringExtra("title"),
                    body = intent.getStringExtra("body"),
                    imageUrl = null,
                    channelId = null,
                    tag = null,
                    eventId = intent.getStringExtra("event_id"),
                    callId = intent.getStringExtra("call_id"),
                    customData = data,
                    notificationId = notificationId
                )
            )
            return true
        }

        private val RESERVED = setOf(MARKER, DELIVERED, "title", "body", "from", "collapse_key", NotificationParser.NOTIFICATION_ID)
    }
}
