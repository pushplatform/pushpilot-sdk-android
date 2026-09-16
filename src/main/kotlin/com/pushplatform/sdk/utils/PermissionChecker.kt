package com.pushplatform.sdk.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionChecker(private val context: Context) {

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Logger.debug("Notification permission not required on API < 33")
            return
        }

        if (hasNotificationPermission()) {
            Logger.debug("Notification permission already granted")
            return
        }

        Logger.debug("Requesting notification permission")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            requestCode
        )
    }

    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }

    companion object {
        const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001
    }
}
