package com.example.admob_next_gen.utilities.firebase

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.example.admob_next_gen.ads.LocalAdsControl
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.nextgen.ads.control.AdsControlStore

/**
 * Feeds the ads control from Firebase Remote Config (parameter [LocalAdsControl.REMOTE_CONFIG_KEY],
 * value = the same JSON as [LocalAdsControl.JSON]).
 *
 * - [fetch] (in `Application.onCreate()`): applies the value activated on a previous launch right
 *   away, then fetches a new one and applies it when it arrives.
 * - [whenFetched]: the splash waits for the fetch (with a time limit) before choosing its ads, so a
 *   new value works from the very next launch screen.
 *
 * Without google-services.json Firebase isn't set up: nothing is fetched and the local JSON stays.
 */
object RemoteAdsControl {

    private const val TAG = "RemoteAdsControl"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val waiters = mutableListOf<() -> Unit>()
    private var isFetchDone = false

    @MainThread
    fun fetch(context: Context, isDebug: Boolean) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase not configured (no google-services.json): using the local ads control")
            return onFetchDone()
        }

        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            // Debug: always fetch fresh values. Release: at most once an hour (Firebase quota).
            minimumFetchIntervalInSeconds = if (isDebug) 0 else FETCH_INTERVAL_SECONDS
        })

        // Value activated on an earlier launch (Remote Config caches it on disk).
        applyIfFromRemote(remoteConfig, source = "remote config (cached)")

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                applyIfFromRemote(remoteConfig, source = "remote config")
            } else {
                Log.w(TAG, "Remote Config fetch failed, keeping the current ads control", task.exception)
            }
            onFetchDone()
        }
    }

    /** Runs [block] once the fetch is done, failed, or [timeoutMillis] passed, whichever comes first. */
    @MainThread
    fun whenFetched(timeoutMillis: Long, block: () -> Unit) {
        if (isFetchDone) return block()

        var hasRun = false
        val runOnce = {
            if (!hasRun) {
                hasRun = true
                block()
            }
        }
        waiters += runOnce
        mainHandler.postDelayed({
            waiters.remove(runOnce)
            runOnce()
        }, timeoutMillis)
    }

    private fun applyIfFromRemote(remoteConfig: FirebaseRemoteConfig, source: String) {
        val value = remoteConfig.getValue(LocalAdsControl.REMOTE_CONFIG_KEY)
        // STATIC/DEFAULT = parameter not published (yet): keep the local JSON.
        if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return
        AdsControlStore.update(value.asString(), source)
    }

    private fun onFetchDone() {
        isFetchDone = true
        val toRun = waiters.toList()
        waiters.clear()
        toRun.forEach { it() }
    }

    private const val FETCH_INTERVAL_SECONDS = 3_600L
}
