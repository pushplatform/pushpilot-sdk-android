package com.pushplatform.sdk.core

import com.pushplatform.sdk.models.SdkError

/** Joins asynchronous installation creation and Firebase token acquisition for one configuration. */
internal class FcmRegistration(
    private val manager: FcmTokenManager,
    private val bundleId: String,
    private val isCurrent: () -> Boolean,
    private val updated: () -> Unit,
    private val failed: (SdkError) -> Unit
) {
    private var installationId: String? = null
    private var token: String? = null
    private var registeredToken: String? = null
    private var inFlight = false
    private var refreshVersion = 0L

    fun installationReady(id: String) {
        synchronized(this) {
            if (!isCurrent()) return
            installationId = id
        }
        registerLatest()
    }

    fun refreshed(value: String) {
        synchronized(this) {
            if (!isCurrent()) return
            refreshVersion++
            if (!accept(value)) return
        }
        registerLatest()
    }

    @Synchronized fun fetchVersion(): Long = refreshVersion

    fun fetched(version: Long, result: Result<String>) {
        var error: SdkError? = null
        synchronized(this) {
            // A later onNewToken callback is newer than this in-flight Firebase lookup.
            if (!isCurrent() || version != refreshVersion) return
            result.fold(
                onSuccess = { if (!accept(it)) error = SdkError.InvalidToken },
                onFailure = { if (token == null) error = SdkError.NetworkError(it) }
            )
        }
        error?.let { failed(it) }
        registerLatest()
    }

    private fun accept(value: String): Boolean {
        if (value.isBlank()) return false
        token = value
        manager.saveToken(value)
        return true
    }

    private fun registerLatest() {
        val request = synchronized(this) {
            val id = installationId ?: return
            val value = token ?: return
            if (!isCurrent() || inFlight || value == registeredToken) return
            inFlight = true
            Pair(id, value)
        }
        manager.registerToken(request.first, request.second, bundleId) { result ->
            var notify = false
            var error: SdkError? = null
            val newerToken = synchronized(this) {
                inFlight = false
                if (!isCurrent()) return@registerToken
                when (result) {
                    is TokenRegistry.Result.Success -> {
                        registeredToken = request.second
                        notify = token == request.second
                    }
                    is TokenRegistry.Result.Failure -> if (token == request.second) error = result.error
                }
                token != request.second
            }
            // Host callbacks may reconfigure the SDK; do not invoke them under our state lock.
            if (notify) updated()
            error?.let { failed(it) }
            // Serialize rotations: an old request/retry cannot overwrite a newer token.
            if (newerToken) registerLatest()
        }
    }
}
