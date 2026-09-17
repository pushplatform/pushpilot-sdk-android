package com.pushplatform.sdk

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProGuardCompatibilityTest {

    @Test
    fun `PushPlatform class exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.PushPlatform")
        assertNotNull(clazz)
    }

    @Test
    fun `PushPlatformDelegate interface exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.PushPlatformDelegate")
        assertNotNull(clazz)
        assertTrue(clazz.isInterface)
    }

    @Test
    fun `ParsedNotification class exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.notifications.ParsedNotification")
        assertNotNull(clazz)
    }

    @Test
    fun `SdkError sealed class exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.models.SdkError")
        assertNotNull(clazz)
    }

    @Test
    fun `SdkError NotConfigured subclass exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.models.SdkError\$NotConfigured")
        assertNotNull(clazz)
    }

    @Test
    fun `SdkError NetworkError subclass exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.models.SdkError\$NetworkError")
        assertNotNull(clazz)
    }

    @Test
    fun `SdkError ApiError subclass exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.models.SdkError\$ApiError")
        assertNotNull(clazz)
    }

    @Test
    fun `PushPlatformFcmService class exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.PushPlatformFcmService")
        assertNotNull(clazz)
    }

    @Test
    fun `UserManager Result class exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.core.UserManager\$Result")
        assertNotNull(clazz)
    }

    @Test
    fun `Environment enum exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.Environment")
        assertNotNull(clazz)
        assertTrue(clazz.isEnum)
    }

    @Test
    fun `PushConfiguration class exists`() {
        val clazz = Class.forName("com.pushplatform.sdk.PushConfiguration")
        assertNotNull(clazz)
    }
}
