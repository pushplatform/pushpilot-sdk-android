package com.pushplatform.sdk

import android.os.Build
import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.models.Installation
import com.pushplatform.sdk.utils.Logger
import java.util.Locale
import java.util.TimeZone

internal object DeviceInfo {

    fun getOsVersion(): String = Build.VERSION.RELEASE

    fun getManufacturer(): String = Build.MANUFACTURER

    fun getModel(): String = Build.MODEL

    fun getLocale(): String {
        return try {
            Locale.getDefault().toString()
        } catch (e: Exception) {
            "en_US"
        }
    }

    fun getTimezone(): String {
        return try {
            TimeZone.getDefault().id
        } catch (e: Exception) {
            "UTC"
        }
    }

    fun createInstallation(
        installationId: String,
        appVersion: String = "1.0.0",
        sdkVersion: String = "1.0.0"
    ): Installation {
        return Installation(
            installationId = installationId,
            platform = "android",
            osVersion = getOsVersion(),
            appVersion = appVersion,
            sdkVersion = sdkVersion,
            locale = getLocale(),
            timezone = getTimezone(),
            manufacturer = getManufacturer(),
            model = getModel()
        )
    }
}
