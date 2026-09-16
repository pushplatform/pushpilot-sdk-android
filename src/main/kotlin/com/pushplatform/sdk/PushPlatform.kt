package com.pushplatform.sdk

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.core.FcmTokenManager
import com.pushplatform.sdk.core.InstallationManager
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.core.TokenRegistry
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.utils.Logger

class PushPlatform private constructor() {

    private var configuration: PushConfiguration? = null
    private var secureStorage: SecureStorage? = null
    private var installationManager: InstallationManager? = null
    private var apiClient: ApiClient? = null
    private var tokenRegistry: TokenRegistry? = null
    private var fcmTokenManager: FcmTokenManager? = null

    var delegate: PushPlatformDelegate? = null

    fun configure(
        context: Context,
        apiKey: String,
        environment: Environment = Environment.PRODUCTION,
        debugMode: Boolean = false
    ) {
        Logger.debugMode = debugMode
        Logger.debug("Configuring PushPlatform SDK")

        configuration = PushConfiguration(
            apiKey = apiKey,
            environment = environment,
            debugMode = debugMode
        )

        val baseUrl = when (environment) {
            Environment.DEVELOPMENT -> "https://api-dev.pushplatform.com"
            Environment.PRODUCTION -> "https://api.pushplatform.com"
        }

        secureStorage = SecureStorage(context.applicationContext)
        installationManager = InstallationManager(secureStorage!!)
        apiClient = ApiClient(apiKey, baseUrl)
        tokenRegistry = TokenRegistry(apiClient!!, secureStorage!!)
        fcmTokenManager = FcmTokenManager(secureStorage!!, tokenRegistry!!)

        val installationId = installationManager!!.getOrCreateInstallationId()
        Logger.info("SDK configured with installation ID: $installationId")

        delegate?.didInitialize(installationId)
    }

    fun getInstallationId(): String? {
        return installationManager?.getInstallationId()
    }

    fun isConfigured(): Boolean {
        return configuration != null
    }

    internal fun getConfiguration(): PushConfiguration {
        return configuration ?: throw SdkError.NotConfigured
    }

    internal fun onFcmTokenRefreshed(token: String) {
        fcmTokenManager?.saveToken(token)

        val installationId = getInstallationId()
        if (installationId != null) {
            val bundleId = configuration?.let { "com.pushplatform.sdk" } ?: return

            fcmTokenManager?.registerToken(installationId, token, bundleId) { result ->
                when (result) {
                    is TokenRegistry.Result.Success -> {
                        Logger.info("FCM token registered successfully")
                        delegate?.didUpdateFcmToken(token)
                    }
                    is TokenRegistry.Result.Failure -> {
                        Logger.error("FCM token registration failed: ${result.error.message}")
                        delegate?.didFailToRegisterFcmToken(result.error)
                    }
                }
            }
        }
    }

    internal fun onFcmMessageReceived(message: RemoteMessage) {
        Logger.debug("Processing FCM message: ${message.messageId}")
        delegate?.didReceiveNotification(message.data)
    }

    companion object {
        @Volatile
        private var instance: PushPlatform? = null

        fun getInstance(): PushPlatform {
            return instance ?: synchronized(this) {
                instance ?: PushPlatform().also { instance = it }
            }
        }
    }
}

interface PushPlatformDelegate {
    fun didInitialize(installationId: String)
    fun didUpdateFcmToken(token: String) {}
    fun didFailToRegisterFcmToken(error: SdkError) {}
    fun didReceiveNotification(data: Map<String, String>) {}
}
