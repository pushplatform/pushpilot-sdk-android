package com.pushplatform.sdk.core

import com.pushplatform.sdk.utils.Logger

class FcmTokenManager(
    private val storage: SecureStorage,
    private val tokenRegistry: TokenRegistry
) {

    fun saveToken(token: String) {
        val previousToken = storage.getString(KEY_FCM_TOKEN)

        if (token != previousToken) {
            Logger.debug("FCM token changed: ${Logger.tokenMasked(token)}")
            storage.putString(KEY_FCM_TOKEN, token)
        } else {
            Logger.debug("FCM token unchanged")
        }
    }

    fun getToken(): String? {
        return storage.getString(KEY_FCM_TOKEN)
    }

    fun registerToken(
        installationId: String,
        token: String,
        bundleId: String,
        callback: (TokenRegistry.Result<Unit>) -> Unit
    ) {
        tokenRegistry.registerToken(
            installationId = installationId,
            provider = "fcm",
            token = token,
            bundleId = bundleId,
            environment = "production",
            callback = callback
        )
    }

    fun clearToken() {
        storage.remove(KEY_FCM_TOKEN)
    }

    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
