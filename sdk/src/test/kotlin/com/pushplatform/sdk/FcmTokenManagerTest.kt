package com.pushplatform.sdk

import com.pushplatform.sdk.core.FcmTokenManager
import com.pushplatform.sdk.core.SecureStorage
import com.pushplatform.sdk.core.TokenRegistry
import com.pushplatform.sdk.models.SdkError
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class FcmTokenManagerTest {

    private lateinit var mockStorage: SecureStorage
    private lateinit var mockTokenRegistry: TokenRegistry
    private lateinit var fcmTokenManager: FcmTokenManager

    @Before
    fun setup() {
        mockStorage = mock()
        mockTokenRegistry = mock()
        fcmTokenManager = FcmTokenManager(mockStorage, mockTokenRegistry)
    }

    @Test
    fun testSaveToken_firstTime() {
        whenever(mockStorage.getString("fcm_token")).thenReturn(null)

        fcmTokenManager.saveToken("test-token-123")

        verify(mockStorage).putString("fcm_token", "test-token-123")
    }

    @Test
    fun testSaveToken_sameToken() {
        whenever(mockStorage.getString("fcm_token")).thenReturn("test-token-123")

        fcmTokenManager.saveToken("test-token-123")

        verify(mockStorage, never()).putString(any(), any())
    }

    @Test
    fun testSaveToken_differentToken() {
        whenever(mockStorage.getString("fcm_token")).thenReturn("old-token")

        fcmTokenManager.saveToken("new-token")

        verify(mockStorage).putString("fcm_token", "new-token")
    }

    @Test
    fun testGetToken_returnsStoredToken() {
        whenever(mockStorage.getString("fcm_token")).thenReturn("stored-token")

        val token = fcmTokenManager.getToken()

        assertEquals("stored-token", token)
    }

    @Test
    fun testGetToken_returnsNullWhenNoToken() {
        whenever(mockStorage.getString("fcm_token")).thenReturn(null)

        val token = fcmTokenManager.getToken()

        assertNull(token)
    }

    @Test
    fun testRegisterToken_success() {
        var callbackResult: TokenRegistry.Result<Unit>? = null

        doAnswer { invocation ->
            val callback = invocation.getArgument<(TokenRegistry.Result<Unit>) -> Unit>(5)
            callback(TokenRegistry.Result.Success(Unit))
            null
        }.whenever(mockTokenRegistry).registerToken(any(), any(), any(), any(), any(), any())

        fcmTokenManager.registerToken("inst-123", "token-456", "com.example.app") { result ->
            callbackResult = result
        }

        verify(mockTokenRegistry).registerToken(
            eq("inst-123"),
            eq("fcm"),
            eq("token-456"),
            eq("com.example.app"),
            eq("production"),
            any()
        )
        assert(callbackResult is TokenRegistry.Result.Success)
    }

    @Test
    fun testRegisterToken_failure() {
        var callbackResult: TokenRegistry.Result<Unit>? = null
        val error = SdkError.NetworkError(Exception("Network failure"))

        doAnswer { invocation ->
            val callback = invocation.getArgument<(TokenRegistry.Result<Unit>) -> Unit>(5)
            callback(TokenRegistry.Result.Failure(error))
            null
        }.whenever(mockTokenRegistry).registerToken(any(), any(), any(), any(), any(), any())

        fcmTokenManager.registerToken("inst-123", "token-456", "com.example.app") { result ->
            callbackResult = result
        }

        assert(callbackResult is TokenRegistry.Result.Failure)
        assertEquals(error, (callbackResult as TokenRegistry.Result.Failure).error)
    }

    @Test
    fun testClearToken() {
        fcmTokenManager.clearToken()

        verify(mockStorage).remove("fcm_token")
    }
}