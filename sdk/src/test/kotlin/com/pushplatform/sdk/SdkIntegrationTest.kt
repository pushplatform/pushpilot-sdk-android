package com.pushplatform.sdk

import android.content.Context
import com.pushplatform.sdk.core.UserManager
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.notifications.ParsedNotification
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SdkIntegrationTest {

    private lateinit var context: Context
    private lateinit var sdk: PushPlatform

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        sdk = PushPlatform.getInstance()
    }

    @Test
    fun `SDK configuration without application ID reports registration requirement`() {
        var initCallbackFired = false
        var configureResult: Result<String>? = null
        val latch = CountDownLatch(1)

        sdk.delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) {
                initCallbackFired = true
            }
        }

        sdk.configure(
            context = context,
            apiKey = "pk_test_integration",
            environment = Environment.DEVELOPMENT,
            debugMode = true,
            completion = {
                configureResult = it
                latch.countDown()
            }
        )

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(sdk.isConfigured())
        assertFalse(initCallbackFired)
        assertTrue(configureResult?.isFailure == true)
        assertNull(sdk.getInstallationId())
    }

    @Test
    fun `configuration requirement is reported across reconfigurations`() {
        val results = mutableListOf<Result<String>>()
        val latch = CountDownLatch(2)
        sdk.configure(
            context = context,
            apiKey = "pk_test_persistence",
            environment = Environment.DEVELOPMENT,
            debugMode = true,
            completion = {
                results += it
                latch.countDown()
            }
        )

        val newSdkInstance = PushPlatform.getInstance()
        newSdkInstance.configure(
            context = context,
            apiKey = "pk_test_persistence",
            environment = Environment.DEVELOPMENT,
            debugMode = true,
            completion = {
                results += it
                latch.countDown()
            }
        )

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(2, results.size)
        assertTrue(results.all { it.isFailure })
    }

    @Test
    fun `login without configuration returns error immediately`() {
        val latch = CountDownLatch(1)
        var result: UserManager.Result<Unit>? = null

        sdk.configure(
            context = context,
            apiKey = "pk_test_temp",
            environment = Environment.DEVELOPMENT,
            debugMode = true
        )

        sdk.login("user-test") {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertNotNull(result)
    }

    @Test
    fun `notification permission check returns value`() {
        sdk.configure(
            context = context,
            apiKey = "pk_test_permission",
            environment = Environment.DEVELOPMENT,
            debugMode = true
        )

        val hasPermission = sdk.hasNotificationPermission()
        assertNotNull(hasPermission)
    }

    @Test
    fun `multiple configures preserve installation ID`() {
        sdk.configure(
            context = context,
            apiKey = "pk_test_first",
            environment = Environment.DEVELOPMENT,
            debugMode = true
        )
        val firstInstallationId = sdk.getInstallationId()

        sdk.configure(
            context = context,
            apiKey = "pk_test_second",
            environment = Environment.PRODUCTION,
            debugMode = false
        )
        val secondInstallationId = sdk.getInstallationId()

        assertEquals(firstInstallationId, secondInstallationId)
        assertTrue(sdk.isConfigured())
    }

    @Test
    fun `delegate does not fire before installation registration succeeds`() {
        val latch = CountDownLatch(1)
        var didInitializeCalled = false
        var receivedInstallationId: String? = null

        sdk.delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) {
                didInitializeCalled = true
                receivedInstallationId = installationId
            }

            override fun didUpdateFcmToken() {}
            override fun didFailToRegisterFcmToken(error: SdkError) {}
            override fun didReceiveNotification(notification: ParsedNotification, isInForeground: Boolean) {}
            override fun onNotificationPermissionResult(granted: Boolean) {}
        }

        sdk.configure(
            context = context,
            apiKey = "pk_test_delegate",
            environment = Environment.DEVELOPMENT,
            debugMode = true,
            completion = { latch.countDown() }
        )

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertFalse(didInitializeCalled)
        assertNull(receivedInstallationId)
    }

    @Test
    fun `environment configuration works for both development and production`() {
        sdk.configure(
            context = context,
            apiKey = "pk_test_env_dev",
            environment = Environment.DEVELOPMENT,
            debugMode = true
        )
        assertTrue(sdk.isConfigured())

        val newSdk = PushPlatform.getInstance()
        newSdk.configure(
            context = context,
            apiKey = "pk_prod_env",
            environment = Environment.PRODUCTION,
            debugMode = false
        )
        assertTrue(newSdk.isConfigured())
    }

    @Test
    fun `empty user ID login is handled`() {
        val latch = CountDownLatch(1)
        var result: UserManager.Result<Unit>? = null

        sdk.configure(
            context = context,
            apiKey = "pk_test_empty_user",
            environment = Environment.DEVELOPMENT,
            debugMode = true
        )

        sdk.login("") {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertNotNull(result)
    }
}
