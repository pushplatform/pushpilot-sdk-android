package com.pushplatform.sdk.core

import com.pushplatform.sdk.utils.Logger
import java.util.UUID

class InstallationManager(private val storage: SecureStorage) {

    fun getOrCreateInstallationId(): String {
        val existingId = storage.getString(KEY_INSTALLATION_ID)

        if (existingId != null) {
            Logger.debug("Retrieved existing installation ID: $existingId")
            return existingId
        }

        val newId = UUID.randomUUID().toString()
        val saved = storage.putString(KEY_INSTALLATION_ID, newId)

        if (!saved) {
            Logger.error("Failed to save installation ID")
        } else {
            Logger.debug("Generated new installation ID: $newId")
        }

        return newId
    }

    fun getInstallationId(): String? {
        return storage.getString(KEY_INSTALLATION_ID)
    }

    fun saveBackendInstallationId(installationId: String): Boolean {
        return storage.putString(KEY_BACKEND_INSTALLATION_ID, installationId)
    }

    fun getBackendInstallationId(): String? {
        return storage.getString(KEY_BACKEND_INSTALLATION_ID)
    }

    fun clearInstallationId(): Boolean {
        return storage.remove(KEY_INSTALLATION_ID)
    }

    companion object {
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_BACKEND_INSTALLATION_ID = "backend_installation_id"
    }
}
