package com.pushplatform.sdk

import android.content.Context
import com.pushplatform.sdk.core.InstallationManager
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.utils.Logger

class PushPlatform private constructor() {

    private var configuration: PushConfiguration? = null
    private var secureStorage: SecureStorage? = null
    private var installationManager: InstallationManager? = null

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

        secureStorage = SecureStorage(context.applicationContext)
        installationManager = InstallationManager(secureStorage!!)

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
}
