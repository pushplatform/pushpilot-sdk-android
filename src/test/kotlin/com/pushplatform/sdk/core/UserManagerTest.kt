package com.pushplatform.sdk.core

import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.models.UserUpdate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class UserManagerTest {

    @Mock
    private lateinit var apiClient: ApiClient

    @Mock
    private lateinit var installationManager: InstallationManager

    private lateinit var userManager: UserManager

    @Before
    fun setup() {
        userManager = UserManager(apiClient, installationManager)
    }

    @Test
    fun `login succeeds on successful API call`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"
        val userId = "user-123"

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        doAnswer { invocation ->
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Success(Unit))
            null
        }.whenever(apiClient).updateInstallation(any(), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.login(userId) {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Success)
    }

    @Test
    fun `login fails when installation ID is null`() {
        val latch = CountDownLatch(1)
        whenever(installationManager.getInstallationId()).thenReturn(null)

        var result: UserManager.Result<Unit>? = null
        userManager.login("user-123") {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Failure)
        val error = (result as UserManager.Result.Failure).error
        assertTrue(error is SdkError.NotConfigured)
    }

    @Test
    fun `login fails on 400 error without retry`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"
        val error = SdkError.ApiError(400, "Bad request")

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        doAnswer { invocation ->
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Failure(error))
            null
        }.whenever(apiClient).updateInstallation(any(), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.login("user-123") {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Failure)
        val resultError = (result as UserManager.Result.Failure).error
        assertTrue(resultError is SdkError.ApiError)
        assertEquals(400, (resultError as SdkError.ApiError).statusCode)

        verify(apiClient, times(1)).updateInstallation(any(), any(), any())
    }

    @Test
    fun `login fails on 401 error without retry`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"
        val error = SdkError.ApiError(401, "Unauthorized")

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        doAnswer { invocation ->
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Failure(error))
            null
        }.whenever(apiClient).updateInstallation(any(), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.login("user-123") {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Failure)
        val resultError = (result as UserManager.Result.Failure).error
        assertTrue(resultError is SdkError.ApiError)
        assertEquals(401, (resultError as SdkError.ApiError).statusCode)

        verify(apiClient, times(1)).updateInstallation(any(), any(), any())
    }

    @Test
    fun `logout succeeds on successful API call`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        doAnswer { invocation ->
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Success(Unit))
            null
        }.whenever(apiClient).updateInstallation(any(), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.logout {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Success)
    }

    @Test
    fun `logout fails when installation ID is null`() {
        val latch = CountDownLatch(1)
        whenever(installationManager.getInstallationId()).thenReturn(null)

        var result: UserManager.Result<Unit>? = null
        userManager.logout {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Failure)
        val error = (result as UserManager.Result.Failure).error
        assertTrue(error is SdkError.NotConfigured)
    }

    @Test
    fun `logout sends null external_user_id to API`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        var capturedUserUpdate: UserUpdate? = null
        doAnswer { invocation ->
            capturedUserUpdate = invocation.getArgument(1)
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Success(Unit))
            null
        }.whenever(apiClient).updateInstallation(eq(installationId), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.logout {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Success)
        assertNotNull(capturedUserUpdate)
        assertNull(capturedUserUpdate?.externalUserId)
    }

    @Test
    fun `login sends correct user ID to API`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"
        val userId = "user-456"

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        var capturedUserUpdate: UserUpdate? = null
        doAnswer { invocation ->
            capturedUserUpdate = invocation.getArgument(1)
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Success(Unit))
            null
        }.whenever(apiClient).updateInstallation(eq(installationId), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.login(userId) {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Success)
        assertNotNull(capturedUserUpdate)
        assertEquals(userId, capturedUserUpdate?.externalUserId)
    }

    @Test
    fun `login with empty user ID is allowed`() {
        val latch = CountDownLatch(1)
        val installationId = "test-installation-id"

        whenever(installationManager.getInstallationId()).thenReturn(installationId)

        var capturedUserUpdate: UserUpdate? = null
        doAnswer { invocation ->
            capturedUserUpdate = invocation.getArgument(1)
            val callback = invocation.getArgument<(ApiClient.Result<Unit>) -> Unit>(2)
            callback(ApiClient.Result.Success(Unit))
            null
        }.whenever(apiClient).updateInstallation(eq(installationId), any(), any())

        var result: UserManager.Result<Unit>? = null
        userManager.login("") {
            result = it
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(result is UserManager.Result.Success)
        assertNotNull(capturedUserUpdate)
        assertEquals("", capturedUserUpdate?.externalUserId)
    }

}
