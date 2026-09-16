package com.pushplatform.sdk

import android.app.Activity
import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.core.FcmTokenManager
import com.pushplatform.sdk.core.InstallationManager
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.core.TokenRegistry
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.notifications.NotificationChannelManager
import com.pushplatform.sdk.utils.Logger
import com.pushplatform.sdk.utils.PermissionChecker

class PushPlatform private constructor() {

    private var configuration: PushConfiguration? = null
    private var secureStorage: SecureStorage? = null
    private var installationManager: InstallationManager? = null
    private var apiClient: ApiClient? = null
    private var tokenRegistry: TokenRegistry? = null
    private var fcmTokenManager: FcmTokenManager? = null
    private var notificationChannelManager: NotificationChannelManager? = null
    private var permissionChecker: PermissionChecker? = null

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
        notificationChannelManager = NotificationChannelManager(context.applicationContext)
        permissionChecker = PermissionChecker(context.applicationContext)

        notificationChannelManager?.createDefaultChannels()

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

    fun hasNotificationPermission(): Boolean {
        return permissionChecker?.hasNotificationPermission() ?: false
    }

    fun requestNotificationPermission(activity: Activity) {
        permissionChecker?.requestNotificationPermission(
            activity,
            PermissionChecker.REQUEST_CODE_NOTIFICATION_PERMISSION
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PermissionChecker.REQUEST_CODE_NOTIFICATION_PERMISSION) {
            val granted = grantResults.isNotEmpty() &&
                          grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            Logger.info("Notification permission result: granted=$granted")
            delegate?.onNotificationPermissionResult(granted)
        }
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
    fun onNotificationPermissionResult(granted: Boolean) {}
}
