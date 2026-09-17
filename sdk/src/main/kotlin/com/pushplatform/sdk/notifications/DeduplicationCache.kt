package com.pushplatform.sdk.notifications

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DeduplicationCache(
    private val maxEntries: Int = 100,
    private val ttlMillis: Long = 24 * 60 * 60 * 1000L // 24 hours
) {

    private data class CacheEntry(
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val lock = ReentrantReadWriteLock()
    private val insertionOrder = mutableListOf<String>()

    fun contains(eventId: String): Boolean = lock.read {
        val entry = cache[eventId] ?: return false
        val age = System.currentTimeMillis() - entry.timestamp

        if (age > ttlMillis) {
            // Entry expired
            return false
        }

        return true
    }

    fun add(eventId: String) = lock.write {
        // Remove expired entries first
        cleanupExpiredEntries()

        // Check if already exists
        if (cache.containsKey(eventId)) {
            return
        }

        // Add new entry
        cache[eventId] = CacheEntry(System.currentTimeMillis())
        insertionOrder.add(eventId)

        // Enforce LRU limit
        if (insertionOrder.size > maxEntries) {
            val oldest = insertionOrder.removeAt(0)
            cache.remove(oldest)
        }
    }

    fun remove(eventId: String) = lock.write {
        cache.remove(eventId)
        insertionOrder.remove(eventId)
    }

    fun clear() = lock.write {
        cache.clear()
        insertionOrder.clear()
    }

    fun size(): Int = lock.read {
        cache.size
    }

    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { (_, entry) -> now - entry.timestamp > ttlMillis }
            .map { it.key }

        expiredKeys.forEach { key ->
            cache.remove(key)
            insertionOrder.remove(key)
        }
    }
}
