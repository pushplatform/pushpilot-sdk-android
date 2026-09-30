package com.pushplatform.sdk

import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.models.Installation
import com.pushplatform.sdk.models.Subscription
import com.pushplatform.sdk.models.UserUpdate
import com.pushplatform.sdk.utils.Logger
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class ApiClientTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var apiClient: ApiClient
    private val apiKey = "pk_test_key_12345"

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        val baseUrl = mockServer.url("/").toString().removeSuffix("/")
        apiClient = ApiClient(apiKey, baseUrl)
        Logger.debugMode = true
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun testCreateInstallation_success() {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("{}"))

        val installation = Installation(
            installationId = "550e8400-e29b-41d4-a716-446655440000",
            platform = "android",
            osVersion = "13",
            appVersion = "1.2.3",
            sdkVersion = "1.0.0",
            locale = "en_US",
            timezone = "UTC",
            manufacturer = "Google",
            model = "Pixel 7"
        )

        val latch = CountDownLatch(1)
        var result: ApiClient.Result<Unit>? = null

        apiClient.createInstallation(installation) { res ->
            result = res
            latch.countDown()
        }

        assertTrue("Callback should complete", latch.await(10, TimeUnit.SECONDS))
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be Success", result is ApiClient.Result.Success)
    }

    @Test
    fun testCreateInstallation_networkError() {
        mockServer.shutdown()

        val installation = Installation(
            installationId = "550e8400-e29b-41d4-a716-446655440000",
            platform = "android",
            osVersion = "13",
            appVersion = "1.2.3",
            sdkVersion = "1.0.0",
            locale = "en_US",
            timezone = "UTC",
            manufacturer = "Google",
            model = "Pixel 7"
        )

        val latch = CountDownLatch(1)
        var result: ApiClient.Result<Unit>? = null

        apiClient.createInstallation(installation) { res ->
            result = res
            latch.countDown()
        }

        assertTrue("Callback should complete", latch.await(10, TimeUnit.SECONDS))
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be Failure", result is ApiClient.Result.Failure)
    }

    @Test
    fun testCreateInstallation_400Error() {
        mockServer.enqueue(MockResponse().setResponseCode(400).setBody("{\"error\":\"Invalid\"}"))

        val installation = Installation(
            installationId = "invalid",
            platform = "android",
            osVersion = "13",
            appVersion = "1.2.3",
            sdkVersion = "1.0.0",
            locale = "en_US",
            timezone = "UTC",
            manufacturer = "Google",
            model = "Pixel 7"
        )

        val latch = CountDownLatch(1)
        var result: ApiClient.Result<Unit>? = null

        apiClient.createInstallation(installation) { res ->
            result = res
            latch.countDown()
        }

        assertTrue("Callback should complete", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Result should be Failure", result is ApiClient.Result.Failure)
        val failure = result as ApiClient.Result.Failure
        assertTrue("Error should be ApiError", failure.error is com.pushplatform.sdk.models.SdkError.ApiError)
    }

    @Test
    fun testUpdateInstallation_success() {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val userUpdate = UserUpdate(externalUserId = "user_42")
        val installationId = "550e8400-e29b-41d4-a716-446655440000"

        val latch = CountDownLatch(1)
        var result: ApiClient.Result<Unit>? = null

        apiClient.updateInstallation(installationId, userUpdate) { res ->
            result = res
            latch.countDown()
        }

        assertTrue("Callback should complete", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Result should be Success", result is ApiClient.Result.Success)
    }

    @Test
    fun testCreateSubscription_success() {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("{}"))

        val subscription = Subscription(
            provider = "fcm",
            environment = "production",
            token = "fcm_token_abc123",
            bundleId = "com.example.app"
        )
        val installationId = "550e8400-e29b-41d4-a716-446655440000"

        val latch = CountDownLatch(1)
        var result: ApiClient.Result<Unit>? = null

        apiClient.createSubscription(installationId, subscription) { res ->
            result = res
            latch.countDown()
        }

        assertTrue("Callback should complete", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Result should be Success", result is ApiClient.Result.Success)
        assertEquals("/v1/installations/$installationId/tokens", mockServer.takeRequest().path)
    }

    @Test
    fun testResultMap_success() {
        val result: ApiClient.Result<String> = ApiClient.Result.Success("test")
        val mapped = result.map { it.uppercase() }

        assertTrue(mapped is ApiClient.Result.Success)
        assertEquals("TEST", (mapped as ApiClient.Result.Success).value)
    }

    @Test
    fun testResultMap_failure() {
        val error = com.pushplatform.sdk.models.SdkError.NetworkError(Exception("test"))
        val result: ApiClient.Result<String> = ApiClient.Result.Failure(error)
        val mapped = result.map { it.uppercase() }

        assertTrue(mapped is ApiClient.Result.Failure)
        assertSame(error, (mapped as ApiClient.Result.Failure).error)
    }

    @Test
    fun testResultIsSuccess() {
        val success: ApiClient.Result<String> = ApiClient.Result.Success("test")
        assertTrue(success.isSuccess())
        assertFalse(success.isFailure())
    }

    @Test
    fun testResultIsFailure() {
        val error = com.pushplatform.sdk.models.SdkError.NetworkError(Exception("test"))
        val failure: ApiClient.Result<String> = ApiClient.Result.Failure(error)
        assertFalse(failure.isSuccess())
        assertTrue(failure.isFailure())
    }
}
