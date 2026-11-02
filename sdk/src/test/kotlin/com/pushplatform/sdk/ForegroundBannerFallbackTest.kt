package com.pushplatform.sdk

import com.pushplatform.sdk.notifications.ParsedNotification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ForegroundBannerFallbackTest {
    private fun notification() = ParsedNotification("Hello", "Body", null, null, null, null, null, emptyMap())

    @Test
    fun zeroWaitsWithoutShowingTheBanner() {
        val display = NotificationDisplay(notification())
        display.deferDecision()
        val pending = CompletableFuture<Boolean>()
        Thread { pending.complete(display.awaitDecision(null)) }.start()
        try {
            pending.get(150, TimeUnit.MILLISECONDS)
            throw AssertionError("awaitDecision returned without finish()")
        } catch (_: TimeoutException) {
            // The handler has not returned, so the system banner stays hidden.
        }
        display.finish()
        assertFalse(pending.get(1, TimeUnit.SECONDS))
    }

    @Test
    fun positiveFallbackShowsTheBannerWhenFinishIsNotCalled() {
        val display = NotificationDisplay(notification())
        display.deferDecision()
        assertFalse(display.awaitDecision(50))
    }

    @Test
    fun preventDefaultBeforeTheFallbackHidesTheBanner() {
        val display = NotificationDisplay(notification())
        display.deferDecision()
        display.preventDefault()
        assertTrue(display.awaitDecision(200))
    }

    @Test
    fun finishBeforeTheFallbackDecidesImmediately() {
        val display = NotificationDisplay(notification())
        display.deferDecision()
        display.preventDefault()
        display.finish()
        assertTrue(display.awaitDecision(200))
    }
}
