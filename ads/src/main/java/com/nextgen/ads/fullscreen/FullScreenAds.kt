package com.nextgen.ads.fullscreen

import android.app.Activity
import android.os.SystemClock
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
import com.nextgen.ads.internal.AdsFlowLog
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.MainDispatch
import com.nextgen.ads.internal.PreloadWaiters

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
    private val readyWaiters = PreloadWaiters()

    /** True while a full-screen ad from this module is on screen. */
    var isShowing: Boolean = false
        private set

    /** [SystemClock.elapsedRealtime] of the last time any full-screen ad appeared, 0 if none yet. Used for frequency caps. */
    var lastShownAtMillis: Long = 0L
        private set

    /** Placement whose show is in progress (loading dialog or on screen); repeated calls for it are ignored. */
    private var activeKey: String? = null

    @MainThread
    fun preload(placementKey: String) {
        val placement = AdsSdk.placement(placementKey)
        FullScreenFormat.of(placement.format) // fails fast for a non-full-screen placement
        AdsSdk.whenSdkReady { startPreload(placement) }
    }

    private fun startPreload(placement: AdPlacement) {
        val placementKey = placement.key
        val format = FullScreenFormat.of(placement.format)

        if (placementKey in preloading) return
        AdsSdk.blockReason(placement)?.let { reason ->
            AdsLog.d("$placementKey -> preload skipped: $reason")
            AdsFlowLog.log(placement, AdsFlowLog.Event.SKIPPED, "preload: $reason")
            return
        }

        val configuration = PreloadConfiguration(AdRequest.Builder(placement.adUnitId).build(), BUFFER_SIZE)
        if (format.start(placementKey, configuration, preloadCallback)) {
            preloading += placementKey
            AdsLog.d("$placementKey -> preload started")
            AdsFlowLog.log(placement, AdsFlowLog.Event.PRELOADING)
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
        AdsSdk.whenSdkReady {
            when {
                isReady(placementKey) -> onResult(true)
                placementKey !in preloading -> onResult(false)
                else -> readyWaiters.await(placementKey, timeoutMillis, { isReady(placementKey) }, onResult)
            }
        }
    }

    /**
     * Shows the next preloaded ad of this placement. [listener] always gets exactly one
     * [FullScreenAdListener.onAdFinished], whether or not an ad was shown. A second call for a
     * placement that is already being shown (e.g. a double tap) is ignored and gets no callbacks.
     */
    @MainThread
    fun show(activity: Activity, placementKey: String, listener: FullScreenAdListener? = null) {
        if (isDuplicate(placementKey)) return
        if (activeKey == null) activeKey = placementKey
        // Normally right away; waits only while the consent form is being shown again at launch.
        AdsSdk.whenSdkReady { showNow(activity, placementKey, listener) }
    }

    private fun showNow(activity: Activity, placementKey: String, listener: FullScreenAdListener?) {
        val placement = AdsSdk.placement(placementKey)
        val format = FullScreenFormat.of(placement.format)

        val reason = when {
            isShowing -> "another full-screen ad is showing"
            activity.isFinishing || activity.isDestroyed -> "activity is finishing"
            else -> AdsSdk.blockReason(placement)
        }
        if (reason != null) return notShown(placementKey, reason, listener)

        val events = eventCallback(placement, listener)
        val onReward = { reward: RewardItem ->
            MainDispatch.post {
                AdsLog.d("$placementKey -> reward earned: ${reward.amount} ${reward.type}")
                AdsFlowLog.log(placement, AdsFlowLog.Event.REWARDED, "${reward.amount} ${reward.type}")
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

    /**
     * Like [show], but first shows a "Loading ad…" dialog for [loadingMillis], so the ad doesn't pop
     * up out of nowhere. Without a ready ad there is no dialog: [listener] finishes right away.
     */
    @MainThread
    fun showWithLoading(
        activity: Activity,
        placementKey: String,
        listener: FullScreenAdListener? = null,
        loadingMillis: Long = LOADING_DIALOG_MILLIS,
    ) {
        showAfterDialog(activity, placementKey, listener, loadingMillis) { AdWaitDialog.loading(activity) }
    }

    /** Shows [dialog] for [waitMillis], then the ad; the dialog closes when the ad appears or the show ends. */
    internal fun showAfterDialog(
        activity: Activity,
        placementKey: String,
        listener: FullScreenAdListener?,
        waitMillis: Long,
        dialog: () -> AdWaitDialog,
    ) {
        if (isDuplicate(placementKey)) return
        if (isShowing || !isReady(placementKey)) return show(activity, placementKey, listener) // reports why not
        activeKey = placementKey

        val waitDialog = dialog().also { it.show() }
        val delegate = listener ?: object : FullScreenAdListener {}
        MainDispatch.postDelayed(waitMillis) {
            showNow(activity, placementKey, object : FullScreenAdListener by delegate {
                override fun onAdShowed() {
                    waitDialog.dismissSafely()
                    delegate.onAdShowed()
                }

                override fun onAdFinished() {
                    waitDialog.dismissSafely()
                    delegate.onAdFinished()
                }
            })
        }
    }

    /** Stops preloading this placement and discards its buffered ads. */
    @MainThread
    fun stop(placementKey: String) {
        if (preloading.remove(placementKey)) {
            FullScreenFormat.of(AdsSdk.placement(placementKey).format).destroy(placementKey)
            readyWaiters.resolve(placementKey, false)
            AdsLog.d("$placementKey -> preload stopped")
        }
    }

    /** Stops every placement, e.g. right after the user purchased "remove ads". */
    @MainThread
    fun stopAll() = preloading.toList().forEach(::stop)

    private fun isDuplicate(placementKey: String): Boolean {
        val isDuplicate = activeKey == placementKey
        if (isDuplicate) AdsLog.d("$placementKey -> ignored: already showing (e.g. a double tap)")
        return isDuplicate
    }

    private fun release(placementKey: String) {
        if (activeKey == placementKey) activeKey = null
    }

    private fun notShown(placementKey: String, reason: String, listener: FullScreenAdListener?) {
        release(placementKey)
        AdsLog.d("$placementKey -> not shown: $reason")
        AdsFlowLog.log(AdsSdk.placement(placementKey), AdsFlowLog.Event.NOT_SHOWN, reason)
        listener?.onAdFailedToShow(reason)
        listener?.onAdFinished()
    }

    private fun eventCallback(placement: AdPlacement, listener: FullScreenAdListener?) = object : AdEventCallback {
        val key = placement.key

        override fun onAdShowedFullScreenContent() = MainDispatch.post {
            lastShownAtMillis = SystemClock.elapsedRealtime()
            listener?.onAdShowed()
        }
        override fun onAdImpression() = MainDispatch.post {
            AdsFlowLog.log(placement, AdsFlowLog.Event.IMPRESSION)
            listener?.onAdImpression()
        }
        override fun onAdClicked() = MainDispatch.post {
            AdsFlowLog.log(placement, AdsFlowLog.Event.CLICKED)
            listener?.onAdClicked()
        }

        override fun onAdPaid(value: AdValue) = AdsSdk.reportPaid(placement, value)

        override fun onAdDismissedFullScreenContent() = MainDispatch.post {
            AdsLog.d("$key -> dismissed")
            AdsFlowLog.log(placement, AdsFlowLog.Event.CLOSED)
            release(key)
            isShowing = false
            listener?.onAdDismissed()
            listener?.onAdFinished()
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = MainDispatch.post {
            AdsLog.w("$key -> failed to show: ${fullScreenContentError.message}")
            AdsFlowLog.log(placement, AdsFlowLog.Event.FAILED, "show: ${fullScreenContentError.message}")
            release(key)
            isShowing = false
            listener?.onAdFailedToShow(fullScreenContentError.message)
            listener?.onAdFinished()
        }
    }

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) = MainDispatch.post {
            AdsLog.d("$preloadId -> ad preloaded")
            AdsFlowLog.log(AdsSdk.placement(preloadId), AdsFlowLog.Event.LOADED_IN_CACHE)
            readyWaiters.resolve(preloadId, true)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) = MainDispatch.post {
            AdsLog.w("$preloadId -> failed to preload: ${adError.code} ${adError.message}")
            AdsFlowLog.log(AdsSdk.placement(preloadId), AdsFlowLog.Event.FAILED, "preload: ${adError.code}: ${adError.message}")
            readyWaiters.resolve(preloadId, false)
        }

        override fun onAdsExhausted(preloadId: String) = MainDispatch.post {
            AdsLog.d("$preloadId -> buffer empty, SDK is loading the next ad")
        }
    }

    /** One ad in memory per placement: enough for one show, refilled right after. */
    private const val BUFFER_SIZE = 1

    /** How long [showWithLoading] shows its dialog before the ad. */
    const val LOADING_DIALOG_MILLIS = 1_000L
}
