package com.pushplatform.sdk

import android.content.Context
import org.json.JSONObject

/** Non-secret bootstrap settings. Provider credentials never belong in an application. */
internal class RuntimeConfigurationStore(context: Context) {
    private val prefs = context.getSharedPreferences("com.pushplatform.bootstrap", Context.MODE_PRIVATE)

    fun save(config: PushConfiguration) {
        val appId = config.applicationId ?: return
        val value = JSONObject().put("applicationId", appId)
            .put("url", config.apiBaseUrl ?: PushPlatform.DEFAULT_API_BASE_URL)
            .put("environment", config.environment.name)
            .put("reportedEnvironment", config.reportedEnvironment ?: JSONObject.NULL)
            .put("foregroundBannerFallback", config.foregroundBannerFallbackSeconds)
        check(prefs.edit().putString("configuration", value.toString()).commit()) { "Unable to persist SDK configuration" }
    }

    fun load(): PushConfiguration? = runCatching {
        val value = JSONObject(prefs.getString("configuration", null) ?: return null)
        PushConfiguration(apiKey = "", applicationId = value.getString("applicationId"),
            apiBaseUrl = value.getString("url"), environment = Environment.valueOf(value.getString("environment")),
            reportedEnvironment = if (value.isNull("reportedEnvironment")) null else value.getString("reportedEnvironment"),
            foregroundBannerFallbackSeconds = value.optDouble("foregroundBannerFallback", 3.0))
    }.getOrNull()
}
