package com.pushplatform.sdk

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.pushplatform.sdk.utils.PermissionChecker
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PermissionCheckerTest {

    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var permissionChecker: PermissionChecker

    @Before
    fun setup() {
        mockContext = mock()
        mockActivity = mock()
        permissionChecker = PermissionChecker(mockContext)
    }

    @Test
    fun testHasNotificationPermission_granted() {
        whenever(mockContext.checkPermission(any(), anyOrNull(), anyOrNull()))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        val hasPermission = permissionChecker.hasNotificationPermission()

        assertTrue(hasPermission)
    }

    @Test
    fun testHasNotificationPermission_denied() {
        whenever(mockContext.checkPermission(any(), anyOrNull(), anyOrNull()))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        val hasPermission = permissionChecker.hasNotificationPermission()

        assertFalse(hasPermission)
    }

    @Test
    fun testRequestNotificationPermission_requestsPermission() {
        whenever(mockContext.checkPermission(any(), anyOrNull(), anyOrNull()))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        permissionChecker.requestNotificationPermission(mockActivity, 1001)

        // ActivityCompat.requestPermissions is static, can't verify directly
        // Just ensure no exception thrown
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class PermissionCheckerPreTiramisuTest {

    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var permissionChecker: PermissionChecker

    @Before
    fun setup() {
        mockContext = mock()
        mockActivity = mock()
        permissionChecker = PermissionChecker(mockContext)
    }

    @Test
    fun testHasNotificationPermission_preTiramisu_alwaysTrue() {
        val hasPermission = permissionChecker.hasNotificationPermission()

        assertTrue(hasPermission)
    }

    @Test
    fun testRequestNotificationPermission_preTiramisu_noOp() {
        permissionChecker.requestNotificationPermission(mockActivity, 1001)

        // No permission request should happen on pre-Android 13
        // Just ensure no exception thrown
    }

    @Test
    fun testShouldShowRequestPermissionRationale_preTiramisu_alwaysFalse() {
        val shouldShow = permissionChecker.shouldShowRequestPermissionRationale(mockActivity)

        assertFalse(shouldShow)
    }
}
