package com.pushplatform.sdk

import com.pushplatform.sdk.notifications.DeduplicationCache
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class DeduplicationCacheTest {

    private lateinit var cache: DeduplicationCache

    @Before
    fun setup() {
        cache = DeduplicationCache(maxEntries = 100, ttlMillis = 1000)
    }

    @Test
    fun testAdd_andContains() {
        assertFalse(cache.contains("event_1"))

        cache.add("event_1")

        assertTrue(cache.contains("event_1"))
    }

    @Test
    fun testAdd_duplicate() {
        cache.add("event_1")
        cache.add("event_1")

        assertEquals(1, cache.size())
    }

    @Test
    fun testRemove() {
        cache.add("event_1")
        assertTrue(cache.contains("event_1"))

        cache.remove("event_1")

        assertFalse(cache.contains("event_1"))
    }

    @Test
    fun testClear() {
        cache.add("event_1")
        cache.add("event_2")
        cache.add("event_3")

        assertEquals(3, cache.size())

        cache.clear()

        assertEquals(0, cache.size())
        assertFalse(cache.contains("event_1"))
        assertFalse(cache.contains("event_2"))
        assertFalse(cache.contains("event_3"))
    }

    @Test
    fun testLruEviction_at101Entries() {
        val smallCache = DeduplicationCache(maxEntries = 100)

        // Add 100 entries
        repeat(100) { i ->
            smallCache.add("event_$i")
        }

        assertEquals(100, smallCache.size())
        assertTrue(smallCache.contains("event_0"))

        // Add 101st entry, should evict oldest
        smallCache.add("event_100")

        assertEquals(100, smallCache.size())
        assertFalse(smallCache.contains("event_0"))
        assertTrue(smallCache.contains("event_100"))
    }

    @Test
    fun testLruEviction_multipleOverLimit() {
        val smallCache = DeduplicationCache(maxEntries = 10)

        // Add 15 entries
        repeat(15) { i ->
            smallCache.add("event_$i")
        }

        assertEquals(10, smallCache.size())
        assertFalse(smallCache.contains("event_0"))
        assertFalse(smallCache.contains("event_1"))
        assertFalse(smallCache.contains("event_2"))
        assertFalse(smallCache.contains("event_3"))
        assertFalse(smallCache.contains("event_4"))
        assertTrue(smallCache.contains("event_14"))
    }

    @Test
    fun testTtlExpiration() {
        val shortTtlCache = DeduplicationCache(maxEntries = 100, ttlMillis = 100)

        shortTtlCache.add("event_1")
        assertTrue(shortTtlCache.contains("event_1"))

        // Wait for expiration
        Thread.sleep(150)

        assertFalse(shortTtlCache.contains("event_1"))
    }

    @Test
    fun testTtlExpiration_cleanupOnAdd() {
        val shortTtlCache = DeduplicationCache(maxEntries = 100, ttlMillis = 100)

        shortTtlCache.add("event_1")
        assertEquals(1, shortTtlCache.size())

        // Wait for expiration
        Thread.sleep(150)

        // Add new entry, should trigger cleanup
        shortTtlCache.add("event_2")

        assertEquals(1, shortTtlCache.size())
        assertFalse(shortTtlCache.contains("event_1"))
        assertTrue(shortTtlCache.contains("event_2"))
    }

    @Test
    fun testThreadSafety_concurrentAdds() {
        val threadCount = 10
        val entriesPerThread = 10
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { threadId ->
            thread {
                repeat(entriesPerThread) { i ->
                    cache.add("event_${threadId}_$i")
                }
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(threadCount * entriesPerThread, cache.size())
    }

    @Test
    fun testThreadSafety_concurrentAddAndCheck() {
        val threadCount = 5
        val latch = CountDownLatch(threadCount * 2)
        val results = mutableListOf<Boolean>()

        // Add threads
        repeat(threadCount) { threadId ->
            thread {
                cache.add("event_$threadId")
                latch.countDown()
            }
        }

        // Check threads
        repeat(threadCount) { threadId ->
            thread {
                Thread.sleep(10) // Small delay to let adds happen
                synchronized(results) {
                    results.add(cache.contains("event_$threadId"))
                }
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(results.all { it })
    }

    @Test
    fun testMultipleEvents() {
        cache.add("event_1")
        cache.add("event_2")
        cache.add("event_3")

        assertTrue(cache.contains("event_1"))
        assertTrue(cache.contains("event_2"))
        assertTrue(cache.contains("event_3"))
        assertFalse(cache.contains("event_4"))
    }

    @Test
    fun testNamespaceCoexistence_eventIdAndCallId() {
        // event_id and call_id can coexist as they're just strings
        cache.add("event_123")
        cache.add("call_456")

        assertTrue(cache.contains("event_123"))
        assertTrue(cache.contains("call_456"))
        assertEquals(2, cache.size())
    }
}
