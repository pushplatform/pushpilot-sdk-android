package com.pushplatform.sdk.core

import com.pushplatform.sdk.models.SdkError
import java.util.UUID
import kotlin.math.pow

/** Account changes are serialized; an old retry cannot re-bind an installation after logout. */
class UserManager(private val apiClient: ApiClient, private val installationManager: InstallationManager) {
    private data class Operation(
        val id: String = UUID.randomUUID().toString(),
        val userId: String?, val metadata: Map<String, Any?>?,
        var callback: ((Result<Unit>) -> Unit)?, var attempt: Int = 0
    )
    private val lock = Any()
    private var desired: Operation? = null
    private var inFlightId: String? = null

    fun login(userId: String, metadata: Map<String, Any?>? = null, callback: (Result<Unit>) -> Unit) = enqueue(userId, metadata, callback)
    fun logout(callback: (Result<Unit>) -> Unit) = enqueue(null, null, callback)

    private fun enqueue(userId: String?, metadata: Map<String, Any?>?, callback: (Result<Unit>) -> Unit) {
        if (installationManager.getBackendInstallationId() == null) { callback(Result.Failure(SdkError.NotConfigured)); return }
        val cancelled = synchronized(lock) {
            val previous = desired
            desired = Operation(userId = userId, metadata = metadata, callback = callback)
            if (previous?.id != inFlightId) previous?.callback else null
        }
        cancelled?.invoke(Result.Failure(SdkError.ApiError(409, "Account operation superseded")))
        send()
    }

    fun retryPending() {
        synchronized(lock) { if (inFlightId != null) return; desired?.attempt = 0 }
        send()
    }

    private fun send() {
        val installationId = installationManager.getBackendInstallationId() ?: return
        val operation = synchronized(lock) {
            if (inFlightId != null) return
            val value = desired ?: return
            inFlightId = value.id
            value.copy()
        }
        val finished: (ApiClient.Result<Unit>) -> Unit = { response ->
            var callback: ((Result<Unit>) -> Unit)? = null
            var outcome: Result<Unit>? = null
            var delay: Long? = null
            var superseded = false
            synchronized(lock) {
                inFlightId = null
                val latest = desired
                if (latest?.id != operation.id) {
                    superseded = true
                    callback = operation.callback
                    outcome = Result.Failure(SdkError.ApiError(409, "Account operation superseded"))
                } else when (response) {
                    is ApiClient.Result.Success -> { desired = null; callback = latest.callback; outcome = Result.Success(Unit) }
                    is ApiClient.Result.Failure -> {
                        val error = response.error
                        if (isRetryable(error) && latest.attempt < 5) {
                            delay = (1000 * 2.0.pow(latest.attempt++)).toLong()
                        } else {
                            callback = latest.callback
                            latest.callback = null
                            outcome = Result.Failure(error)
                            if (!isRetryable(error)) desired = null
                        }
                    }
                }
            }
            outcome?.let { callback?.invoke(it) }
            if (superseded) send()
            delay?.let { wait -> android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val current = synchronized(lock) { desired?.id == operation.id }
                if (current) send()
            }, wait) }
        }
        if (operation.userId != null) apiClient.loginUser(installationId, operation.userId, operation.metadata, finished)
        else apiClient.logoutUser(installationId, finished)
    }

    private fun isRetryable(error: SdkError) = when(error) {
        is SdkError.NetworkError -> true
        is SdkError.ApiError -> error.statusCode == 429 || error.statusCode >= 500
        else -> false
    }

    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val error: SdkError) : Result<Nothing>()
    }
}
