package com.pushplatform.sdk

import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.pushplatform.sdk.models.SdkError
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class InitialFcmRegistrationTest {
    private lateinit var server: MockWebServer
    private lateinit var sdk: PushPlatform
    private lateinit var firebase: FirebaseMessaging
    private lateinit var staticFirebase: MockedStatic<FirebaseMessaging>
    private val events = CopyOnWriteArrayList<String>()
    private val errors = CopyOnWriteArrayList<SdkError>()
    private val application = "550e8400-e29b-41d4-a716-446655440000"
    private val installation = "550e8400-e29b-41d4-a716-446655440001"

    @Before fun setup() {
        resetSingleton()
        RuntimeEnvironment.getApplication().getSharedPreferences("com.pushplatform.sdk_prefs", 0).edit().clear().commit()
        server = MockWebServer().also { it.start() }
        sdk = PushPlatform.getInstance()
        firebase = mock()
        whenever(firebase.token).thenReturn(Tasks.forResult("existing-fcm-token"))
        staticFirebase = Mockito.mockStatic(FirebaseMessaging::class.java)
        staticFirebase.`when`<FirebaseMessaging> { FirebaseMessaging.getInstance() }.thenReturn(firebase)
        sdk.delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) { events += "initialized:$installationId" }
            override fun didUpdateFcmToken() { events += "registered" }
            override fun didFailToRegisterFcmToken(error: SdkError) { errors += error }
        }
    }

    @After fun teardown() {
        sdk.delegate = null
        staticFirebase.close()
        server.shutdown()
        resetSingleton()
    }

    private fun resetSingleton() {
        PushPlatform::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
    }

    private fun configure(environment: Environment = Environment.DEVELOPMENT, key: String = "scoped-device-key") {
        sdk.configure(RuntimeEnvironment.getApplication(), key, environment,
            apiBaseUrl = server.url("/").toString().removeSuffix("/"), applicationId = application)
    }

    private fun installationResponse(id: String = installation) = MockResponse().setResponseCode(201).setBody("{\"id\":\"$id\"}")
    private fun tokenResponse() = MockResponse().setResponseCode(201).setBody("{}")

    private fun awaitCondition(check: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!check() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Expected callback/state within 5 seconds", check())
    }

    private fun request(): RecordedRequest = server.takeRequest(3, TimeUnit.SECONDS) ?: error("Missing backend request")

    private fun assertToken(request: RecordedRequest, token: String, environment: String, id: String = installation, key: String = "scoped-device-key") {
        assertEquals("/v1/installations/$id/tokens", request.path)
        assertEquals("Bearer $key", request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("fcm", body.getString("provider"))
        assertEquals(token, body.getString("token"))
        assertEquals(environment, body.getString("environment"))
        assertEquals(RuntimeEnvironment.getApplication().packageName, body.getString("bundle_id"))
    }

    @Test fun `configure registers existing Firebase token without onNewToken`() {
        server.enqueue(installationResponse()); server.enqueue(tokenResponse())
        configure()
        val install = request()
        assertEquals("/v1/installations", install.path)
        assertEquals(application, JSONObject(install.body.readUtf8()).getString("application_id"))
        awaitCondition { events.contains("registered") }
        assertToken(request(), "existing-fcm-token", "development")
        assertEquals(installation, sdk.getInstallationId())
        assertTrue(errors.isEmpty())
        assertEquals(2, server.requestCount)
    }

    @Test fun `service token before configure is replayed after installation exists`() {
        val pending = TaskCompletionSource<String>()
        whenever(firebase.token).thenReturn(pending.task)
        PushPlatformFcmService().onNewToken("early-fcm-token")
        assertEquals(0, server.requestCount)
        server.enqueue(installationResponse()); server.enqueue(tokenResponse())
        configure(Environment.PRODUCTION)
        request()
        awaitCondition { events.contains("registered") }
        assertToken(request(), "early-fcm-token", "production")
        pending.setResult("early-fcm-token")
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, server.requestCount)
    }

    @Test fun `rotation while installation is pending supersedes stale Firebase lookup`() {
        val pending = TaskCompletionSource<String>()
        whenever(firebase.token).thenReturn(pending.task)
        configure()
        assertEquals("/v1/installations", request().path)
        PushPlatformFcmService().onNewToken("rotated-fcm-token")
        pending.setResult("stale-query-token")
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, server.requestCount)
        server.enqueue(installationResponse()); server.enqueue(tokenResponse())
        awaitCondition { events.contains("registered") }
        assertToken(request(), "rotated-fcm-token", "development")
        assertEquals(2, server.requestCount)
    }

    @Test fun `later onNewToken registers rotation in the configured production scope`() {
        server.enqueue(installationResponse()); server.enqueue(tokenResponse()); server.enqueue(tokenResponse())
        configure(Environment.PRODUCTION)
        request(); awaitCondition { events.contains("registered") }
        assertToken(request(), "existing-fcm-token", "production")
        PushPlatformFcmService().onNewToken("next-fcm-token")
        awaitCondition { events.count { it == "registered" } == 2 }
        assertToken(request(), "next-fcm-token", "production")
    }

    @Test fun `rotation waits for older token registration and is applied last`() {
        server.enqueue(installationResponse())
        server.enqueue(tokenResponse().setBodyDelay(300, TimeUnit.MILLISECONDS))
        server.enqueue(tokenResponse())
        configure()
        request()
        awaitCondition { server.requestCount == 2 }
        assertToken(request(), "existing-fcm-token", "development")
        PushPlatformFcmService().onNewToken("latest-fcm-token")
        awaitCondition { events.contains("registered") }
        assertToken(request(), "latest-fcm-token", "development")
        assertEquals(1, events.count { it == "registered" })
    }

    @Test fun `Firebase lookup failure does not report token registration success`() {
        whenever(firebase.token).thenReturn(Tasks.forException(IllegalStateException("controlled Firebase failure")))
        server.enqueue(installationResponse())
        configure()
        request()
        awaitCondition { events.any { it.startsWith("initialized:") } && errors.isNotEmpty() }
        assertFalse(events.contains("registered"))
        assertEquals(1, server.requestCount)
    }

    @Test fun `backend rejects token registration without emitting registered event`() {
        server.enqueue(installationResponse()); server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"denied\"}"))
        configure()
        request(); awaitCondition { errors.isNotEmpty() }
        assertToken(request(), "existing-fcm-token", "development")
        assertFalse(events.contains("registered"))
        assertEquals(401, (errors.single() as SdkError.ApiError).statusCode)
    }

    @Test fun `old configuration callbacks cannot register token or overwrite new installation`() {
        val oldToken = TaskCompletionSource<String>()
        whenever(firebase.token).thenReturn(oldToken.task)
        server.enqueue(installationResponse().setBodyDelay(300, TimeUnit.MILLISECONDS))
        configure(key = "old-key")
        request()
        val newId = "550e8400-e29b-41d4-a716-446655440002"
        whenever(firebase.token).thenReturn(Tasks.forResult("new-scope-token"))
        server.enqueue(installationResponse(newId)); server.enqueue(tokenResponse())
        configure(Environment.PRODUCTION, "new-key")
        assertEquals("/v1/installations", request().path)
        oldToken.setResult("old-scope-token")
        awaitCondition { events.contains("registered") }
        assertToken(request(), "new-scope-token", "production", newId, "new-key")
        Thread.sleep(350)
        assertEquals(newId, sdk.getInstallationId())
        assertEquals(3, server.requestCount)
        assertFalse(events.contains("initialized:$installation"))
    }
}
