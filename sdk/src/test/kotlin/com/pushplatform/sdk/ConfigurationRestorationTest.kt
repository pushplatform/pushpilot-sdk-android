package com.pushplatform.sdk

import android.os.Looper
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.messaging.FirebaseMessaging
import com.pushplatform.sdk.core.ApiClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
class ConfigurationRestorationTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var oldServer: MockWebServer
    private lateinit var server: MockWebServer
    private lateinit var sdk: PushPlatform
    private lateinit var firebase: MockedStatic<FirebaseMessaging>
    private val initialized = CopyOnWriteArrayList<String>()

    @Before fun setup() {
        PushPlatform::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        for (name in listOf("com.pushplatform.bootstrap", "com.pushplatform.sdk_prefs")) {
            context.getSharedPreferences(name, 0).edit().clear().commit()
        }
        oldServer = MockWebServer().also { it.start() }
        server = MockWebServer().also { it.start() }
        val messaging = mock<FirebaseMessaging>()
        whenever(messaging.token).thenReturn(TaskCompletionSource<String>().task)
        firebase = Mockito.mockStatic(FirebaseMessaging::class.java)
        firebase.`when`<FirebaseMessaging> { FirebaseMessaging.getInstance() }.thenReturn(messaging)
        sdk = PushPlatform.getInstance()
        sdk.delegate = object : PushPlatformDelegate {
            override fun didInitialize(installationId: String) { initialized += installationId }
        }
    }

    @After fun teardown() {
        sdk.delegate = null
        firebase.close()
        oldServer.shutdown()
        server.shutdown()
    }

    private fun response(id: String) = MockResponse().setResponseCode(201).setBody("{\"id\":\"$id\"}")
    private fun url(target: MockWebServer = server) = target.url("/").toString().removeSuffix("/")
    private fun options(app: String = "app", target: MockWebServer = server) = PushPlatform.InitializeOptions(app, url(target))
    private fun save(app: String = "app", target: MockWebServer = oldServer) {
        RuntimeConfigurationStore(context).save(PushConfiguration("", applicationId = app,
            apiBaseUrl = url(target), reportedEnvironment = null))
    }
    private fun await(check: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!check() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Expected state within five seconds", check())
    }

    @Test fun `explicit URL replaces completed automatic restoration`() {
        save(); oldServer.enqueue(response("old")); assertTrue(sdk.restore(context))
        await { sdk.getInstallationId() == "old" }
        server.enqueue(response("new"))
        var result: Result<String>? = null
        sdk.initialize(context, options()) { result = it }
        await { result != null }
        assertEquals("new", result!!.getOrThrow())
        assertEquals(url(), sdk.getServerURL())
        assertEquals(url(), RuntimeConfigurationStore(context).load()!!.apiBaseUrl)
    }

    @Test fun `explicit application replaces restored application and keeps device identity`() {
        save("old-app", server); server.enqueue(response("old")); sdk.restore(context)
        await { sdk.getInstallationId() == "old" }
        val storage = com.pushplatform.sdk.core.SecureStorage(context)
        val deviceId = storage.getString("installation_id")
        storage.putString("subscription_id", "old-subscription")
        server.enqueue(response("new"))
        var result: Result<String>? = null
        sdk.initialize(context, options("new-app")) { result = it }
        await { result != null }
        assertEquals("new", result!!.getOrThrow())
        assertNull(storage.getString("subscription_id"))
        assertEquals(deviceId, storage.getString("installation_id"))
        assertEquals("new-app", RuntimeConfigurationStore(context).load()!!.applicationId)
    }

    @Test fun `late restored response cannot overwrite explicit configuration or complete callbacks twice`() {
        save(); oldServer.enqueue(response("old").setBodyDelay(400, TimeUnit.MILLISECONDS))
        sdk.restore(context)
        assertNotNull(oldServer.takeRequest(3, TimeUnit.SECONDS))
        server.enqueue(response("new"))
        val results = CopyOnWriteArrayList<Result<String>>()
        sdk.initialize(context, options()) { results += it }
        await { results.isNotEmpty() }
        assertEquals("new", results.single().getOrThrow())
        Thread.sleep(500)
        assertEquals("new", sdk.getInstallationId())
        assertEquals(listOf("new"), initialized.toList())
        assertEquals(1, results.size)
    }

    @Test fun `matching explicit initialize joins restoration and applies runtime options`() {
        save(target = server); server.enqueue(response("same").setBodyDelay(200, TimeUnit.MILLISECONDS))
        sdk.restore(context)
        val results = CopyOnWriteArrayList<Result<String>>()
        sdk.initialize(context, options().copy(debugMode = true, foregroundBannerFallback = 7.0)) { results += it }
        sdk.initialize(context, options()) { results += it }
        await { results.size == 2 }
        assertTrue(results.all { it.getOrThrow() == "same" })
        assertEquals(1, server.requestCount)
        assertEquals(7.0, RuntimeConfigurationStore(context).load()!!.foregroundBannerFallbackSeconds, 0.0)
    }

    @Test fun `two different explicit configurations remain an error`() {
        server.enqueue(response("first"))
        sdk.initialize(context, options())
        var conflict: Result<String>? = null
        sdk.initialize(context, options("different")) { conflict = it }
        assertTrue(conflict!!.exceptionOrNull() is IllegalStateException)
        await { sdk.getInstallationId() == "first" }
        assertEquals(1, server.requestCount)
    }

    @Test fun `invalid explicit options do not consume automatic restoration`() {
        save(); oldServer.enqueue(response("old")); sdk.restore(context)
        var invalid: Result<String>? = null
        sdk.initialize(context, options().copy(foregroundBannerFallback = Double.NaN)) { invalid = it }
        assertTrue(invalid!!.isFailure)
        server.enqueue(response("valid"))
        var valid: Result<String>? = null
        sdk.initialize(context, options()) { valid = it }
        await { valid != null }
        assertEquals("valid", valid!!.getOrThrow())
    }

    @Test fun `missing URL replaces restored custom server with production default`() {
        save(); oldServer.enqueue(response("old")); sdk.restore(context)
        await { sdk.getInstallationId() == "old" }
        Mockito.mockConstruction(ApiClient::class.java).use { clients ->
            sdk.initialize(context, "app")
            assertEquals(PushPlatform.DEFAULT_API_BASE_URL, sdk.getServerURL())
            assertEquals(PushPlatform.DEFAULT_API_BASE_URL, RuntimeConfigurationStore(context).load()!!.apiBaseUrl)
            assertEquals(1, clients.constructed().size)
        }
    }

    @Test fun `explicit environment can replace restored environment`() {
        save(target = server); server.enqueue(response("old")); sdk.restore(context)
        await { sdk.getInstallationId() == "old" }
        server.enqueue(response("development"))
        var result: Result<String>? = null
        sdk.initialize(context, options().copy(environment = Environment.DEVELOPMENT)) { result = it }
        await { result != null }
        assertEquals("development", result!!.getOrThrow())
        assertEquals("development", RuntimeConfigurationStore(context).load()!!.reportedEnvironment)
    }

    @Test fun `old subscription response cannot overwrite the new subscription`() {
        val messaging = mock<FirebaseMessaging>()
        whenever(messaging.token).thenReturn(com.google.android.gms.tasks.Tasks.forResult("token"))
        firebase.`when`<FirebaseMessaging> { FirebaseMessaging.getInstance() }.thenReturn(messaging)
        save()
        oldServer.enqueue(response("old"))
        oldServer.enqueue(MockResponse().setResponseCode(201)
            .setBody("{\"subscription_id\":\"old-subscription\"}")
            .setBodyDelay(400, TimeUnit.MILLISECONDS))
        sdk.restore(context)
        await { oldServer.requestCount == 2 }
        server.enqueue(response("new"))
        server.enqueue(MockResponse().setResponseCode(201).setBody("{\"subscription_id\":\"new-subscription\"}"))
        sdk.initialize(context, options())
        val storage = com.pushplatform.sdk.core.SecureStorage(context)
        await { storage.getString("subscription_id") == "new-subscription" }
        Thread.sleep(500)
        assertEquals("new-subscription", storage.getString("subscription_id"))
        assertEquals("new", sdk.getInstallationId())
    }

}
