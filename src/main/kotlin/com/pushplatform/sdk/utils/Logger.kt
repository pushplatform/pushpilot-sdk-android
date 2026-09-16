package com.pushplatform.sdk.utils

import android.util.Log

object Logger {
    private const val TAG = "PushPlatform"
    var debugMode = false

    fun debug(message: String) {
        if (debugMode) {
            Log.d(TAG, message)
        }
    }

    fun info(message: String) {
        if (debugMode) {
            Log.i(TAG, message)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (debugMode) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }

    fun tokenMasked(token: String): String {
        return if (token.length > 8) {
            "${token.take(8)}..."
        } else {
            "***"
        }
    }
}
