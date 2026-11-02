package com.pushplatform.sdk.core

import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.utils.Logger

/**
 * Sends PUT /notifications/{id} when the user taps a notification. A tap that
 * arrives before initialize, or before the subscription id is known, waits here.
 */
class NotificationOpenReporter {
    private val lock = Any()
    private val pending = LinkedHashSet<String>()
    private var apiClient: ApiClient? = null
    private var applicationId: String? = null
    private var subscriptionId: () -> String? = { null }

    fun report(notificationId: String) {
        if (notificationId.isEmpty()) return
        synchronized(lock) { enqueue(notificationId) }
        flush()
    }

    fun start(apiClient: ApiClient, applicationId: String, subscriptionId: () -> String?) {
        synchronized(lock) {
            this.apiClient = apiClient
            this.applicationId = applicationId
            this.subscriptionId = subscriptionId
        }
        flush()
    }

    fun flush() {
        val ready = synchronized(lock) {
            val client = apiClient ?: return
            val appId = applicationId ?: return
            if (pending.isEmpty()) return
            val subscription = subscriptionId() ?: return
            val ids = pending.toList()
            pending.clear()
            Triple(client, appId, subscription) to ids
        }
        val (target, ids) = ready
        val (client, appId, subscription) = target
        ids.forEach { id ->
            client.reportOpen(id, appId, subscription) { result ->
                if (result is ApiClient.Result.Failure) {
                    Logger.debug("Notification open was not recorded: ${result.error.message}")
                    // Repeated opens are not counted twice, so a network failure is retried on the next flush.
                    if (result.error is SdkError.NetworkError) synchronized(lock) { enqueue(id) }
                }
            }
        }
    }

    /** Notification ids not yet sent to the server. */
    fun pendingIds(): List<String> = synchronized(lock) { pending.toList() }

    private fun enqueue(notificationId: String) {
        pending.add(notificationId)
        while (pending.size > MAX_PENDING) pending.remove(pending.first())
    }

    private companion object {
        const val MAX_PENDING = 50
    }
}
