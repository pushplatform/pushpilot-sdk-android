package com.pushplatform.sdk.core

import android.os.Build
import com.pushplatform.sdk.models.*
import com.pushplatform.sdk.utils.Logger
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val apiKey: String, private val baseUrl: String = "https://api.pushplatform.com") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    fun createInstallation(
        installation: Installation,
        callback: (Result<Unit>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("installation_id", installation.installationId)
            put("platform", installation.platform)
            put("os_version", installation.osVersion)
            put("app_version", installation.appVersion)
            put("sdk_version", installation.sdkVersion)
            put("locale", installation.locale)
            put("timezone", installation.timezone)
            put("manufacturer", installation.manufacturer)
            put("model", installation.model)
        }

        val request = buildRequest("/v1/installations", json.toString())

        executeRequest(request) { result ->
            callback(result.map { Unit })
        }
    }

    fun registerInstallation(
        applicationId: String,
        deviceId: String,
        environment: String,
        osVersion: String,
        appVersion: String,
        deviceModel: String,
        callback: (Result<String>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("application_id", applicationId)
            put("device_id", deviceId)
            put("platform", "android")
            put("environment", environment)
            put("os_version", osVersion)
            put("app_version", appVersion)
            put("sdk_version", "1.0.0")
            put("device_model", deviceModel)
        }
        executeRequest(buildRequest("/v1/installations", json.toString())) { result ->
            when (result) {
                is Result.Success -> {
                    val installationId = runCatching { JSONObject(result.value).getString("id") }
                    installationId.onSuccess { callback(Result.Success(it)) }
                        .onFailure { callback(Result.Failure(SdkError.ApiError(201, "Invalid installation response"))) }
                }
                is Result.Failure -> callback(result)
            }
        }
    }

    fun loginUser(installationId: String, userId: String, callback: (Result<Unit>) -> Unit) {
        val json = JSONObject().put("external_user_id", userId)
        executeRequest(buildRequest("/v1/installations/$installationId/login", json.toString())) { result ->
            callback(result.map { Unit })
        }
    }

    fun logoutUser(installationId: String, callback: (Result<Unit>) -> Unit) {
        executeRequest(buildRequest("/v1/installations/$installationId/logout", "")) { result ->
            callback(result.map { Unit })
        }
    }

    fun updateInstallation(
        installationId: String,
        userUpdate: UserUpdate,
        callback: (Result<Unit>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("external_user_id", userUpdate.externalUserId)
        }

        val request = buildRequest("/v1/installations/$installationId", json.toString(), method = "PATCH")

        executeRequest(request) { result ->
            callback(result.map { Unit })
        }
    }

    fun createSubscription(
        installationId: String,
        subscription: Subscription,
        callback: (Result<Unit>) -> Unit
    ) {
        val json = JSONObject().apply {
            put("provider", subscription.provider)
            put("environment", subscription.environment)
            put("token", subscription.token)
            put("bundle_id", subscription.bundleId)
        }

        val request = buildRequest("/v1/installations/$installationId/subscriptions", json.toString())

        executeRequest(request) { result ->
            callback(result.map { Unit })
        }
    }

    private fun buildRequest(path: String, body: String, method: String = "POST"): Request {
        val requestBody = body.toRequestBody(mediaTypeJson)

        return Request.Builder()
            .url("$baseUrl$path")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "PushPlatform-Android-SDK/1.0.0")
            .method(method, if (method == "POST" || method == "PATCH") requestBody else null)
            .build()
    }

    private fun executeRequest(
        request: Request,
        callback: (Result<String>) -> Unit
    ) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.error("API request failed", e)
                callback(Result.Failure(SdkError.NetworkError(e)))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    when (response.code) {
                        in 200..299 -> {
                            val body = response.body?.string() ?: ""
                            Logger.debug("API response: ${response.code}")
                            callback(Result.Success(body))
                        }
                        in 400..499 -> {
                            val errorBody = response.body?.string() ?: "Client error"
                            Logger.error("API client error: ${response.code} - $errorBody")
                            callback(Result.Failure(SdkError.ApiError(response.code, errorBody)))
                        }
                        in 500..599 -> {
                            val errorBody = response.body?.string() ?: "Server error"
                            Logger.error("API server error: ${response.code} - $errorBody")
                            callback(Result.Failure(SdkError.ApiError(response.code, errorBody)))
                        }
                        else -> {
                            Logger.error("API unexpected status: ${response.code}")
                            callback(Result.Failure(SdkError.ApiError(response.code, "Unexpected status code")))
                        }
                    }
                }
            }
        })
    }

    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val error: SdkError) : Result<Nothing>()

        fun <R> map(transform: (T) -> R): Result<R> {
            return when (this) {
                is Success -> Success(transform(value))
                is Failure -> this
            }
        }

        fun isSuccess(): Boolean = this is Success
        fun isFailure(): Boolean = this is Failure
    }
}
