package com.pushplatform.sdk.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

class AppLifecycleTracker(application: Application) {

    private val activityCount = AtomicInteger(0)

    @Volatile
    private var isInForeground = false

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (activityCount.incrementAndGet() == 1) {
                    isInForeground = true
                }
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                if (activityCount.decrementAndGet() == 0) {
                    isInForeground = false
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun isAppInForeground(): Boolean = isInForeground
}
