package com.pushplatform.sdk.notifications

import android.content.Context
import android.content.Intent

/** Asks the launcher to show an absolute count. Some launchers only count visible notifications. */
object BadgeCount {
    fun apply(context: Context, count: Int) {
        val component = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component ?: return
        val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
            .putExtra("badge_count", count)
            .putExtra("badge_count_package_name", context.packageName)
            .putExtra("badge_count_class_name", component.className)
        context.sendBroadcast(intent)
    }
}
