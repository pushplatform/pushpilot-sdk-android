package com.pushplatform.sdk.models

data class Installation(
    val installationId: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String,
    val sdkVersion: String,
    val locale: String,
    val timezone: String,
    val manufacturer: String,
    val model: String
)

data class Subscription(
    val provider: String,
    val environment: String,
    val token: String,
    val bundleId: String
)

data class UserUpdate(
    val externalUserId: String?
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
