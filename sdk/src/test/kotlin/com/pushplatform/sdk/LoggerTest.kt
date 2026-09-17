package com.pushplatform.sdk

import com.pushplatform.sdk.utils.Logger
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoggerTest {

    @Before
    fun setup() {
        Logger.debugMode = false
    }

    @Test
    fun testTokenMasked_masksLongToken() {
        val token = "eE4jB7kLmN8pQ2rT5vW9yZ3aBcDeFgHiJkLmNoPqRsTuVwXyZ"

        val masked = Logger.tokenMasked(token)

        assertEquals("eE4jB7kL...", masked)
    }

    @Test
    fun testTokenMasked_masksExactly8Characters() {
        val token = "12345678abcdefgh"

        val masked = Logger.tokenMasked(token)

        assertEquals("12345678...", masked)
    }

    @Test
    fun testTokenMasked_masksShortToken() {
        val token = "short"

        val masked = Logger.tokenMasked(token)

        assertEquals("***", masked)
    }

    @Test
    fun testTokenMasked_masksEmptyToken() {
        val token = ""

        val masked = Logger.tokenMasked(token)

        assertEquals("***", masked)
    }

    @Test
    fun testTokenMasked_masksExactly8CharToken() {
        val token = "exactly8"

        val masked = Logger.tokenMasked(token)

        assertEquals("***", masked)
    }

    @Test
    fun testTokenMasked_masksExactly9CharToken() {
        val token = "exactly_9"

        val masked = Logger.tokenMasked(token)

        assertEquals("exactly_...", masked)
    }

    @Test
    fun testDebugMode_defaultsToFalse() {
        assertFalse(Logger.debugMode)
    }

    @Test
    fun testDebugMode_canBeEnabled() {
        Logger.debugMode = true

        assertTrue(Logger.debugMode)
    }

    @Test
    fun testDebugMode_canBeDisabled() {
        Logger.debugMode = true
        Logger.debugMode = false

        assertFalse(Logger.debugMode)
    }

    @Test
    fun testDebug_doesNotCrashWhenDisabled() {
        Logger.debugMode = false

        Logger.debug("This should not crash")
    }

    @Test
    fun testInfo_doesNotCrashWhenDisabled() {
        Logger.debugMode = false

        Logger.info("This should not crash")
    }

    @Test
    fun testError_doesNotCrashWhenDisabled() {
        Logger.debugMode = false

        Logger.error("This should not crash")
    }

    @Test
    fun testError_withThrowable_doesNotCrash() {
        Logger.debugMode = true

        Logger.error("Error message", RuntimeException("Test exception"))
    }
}
