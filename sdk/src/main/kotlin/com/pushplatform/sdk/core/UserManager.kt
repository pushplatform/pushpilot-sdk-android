package com.pushplatform.sdk.core

import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.models.UserUpdate
import com.pushplatform.sdk.utils.Logger
import kotlin.math.pow

class UserManager(
    private val apiClient: ApiClient,
    private val installationManager: InstallationManager
) {

    private val maxRetries = 5
    private val baseDelayMs = 1000L
    private val maxDelayMs = 60000L

    fun login(userId: String, callback: (Result<Unit>) -> Unit) {
        val installationId = installationManager.getBackendInstallationId()

        if (installationId == null) {
            Logger.error("Cannot login: SDK not configured or installation ID missing")
            callback(Result.Failure(SdkError.NotConfigured))
            return
        }

        updateUserWithRetry(installationId, userId, 0, callback)
    }

    fun logout(callback: (Result<Unit>) -> Unit) {
        val installationId = installationManager.getBackendInstallationId()

        if (installationId == null) {
            Logger.error("Cannot logout: SDK not configured or installation ID missing")
            callback(Result.Failure(SdkError.NotConfigured))
            return
        }

        updateUserWithRetry(installationId, null, 0, callback)
    }

    private fun updateUserWithRetry(
        installationId: String,
        userId: String?,
        attempt: Int,
        callback: (Result<Unit>) -> Unit
    ) {
        val request: (ApiClient.Result<Unit>) -> Unit = { result ->
            when (result) {
                is ApiClient.Result.Success -> {
                    val action = if (userId != null) "Login" else "Logout"
                    Logger.debug("$action successful on attempt ${attempt + 1}")
                    callback(Result.Success(Unit))
                }
                is ApiClient.Result.Failure -> {
                    val error = result.error
                    val shouldRetry = shouldRetry(error, attempt)

                    if (shouldRetry) {
                        val delay = calculateDelay(attempt)
                        val action = if (userId != null) "login" else "logout"
                        Logger.debug("Retrying $action in ${delay}ms (attempt ${attempt + 1}/$maxRetries)")

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            updateUserWithRetry(installationId, userId, attempt + 1, callback)
                        }, delay)
                    } else {
                        val action = if (userId != null) "Login" else "Logout"
                        Logger.error("$action failed permanently: ${error.message}")
                        callback(Result.Failure(error))
                    }
                }
            }
        }
        if (userId != null) {
            apiClient.loginUser(installationId, userId, request)
        } else {
            apiClient.logoutUser(installationId, request)
        }
    }

    private fun shouldRetry(error: SdkError, attempt: Int): Boolean {
        if (attempt >= maxRetries) {
            return false
        }

        return when (error) {
            is SdkError.NetworkError -> true
            is SdkError.ApiError -> {
                val statusCode = error.statusCode
                when {
                    statusCode == 429 -> true  // Rate limit
                    statusCode in 500..599 -> true  // Server errors
                    statusCode == 401 -> false  // Unauthorized - bad API key
                    statusCode == 400 -> false  // Bad request
                    else -> false
                }
            }
            is SdkError.NotConfigured -> false
            else -> false
        }
    }

    private fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = (baseDelayMs * 2.0.pow(attempt)).toLong()
        return minOf(exponentialDelay, maxDelayMs)
    }

    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val error: SdkError) : Result<Nothing>()
    }
}
