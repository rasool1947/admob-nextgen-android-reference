package com.nextgen.ads.fullscreen

import android.app.Activity
import androidx.annotation.MainThread
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.config.AdPlacement
import com.nextgen.ads.config.AdRevenue
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.MainDispatch

/**
 * App Open, Interstitial, Rewarded and Rewarded Interstitial ads, backed by the Next-Gen preloaders.
 *
 * - [preload] once a placement may be needed soon; the SDK keeps an ad ready, refills it after
 *   every show and replaces expired ones on its own.
 * - [show] when the moment comes; [FullScreenAdListener.onAdFinished] tells you when to continue.
 * - [stop] a placement that won't be shown anymore (e.g. after onboarding).
 *
 * Each placement is its own preload id, so different placements never share or swap ads.
 * Only one full-screen ad can be on screen at a time ([isShowing]).
 * Everything here must be called on the main thread; every listener call arrives on the main thread.
 */
object FullScreenAds {

    /** Main thread only. */
    private val preloading = mutableSetOf<String>()
    private val readyWaiters = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()

    /** True while a full-screen ad from this module is on screen. */
    var isShowing: Boolean = false
        private set

    @MainThread
    fun preload(placementKey: String) {
        val placement = AdsSdk.placement(placementKey)
        val format = FullScreenFormat.of(placement.format)

        if (placementKey in preloading) return
        blockReason(placement)?.let { reason ->
            AdsLog.d("$placementKey -> preload skipped: $reason")
            return
        }

        val configuration = PreloadConfiguration(AdRequest.Builder(placement.adUnitId).build(), BUFFER_SIZE)
        if (format.start(placementKey, configuration, preloadCallback)) {
            preloading += placementKey
            AdsLog.d("$placementKey -> preload started")
        } else {
            AdsLog.w("$placementKey -> preload could not start")
        }
    }

    /** True when an ad for this placement is preloaded and can be shown right now. */
    @MainThread
    fun isReady(placementKey: String): Boolean {
        val placement = AdsSdk.placement(placementKey)
        return placementKey in preloading && FullScreenFormat.of(placement.format).isAdAvailable(placementKey)
    }

    /**
     * Calls [onResult] once: `true` as soon as an ad is ready, `false` if loading failed, the
     * placement isn't preloading (no consent / premium / disabled) or [timeoutMillis] passed.
     * Useful on a splash screen.
     */
    @MainThread
    fun whenReady(placementKey: String, timeoutMillis: Long, onResult: (isReady: Boolean) -> Unit) {
        if (isReady(placementKey)) return onResult(true)
        if (placementKey !in preloading) return onResult(false)

        val waiters = readyWaiters.getOrPut(placementKey) { mutableListOf() }
        var isDone = false
        lateinit var timeout: Runnable
        val waiter: (Boolean) -> Unit = { ready ->
            if (!isDone) {
                isDone = true
                MainDispatch.cancel(timeout)
                onResult(ready)
            }
        }
        timeout = Runnable {
            waiters.remove(waiter)
            waiter(isReady(placementKey))
        }
        waiters += waiter
        MainDispatch.postDelayed(timeoutMillis, timeout)
    }

    /**
     * Shows the next preloaded ad of this placement. [listener] always gets exactly one
     * [FullScreenAdListener.onAdFinished], whether or not an ad was shown.
     */
    @MainThread
    fun show(activity: Activity, placementKey: String, listener: FullScreenAdListener? = null) {
        val placement = AdsSdk.placement(placementKey)
        val format = FullScreenFormat.of(placement.format)

        val reason = when {
            isShowing -> "another full-screen ad is showing"
            activity.isFinishing || activity.isDestroyed -> "activity is finishing"
            else -> blockReason(placement)
        }
        if (reason != null) return notShown(placementKey, reason, listener)

        val events = eventCallback(placement, listener)
        val onReward = { reward: RewardItem ->
            MainDispatch.post {
                AdsLog.d("$placementKey -> reward earned: ${reward.amount} ${reward.type}")
                listener?.onUserEarnedReward(reward)
            }
        }

        isShowing = true
        if (!format.pollAndShow(placementKey, activity, events, onReward)) {
            isShowing = false
            notShown(placementKey, "no ad available yet", listener)
        } else {
            AdsLog.d("$placementKey -> showing")
        }
    }

    /** Stops preloading this placement and discards its buffered ads. */
    @MainThread
    fun stop(placementKey: String) {
        if (preloading.remove(placementKey)) {
            FullScreenFormat.of(AdsSdk.placement(placementKey).format).destroy(placementKey)
            resolveWaiters(placementKey, false)
            AdsLog.d("$placementKey -> preload stopped")
        }
    }

    /** Stops every placement, e.g. right after the user purchased "remove ads". */
    @MainThread
    fun stopAll() = preloading.toList().forEach(::stop)

    private fun blockReason(placement: AdPlacement): String? = when {
        !AdsSdk.canRequestAds -> "no consent"
        !AdsSdk.isInitialized.value -> "SDK not initialized"
        AdsSdk.config.isPremium() -> "premium user"
        !placement.isEnabled() -> "placement disabled"
        else -> null
    }

    private fun notShown(placementKey: String, reason: String, listener: FullScreenAdListener?) {
        AdsLog.d("$placementKey -> not shown: $reason")
        listener?.onAdFailedToShow(reason)
        listener?.onAdFinished()
    }

    private fun eventCallback(placement: AdPlacement, listener: FullScreenAdListener?) = object : AdEventCallback {
        val key = placement.key

        override fun onAdShowedFullScreenContent() = MainDispatch.post { listener?.onAdShowed() }
        override fun onAdImpression() = MainDispatch.post { listener?.onAdImpression() }
        override fun onAdClicked() = MainDispatch.post { listener?.onAdClicked() }

        override fun onAdPaid(value: AdValue) = MainDispatch.post {
            AdsSdk.config.onAdPaid?.invoke(
                AdRevenue(key, placement.format, placement.adUnitId, value.valueMicros, value.currencyCode, value.precisionType.name)
            )
        }

        override fun onAdDismissedFullScreenContent() = MainDispatch.post {
            AdsLog.d("$key -> dismissed")
            isShowing = false
            listener?.onAdDismissed()
            listener?.onAdFinished()
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = MainDispatch.post {
            AdsLog.w("$key -> failed to show: ${fullScreenContentError.message}")
            isShowing = false
            listener?.onAdFailedToShow(fullScreenContentError.message)
            listener?.onAdFinished()
        }
    }

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) = MainDispatch.post {
            AdsLog.d("$preloadId -> ad preloaded")
            resolveWaiters(preloadId, true)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) = MainDispatch.post {
            AdsLog.w("$preloadId -> failed to preload: ${adError.code} ${adError.message}")
            resolveWaiters(preloadId, false)
        }

        override fun onAdsExhausted(preloadId: String) = MainDispatch.post {
            AdsLog.d("$preloadId -> buffer empty, SDK is loading the next ad")
        }
    }

    private fun resolveWaiters(placementKey: String, ready: Boolean) {
        readyWaiters.remove(placementKey)?.forEach { it(ready) }
    }

    /** One ad in memory per placement: enough for one show, refilled right after. */
    private const val BUFFER_SIZE = 1
}
