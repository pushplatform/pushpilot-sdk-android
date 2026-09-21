package com.pushplatform.sdk

enum class Environment {
    DEVELOPMENT,
    PRODUCTION
}

data class PushConfiguration(
    val apiKey: String,
    val environment: Environment = Environment.PRODUCTION,
    val debugMode: Boolean = false,
    val apiBaseUrl: String? = null,
    val applicationId: String? = null
)
