package com.pushplatform.sdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.TimeZone

internal object DeviceMetadata {
    fun collect(context: Context): Map<String, String> = mapOf(
        "device_kind" to classify(Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER, Build.BRAND, Build.DEVICE, Build.HARDWARE),
        "locale" to Locale.getDefault().toLanguageTag(),
        "timezone" to TimeZone.getDefault().id,
        "notification_permission" to permission(context)
    )

    // Android exposes no tamper-proof emulator attestation. These are local OS
    // signals; a rooted/custom environment can spoof them. Missing data is unknown.
    internal fun classify(fingerprint: String?, model: String?, manufacturer: String?, brand: String?, device: String?, hardware: String?): String {
        val values = listOf(fingerprint, model, manufacturer, brand, device, hardware).map { it.orEmpty().lowercase(Locale.ROOT) }
        val fp=values[0]; val md=values[1]; val maker=values[2]; val br=values[3]; val dv=values[4]; val hw=values[5]
        if (fp.startsWith("generic") || fp.contains("emulator") || fp.contains("vbox") ||
            md.contains("sdk_gphone") || md.contains("google_sdk") || md.contains("emulator") ||
            maker.contains("genymotion") || hw in listOf("goldfish", "ranchu", "vbox86") ||
            (br.startsWith("generic") && dv.startsWith("generic"))) return "emulator"
        if (values.any { it.isBlank() || it == "unknown" }) return "unknown"
        return "physical"
    }

    fun permission(context: Context): String = try {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT < 33) {
            if (enabled) "granted" else "denied"
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            if (enabled) "granted" else "denied"
        } else {
            // Android's public permission API reports granted/not granted;
            // it does not reliably expose whether the prompt was shown before.
            "denied"
        }
    } catch (_: Exception) { "unknown" }
}
