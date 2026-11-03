package com.pushplatform.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.core.FcmTokenManager
import com.pushplatform.sdk.core.FcmRegistration
import com.pushplatform.sdk.core.InstallationManager
import com.pushplatform.sdk.core.NotificationOpenReporter
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.core.TokenRegistry
import com.pushplatform.sdk.core.UserManager
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.notifications.NotificationChannelManager
import com.pushplatform.sdk.notifications.ParsedNotification
import com.pushplatform.sdk.utils.AppLifecycleTracker
import com.pushplatform.sdk.utils.Logger
import com.pushplatform.sdk.utils.PermissionChecker
import com.pushplatform.sdk.utils.DeviceMetadata
import java.util.concurrent.atomic.AtomicLong

class PushPlatform private constructor() {

    private var configuration: PushConfiguration? = null
    private var applicationContext: Context? = null
    private var secureStorage: SecureStorage? = null
    private var installationManager: InstallationManager? = null
    private var apiClient: ApiClient? = null
    private var tokenRegistry: TokenRegistry? = null
    private var fcmTokenManager: FcmTokenManager? = null
    private var notificationChannelManager: NotificationChannelManager? = null
    private var permissionChecker: PermissionChecker? = null
    private var lifecycleTracker: AppLifecycleTracker? = null
    private var userManager: UserManager? = null

    private val configurationVersion = AtomicLong()
    private val tokenLock = Any()
    private val openReporter = NotificationOpenReporter()
    @Volatile private var fcmRegistration: FcmRegistration? = null
    private var pendingFcmToken: String? = null

    var delegate: PushPlatformDelegate? = null
    private var deviceToken: String? = null

    data class InitializeOptions(
        val applicationId: String,
        val url: String? = null,
        val environment: Environment? = null,
        val debugMode: Boolean = false,
        val foregroundBannerFallback: Double = 0.0
    )

    fun initialize(context: Context, applicationId: String, completion: ((Result<String>) -> Unit)? = null) {
        initialize(context, InitializeOptions(applicationId), completion)
    }

    fun initialize(context: Context, options: InitializeOptions, completion: ((Result<String>) -> Unit)? = null) {
        if (configuration != null) {
            completion?.invoke(Result.success(getInstallationId() ?: options.applicationId))
            return
        }
        if (!options.foregroundBannerFallback.isFinite() || options.foregroundBannerFallback < 0) {
            completion?.invoke(Result.failure(SdkError.ApiError(400, "foregroundBannerFallback must be a non-negative number of seconds")))
            return
        }
        val reported = when {
            options.environment != null -> options.environment.name.lowercase()
            options.url != null -> "development"
            else -> null
        }
        configure(
            context,
            apiKey = "",
            environment = options.environment ?: Environment.PRODUCTION,
            debugMode = options.debugMode,
            apiBaseUrl = options.url ?: DEFAULT_API_BASE_URL,
            applicationId = options.applicationId,
            reportedEnvironment = reported,
            foregroundBannerFallbackSeconds = options.foregroundBannerFallback,
            completion = completion
        )
    }

    fun getDeviceToken(): String? = deviceToken

    fun configure(
        context: Context,
        apiKey: String,
        environment: Environment = Environment.PRODUCTION,
        debugMode: Boolean = false,
        apiBaseUrl: String? = null,
        applicationId: String? = null,
        reportedEnvironment: String? = environment.name.lowercase(),
        foregroundBannerFallbackSeconds: Double = 0.0,
        completion: ((Result<String>) -> Unit)? = null
    ) {
        val version = configurationVersion.incrementAndGet()
        fcmRegistration = null
        Logger.debugMode = debugMode
        Logger.debug("Configuring PushPlatform SDK")

        configuration = PushConfiguration(
            apiKey = apiKey,
            environment = environment,
            debugMode = debugMode,
            apiBaseUrl = apiBaseUrl,
            applicationId = applicationId,
            reportedEnvironment = reportedEnvironment,
            foregroundBannerFallbackSeconds = foregroundBannerFallbackSeconds
        )

        val baseUrl = apiBaseUrl ?: DEFAULT_API_BASE_URL

        val appContext = context.applicationContext
        applicationContext = appContext
        secureStorage = SecureStorage(appContext)
        installationManager = InstallationManager(secureStorage!!)
        apiClient = ApiClient(apiKey, baseUrl)
        val registry = TokenRegistry(apiClient!!, secureStorage!!)
        registry.onSubscriptionId = { openReporter.flush() }
        tokenRegistry = registry
        fcmTokenManager = FcmTokenManager(secureStorage!!, tokenRegistry!!, reportedEnvironment ?: "")
        notificationChannelManager = NotificationChannelManager(appContext)
        permissionChecker = PermissionChecker(appContext)
        userManager = UserManager(apiClient!!, installationManager!!)

        // Initialize lifecycle tracker
        if (appContext is Application) {
            lifecycleTracker = AppLifecycleTracker(appContext) {
                refreshMetadata()
                clearBadgeAndReportSession()
            }
        }

        notificationChannelManager?.createDefaultChannels()

        val deviceId = installationManager!!.getOrCreateInstallationId()
        val configuredApplicationId = applicationId
        if (configuredApplicationId == null) {
            Logger.info("SDK configured with local device ID: $deviceId")
            completion?.invoke(Result.failure(IllegalArgumentException("applicationId is required for installation registration")))
            return
        }
        openReporter.start(apiClient!!, configuredApplicationId) { registry.getSubscriptionId() }

        val manager = installationManager!!
        val registration = FcmRegistration(
            fcmTokenManager!!, appContext.packageName,
            isCurrent = { configurationVersion.get() == version },
            updated = { delegate?.didUpdateFcmToken() },
            failed = { delegate?.didFailToRegisterFcmToken(it) }
        )
        synchronized(tokenLock) {
            fcmRegistration = registration
            pendingFcmToken?.let { registration.refreshed(it) }
        }
        val fetchVersion = registration.fetchVersion()
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                val result = if (task.isSuccessful) Result.success(task.result)
                    else Result.failure(task.exception ?: IllegalStateException("FCM token unavailable"))
                registration.fetched(fetchVersion, result)
            }
        } catch (error: Exception) {
            registration.fetched(fetchVersion, Result.failure(error))
        }

        apiClient!!.registerInstallation(
            applicationId = configuredApplicationId,
            deviceId = deviceId,
            environment = reportedEnvironment,
            osVersion = Build.VERSION.RELEASE ?: "unknown",
            appVersion = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "unknown",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            metadata = DeviceMetadata.collect(appContext)
        ) { result ->
            if (configurationVersion.get() != version) {
                completion?.invoke(Result.failure(IllegalStateException("SDK configuration was replaced")))
                return@registerInstallation
            }
            when (result) {
                is ApiClient.Result.Success -> {
                    manager.saveBackendInstallationId(result.value)
                    registration.installationReady(result.value)
                    Logger.info("SDK configured with installation ID: ${result.value}")
                    delegate?.didInitialize(result.value)
                    completion?.invoke(Result.success(result.value))
                }
                is ApiClient.Result.Failure -> completion?.invoke(Result.failure(result.error))
            }
        }
    }

    /** Refresh real permission, locale and device metadata without replacing the installation ID. */
    fun refreshMetadata() {
        val config = configuration ?: return
        val context = applicationContext ?: return
        val manager = installationManager ?: return
        if (manager.getBackendInstallationId() == null) return
        val appId = config.applicationId ?: return
        apiClient?.registerInstallation(
            applicationId = appId, deviceId = manager.getOrCreateInstallationId(),
            environment = config.reportedEnvironment, osVersion = Build.VERSION.RELEASE ?: "unknown",
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}", metadata = DeviceMetadata.collect(context)
        ) { result -> if (result is ApiClient.Result.Failure) Logger.debug("Metadata refresh failed") }
    }

    fun getInstallationId(): String? {
        return installationManager?.getBackendInstallationId()
    }

    /** FCM subscription id issued by the server. Notification opens are reported with it. */
    fun getSubscriptionId(): String? {
        return tokenRegistry?.getSubscriptionId()
    }

    internal fun saveBadgeRevision(revision: Int) {
        secureStorage?.putString(KEY_BADGE_REVISION, revision.toString())
    }

    internal fun reportReceived(notificationId: String) {
        val appId = configuration?.applicationId ?: return
        val subscriptionId = tokenRegistry?.getSubscriptionId() ?: return
        apiClient?.reportReceived(notificationId, appId, subscriptionId) { }
    }

    private fun clearBadgeAndReportSession() {
        val context = applicationContext ?: return
        com.pushplatform.sdk.notifications.BadgeCount.apply(context, 0)
        val appId = configuration?.applicationId ?: return
        val subscriptionId = tokenRegistry?.getSubscriptionId() ?: return
        val revision = secureStorage?.getString(KEY_BADGE_REVISION)?.toIntOrNull()
        apiClient?.resetBadge(appId, subscriptionId, revision) { }
        apiClient?.reportSession(appId, subscriptionId) { }
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

    fun login(userId: String, metadata: Map<String, Any?>? = null, callback: (UserManager.Result<Unit>) -> Unit) {
        userManager?.login(userId, metadata, callback) ?: callback(UserManager.Result.Failure(SdkError.NotConfigured))
    }

    fun logout(callback: (UserManager.Result<Unit>) -> Unit) {
        userManager?.logout(callback) ?: callback(UserManager.Result.Failure(SdkError.NotConfigured))
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PermissionChecker.REQUEST_CODE_NOTIFICATION_PERMISSION) {
            val granted = grantResults.isNotEmpty() &&
                          grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            Logger.info("Notification permission result: granted=$granted")
            refreshMetadata()
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
        deviceToken = token
        delegate?.didUpdateDeviceToken(token)
        val registration = synchronized(tokenLock) {
            pendingFcmToken = token
            fcmRegistration
        }
        registration?.refreshed(token)
    }

    internal fun onFcmMessageReceived(
        message: RemoteMessage,
        parsed: ParsedNotification,
        isInForeground: Boolean
    ): Boolean {
        Logger.debug("Processing FCM message: ${message.messageId}, foreground=$isInForeground")
        val display = NotificationDisplay(parsed)
        if (isInForeground) delegate?.notificationWillDisplay(display)
        delegate?.didReceiveNotification(parsed, isInForeground)
        val seconds = configuration?.foregroundBannerFallbackSeconds ?: 0.0
        val timeoutMs = if (seconds > 0) (seconds * 1000).toLong() else null
        return display.awaitDecision(timeoutMs)
    }

    internal fun onNotificationOpened(parsed: ParsedNotification) {
        parsed.notificationId?.let { openReporter.report(it) }
        delegate?.didOpenNotification(parsed)
    }

    companion object {
        const val DEFAULT_API_BASE_URL = "https://api.pushpilot.ru"
        private const val KEY_BADGE_REVISION = "badge_revision"
        @Volatile
        private var instance: PushPlatform? = null

        fun getInstance(): PushPlatform {
            return instance ?: synchronized(this) {
                instance ?: PushPlatform().also { instance = it }
            }
        }
    }
}

class NotificationDisplay(val notification: ParsedNotification) {
    private val lock = Any()
    private var prevented = false
    private var deferred = false
    private var finished = false
    private var waiter: java.util.concurrent.CountDownLatch? = null

    fun preventDefault() { synchronized(lock) { prevented = true } }
    val isPrevented: Boolean get() = synchronized(lock) { prevented }

    /** Hold the system banner until finish(). Sync delegates that never call this return immediately. */
    fun deferDecision() {
        synchronized(lock) {
            deferred = true
            if (waiter == null && !finished) waiter = java.util.concurrent.CountDownLatch(1)
        }
    }

    fun finish() {
        val latch = synchronized(lock) {
            if (finished) return
            finished = true
            waiter
        }
        latch?.countDown()
    }

    fun awaitDecision(timeoutMs: Long? = null): Boolean {
        val latch = synchronized(lock) {
            if (!deferred || finished) return prevented
            waiter
        }
        try {
            if (timeoutMs != null && timeoutMs > 0) {
                latch?.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            } else {
                latch?.await()
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return isPrevented
    }
}

interface PushPlatformDelegate {
    fun didInitialize(installationId: String)
    fun didUpdateFcmToken() {}
    fun didUpdateDeviceToken(token: String) {}
    fun didFailToRegisterFcmToken(error: SdkError) {}
    fun didReceiveNotification(notification: ParsedNotification, isInForeground: Boolean) {}
    fun notificationWillDisplay(event: NotificationDisplay) {}
    fun didOpenNotification(notification: ParsedNotification) {}
    fun onNotificationPermissionResult(granted: Boolean) {}
}
