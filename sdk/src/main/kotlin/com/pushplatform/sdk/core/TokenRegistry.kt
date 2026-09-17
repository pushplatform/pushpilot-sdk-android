package com.pushplatform.sdk.core

import com.pushplatform.sdk.models.Subscription
import com.pushplatform.sdk.utils.Logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class TokenRegistry(
    private val apiClient: ApiClient,
    private val storage: SecureStorage
) {

    private val maxRetries = 5
    private val baseDelayMs = 1000L
    private val maxDelayMs = 60000L

    fun registerToken(
        installationId: String,
        provider: String,
        token: String,
        bundleId: String,
        environment: String = "production",
        callback: (Result<Unit>) -> Unit
    ) {
        val subscription = Subscription(
            provider = provider,
            environment = environment,
            token = token,
            bundleId = bundleId
        )

        registerWithRetry(installationId, subscription, 0, callback)
    }

    private fun registerWithRetry(
        installationId: String,
        subscription: Subscription,
        attempt: Int,
        callback: (Result<Unit>) -> Unit
    ) {
        apiClient.createSubscription(installationId, subscription) { result ->
            when (result) {
                is ApiClient.Result.Success -> {
                    Logger.debug("Token registered successfully on attempt ${attempt + 1}")
                    callback(Result.Success(Unit))
                }
                is ApiClient.Result.Failure -> {
                    val error = result.error
                    val shouldRetry = shouldRetry(error, attempt)

                    if (shouldRetry) {
                        val delay = calculateDelay(attempt)
                        Logger.debug("Retrying token registration in ${delay}ms (attempt ${attempt + 1}/$maxRetries)")

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            registerWithRetry(installationId, subscription, attempt + 1, callback)
                        }, delay)
                    } else {
                        Logger.error("Token registration failed permanently: ${error.message}")
                        callback(Result.Failure(error))
                    }
                }
            }
        }
    }

    private fun shouldRetry(error: com.pushplatform.sdk.models.SdkError, attempt: Int): Boolean {
        if (attempt >= maxRetries) {
            return false
        }

        return when (error) {
            is com.pushplatform.sdk.models.SdkError.NetworkError -> true
            is com.pushplatform.sdk.models.SdkError.ApiError -> {
                val statusCode = error.statusCode
                when {
                    statusCode == 429 -> true  // Rate limit
                    statusCode in 500..599 -> true  // Server errors
                    statusCode == 401 -> false  // Unauthorized - bad API key
                    statusCode == 400 -> false  // Bad request - won't fix with retry
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = (baseDelayMs * 2.0.pow(attempt)).toLong()
        return minOf(exponentialDelay, maxDelayMs)
    }

    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val error: com.pushplatform.sdk.models.SdkError) : Result<Nothing>()
    }
}
