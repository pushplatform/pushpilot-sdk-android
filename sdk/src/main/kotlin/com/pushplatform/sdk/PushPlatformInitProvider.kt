package com.pushplatform.sdk

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.pushplatform.sdk.notifications.ParsedNotification

/** Starts click delivery without a custom Application class. */
class PushPlatformInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val application = context?.applicationContext as? Application ?: return true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = deliver(activity.intent)
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) = deliver(activity.intent)
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        return true
    }

    private fun deliver(intent: Intent?) {
        if (intent == null || !intent.getBooleanExtra(MARKER, false)) return
        intent.removeExtra(MARKER)
        val data = intent.extras?.keySet().orEmpty()
            .filter { it != MARKER && it != "title" && it != "body" }
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
                customData = data
            )
        )
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        const val MARKER = "pushplatform_notification"
    }
}
