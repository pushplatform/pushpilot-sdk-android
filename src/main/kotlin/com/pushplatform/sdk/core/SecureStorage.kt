package com.pushplatform.sdk.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pushplatform.sdk.models.SdkError
import com.pushplatform.sdk.utils.Logger

class SecureStorage(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        initializePreferences()
    }

    private fun initializePreferences(): SharedPreferences {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                Logger.debug("EncryptedSharedPreferences not available (API < 23), using plain SharedPreferences")
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        } catch (e: Exception) {
            Logger.error("Failed to initialize EncryptedSharedPreferences, falling back to plain", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getString(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            Logger.error("Failed to read key: $key", e)
            null
        }
    }

    fun putString(key: String, value: String): Boolean {
        return try {
            prefs.edit().putString(key, value).commit()
        } catch (e: Exception) {
            Logger.error("Failed to write key: $key", e)
            false
        }
    }

    fun remove(key: String): Boolean {
        return try {
            prefs.edit().remove(key).commit()
        } catch (e: Exception) {
            Logger.error("Failed to remove key: $key", e)
            false
        }
    }

    fun contains(key: String): Boolean {
        return try {
            prefs.contains(key)
        } catch (e: Exception) {
            Logger.error("Failed to check key: $key", e)
            false
        }
    }

    fun clear(): Boolean {
        return try {
            prefs.edit().clear().commit()
        } catch (e: Exception) {
            Logger.error("Failed to clear storage", e)
            false
        }
    }

    companion object {
        private const val PREFS_NAME = "com.pushplatform.sdk_prefs"
    }
}
