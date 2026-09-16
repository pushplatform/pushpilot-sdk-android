package com.pushplatform.sdk

import android.os.Looper
import com.pushplatform.sdk.core.ApiClient
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.core.TokenRegistry
import com.pushplatform.sdk.models.SdkError
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class TokenRegistryTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var apiClient: ApiClient
    private lateinit var mockStorage: SecureStorage
    private lateinit var tokenRegistry: TokenRegistry

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        val baseUrl = mockServer.url("/").toString().trimEnd('/')
        apiClient = ApiClient("test-api-key", baseUrl)
        mockStorage = mock()
        tokenRegistry = TokenRegistry(apiClient, mockStorage)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun testRegisterToken_success() {
        mockServer.enqueue(MockResponse().setResponseCode(201).setBody("{}"))

        val latch = CountDownLatch(1)
        var result: TokenRegistry.Result<Unit>? = null

        tokenRegistry.registerToken(
            installationId = "inst-123",
            provider = "fcm",
            token = "token-456",
            bundleId = "com.example.app"
        ) { r ->
            result = r
            latch.countDown()
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertNotNull(result)
        assertTrue(result is TokenRegistry.Result.Success)
    }

    @Test
    fun testRegisterToken_400Error_noRetry() {
        mockServer.enqueue(MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_request\"}"))

        val latch = CountDownLatch(1)
        var result: TokenRegistry.Result<Unit>? = null

        tokenRegistry.registerToken(
            installationId = "inst-123",
            provider = "fcm",
            token = "token-456",
            bundleId = "com.example.app"
        ) { r ->
            result = r
            latch.countDown()
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertNotNull(result)
        assertTrue(result is TokenRegistry.Result.Failure)
        val error = (result as TokenRegistry.Result.Failure).error
        assertTrue(error is SdkError.ApiError)
        assertEquals(400, (error as SdkError.ApiError).statusCode)
    }

    @Test
    fun testRegisterToken_401Error_noRetry() {
        mockServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"unauthorized\"}"))

        val latch = CountDownLatch(1)
        var result: TokenRegistry.Result<Unit>? = null

        tokenRegistry.registerToken(
            installationId = "inst-123",
            provider = "fcm",
            token = "token-456",
            bundleId = "com.example.app"
        ) { r ->
            result = r
            latch.countDown()
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertNotNull(result)
        assertTrue(result is TokenRegistry.Result.Failure)
    }
}
