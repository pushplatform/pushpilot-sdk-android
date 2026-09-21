package com.pushplatform.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.core.FcmTokenManager
import com.pushplatform.sdk.core.InstallationManager
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.core.TokenRegistry
import com.pushplatform.sdk.core.UserManager
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.notifications.NotificationChannelManager
import com.pushplatform.sdk.notifications.ParsedNotification
import com.pushplatform.sdk.utils.AppLifecycleTracker
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
    private var lifecycleTracker: AppLifecycleTracker? = null
    private var userManager: UserManager? = null

    var delegate: PushPlatformDelegate? = null

    fun configure(
        context: Context,
        apiKey: String,
        environment: Environment = Environment.PRODUCTION,
        debugMode: Boolean = false,
        apiBaseUrl: String? = null,
        applicationId: String? = null,
        completion: ((Result<String>) -> Unit)? = null
    ) {
        Logger.debugMode = debugMode
        Logger.debug("Configuring PushPlatform SDK")

        configuration = PushConfiguration(
            apiKey = apiKey,
            environment = environment,
            debugMode = debugMode,
            apiBaseUrl = apiBaseUrl,
            applicationId = applicationId
        )

        val baseUrl = apiBaseUrl ?: when (environment) {
            Environment.DEVELOPMENT -> "https://api-dev.pushplatform.com"
            Environment.PRODUCTION -> "https://api.pushplatform.com"
        }

        val appContext = context.applicationContext
        secureStorage = SecureStorage(appContext)
        installationManager = InstallationManager(secureStorage!!)
        apiClient = ApiClient(apiKey, baseUrl)
        tokenRegistry = TokenRegistry(apiClient!!, secureStorage!!)
        fcmTokenManager = FcmTokenManager(secureStorage!!, tokenRegistry!!)
        notificationChannelManager = NotificationChannelManager(appContext)
        permissionChecker = PermissionChecker(appContext)
        userManager = UserManager(apiClient!!, installationManager!!)

        // Initialize lifecycle tracker
        if (appContext is Application) {
            lifecycleTracker = AppLifecycleTracker(appContext)
        }

        notificationChannelManager?.createDefaultChannels()

        val deviceId = installationManager!!.getOrCreateInstallationId()
        val configuredApplicationId = applicationId
        if (configuredApplicationId == null) {
            Logger.info("SDK configured with local device ID: $deviceId")
            completion?.invoke(Result.failure(IllegalArgumentException("applicationId is required for installation registration")))
            return
        }

        apiClient!!.registerInstallation(
            applicationId = configuredApplicationId,
            deviceId = deviceId,
            environment = environment.name.lowercase(),
            osVersion = Build.VERSION.RELEASE ?: "unknown",
            appVersion = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "unknown",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        ) { result ->
            when (result) {
                is ApiClient.Result.Success -> {
                    installationManager!!.saveBackendInstallationId(result.value)
                    Logger.info("SDK configured with installation ID: ${result.value}")
                    delegate?.didInitialize(result.value)
                    completion?.invoke(Result.success(result.value))
                }
                is ApiClient.Result.Failure -> completion?.invoke(Result.failure(result.error))
            }
        }
    }

    fun getInstallationId(): String? {
        return installationManager?.getBackendInstallationId()
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

    fun login(userId: String, callback: (UserManager.Result<Unit>) -> Unit) {
        userManager?.login(userId, callback) ?: callback(UserManager.Result.Failure(SdkError.NotConfigured))
    }

    fun logout(callback: (UserManager.Result<Unit>) -> Unit) {
        userManager?.logout(callback) ?: callback(UserManager.Result.Failure(SdkError.NotConfigured))
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PermissionChecker.REQUEST_CODE_NOTIFICATION_PERMISSION) {
            val granted = grantResults.isNotEmpty() &&
                          grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            Logger.info("Notification permission result: granted=$granted")
            delegate?.onNotificationPermissionResult(granted)
        }
    }

    internal fun isAppInForeground(): Boolean {
        return lifecycleTracker?.isAppInForeground() ?: false
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
                        delegate?.didUpdateFcmToken()
                    }
                    is TokenRegistry.Result.Failure -> {
                        Logger.error("FCM token registration failed: ${result.error.message}")
                        delegate?.didFailToRegisterFcmToken(result.error)
                    }
                }
            }
        }
    }

    internal fun onFcmMessageReceived(
        message: RemoteMessage,
        parsed: ParsedNotification,
        isInForeground: Boolean
    ) {
        Logger.debug("Processing FCM message: ${message.messageId}, foreground=$isInForeground")
        delegate?.didReceiveNotification(parsed, isInForeground)
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
    fun didUpdateFcmToken() {}
    fun didFailToRegisterFcmToken(error: SdkError) {}
    fun didReceiveNotification(notification: ParsedNotification, isInForeground: Boolean) {}
    fun onNotificationPermissionResult(granted: Boolean) {}
}
