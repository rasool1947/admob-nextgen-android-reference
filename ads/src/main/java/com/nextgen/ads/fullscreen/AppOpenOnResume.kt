package com.nextgen.ads.fullscreen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.MainDispatch
import java.lang.ref.WeakReference

/**
 * Shows an App Open ad when the user brings the app back to the foreground.
 *
 * Call [enable] once the launch flow (splash / consent) is over; it does nothing while the ads
 * control has `app_open_resume` off. The cold-start App Open ad is the splash screen's job
 * ([FullScreenAds.show]); this only covers returning from background.
 * It never shows on top of another full-screen ad (e.g. when coming back from an ad click).
 */
object AppOpenOnResume {

    private var placementKey: String? = null
    private var currentActivity: WeakReference<Activity>? = null
    private var skipNextResume = false
    private var isRegistered = false

    @MainThread
    fun enable(placementKey: String) {
        this.placementKey = placementKey
        if (!AdsControlStore.current.appOpenOnResume) {
            AdsLog.d("$placementKey -> app open on resume is off (ads control)")
            return
        }
        FullScreenAds.preload(placementKey)
        AdsLog.d("$placementKey -> app open on resume enabled")
    }

    @MainThread
    fun disable() {
        placementKey?.let { FullScreenAds.stop(it) }
        placementKey = null
    }

    /**
     * Don't show on the next return to the app. Call before leaving on purpose, e.g. opening a
     * share sheet, the Play billing flow or a permission screen.
     */
    @MainThread
    fun skipNextResume() {
        skipNextResume = true
    }

    /** Called by AdsSdk.configure() so the current Activity is known before the first resume. */
    internal fun register(application: Application) {
        if (isRegistered) return
        isRegistered = true
        application.registerActivityLifecycleCallbacks(activityTracker)
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundObserver)
    }

    private val foregroundObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // Post: on some devices the process ON_START arrives before the Activity's onStart.
            MainDispatch.handler.post { onAppForegrounded() }
        }
    }

    private fun onAppForegrounded() {
        val key = placementKey ?: return
        if (!AdsControlStore.current.appOpenOnResume) return // ads control `app_open_resume`
        if (skipNextResume) {
            skipNextResume = false
            AdsLog.d("$key -> resume skipped once")
            return
        }
        if (FullScreenAds.isShowing) return

        // Starts preloading if it couldn't before (e.g. consent was granted later); no-op otherwise.
        FullScreenAds.preload(key)
        val activity = currentActivity?.get() ?: return
        if (!FullScreenAds.isReady(key)) {
            AdsLog.d("$key -> resume: no ad ready")
            return
        }
        FullScreenAds.show(activity, key)
    }

    private val activityTracker = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            currentActivity = WeakReference(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity?.get() === activity) currentActivity = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    }
}
