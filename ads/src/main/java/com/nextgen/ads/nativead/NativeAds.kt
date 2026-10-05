package com.nextgen.ads.nativead

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement
import com.nextgen.ads.config.forScreen
import com.nextgen.ads.internal.AdsFlowLog
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.KeptAds
import com.nextgen.ads.internal.MainDispatch
import com.nextgen.ads.internal.PreloadWaiters
import com.nextgen.ads.internal.ShownAds

/**
 * Native ads bound to a screen's lifecycle.
 *
 * ```
 * NativeAds.loadInto(binding.nativeAd, viewLifecycleOwner, "native_home")
 * ```
 * Each load() is independent: a placement never borrows another placement's ad, and every ad
 * is destroyed when its lifecycle is destroyed (pass the Fragment's `viewLifecycleOwner`).
 *
 * Optional [preload]: start loading a placement one screen early (e.g. from the splash) so it
 * shows instantly; load() then takes the preloaded ad. [stop] it once it's no longer needed.
 */
object NativeAds {

    /** Main thread only. */
    private val preloading = mutableSetOf<String>()
    private val readyWaiters = PreloadWaiters()

    /** Screen each preload is for (log label), e.g. the next onboarding page. Main thread only. */
    private val preloadScreens = mutableMapOf<String, String?>()

    /**
     * @param screen     Log label of the screen this ad is for, if not the placement's own (e.g. "OB3").
     * @param bufferSize Ads kept ready. 2 when several screens in a row use this placement (onboarding
     *                   pages), so the next one is ready while the current one shows.
     */
    @MainThread
    fun preload(placementKey: String, screen: String? = null, bufferSize: Int = BUFFER_SIZE) {
        val placement = nativePlacement(placementKey)
        val isNewScreen = preloadScreens.put(placementKey, screen) != screen
        AdsSdk.whenSdkReady { startPreload(placement.forScreen(screen), isNewScreen, bufferSize) }
    }

    private fun startPreload(placement: AdPlacement, isNewScreen: Boolean, bufferSize: Int) {
        val placementKey = placement.key
        if (placementKey in preloading) {
            // Same cache, now filling for another screen (e.g. the next onboarding page): say so in the log.
            if (isNewScreen) {
                AdsFlowLog.log(placement, AdsFlowLog.Event.PRELOADING)
                if (NativeAdPreloader.isAdAvailable(placementKey)) AdsFlowLog.log(placement, AdsFlowLog.Event.READY_IN_CACHE)
            }
            return
        }
        AdsSdk.blockReason(placement)?.let { reason ->
            AdsLog.d("$placementKey -> native preload skipped: $reason")
            AdsFlowLog.log(placement, AdsFlowLog.Event.SKIPPED, "preload: $reason")
            return
        }
        val configuration = PreloadConfiguration(request(placement), bufferSize)
        if (NativeAdPreloader.start(placementKey, configuration, preloadCallback)) {
            preloading += placementKey
            AdsLog.d("$placementKey -> native preload started")
            AdsFlowLog.log(placement, AdsFlowLog.Event.PRELOADING)
        }
    }

    @MainThread
    fun stop(placementKey: String) {
        if (preloading.remove(placementKey)) {
            NativeAdPreloader.destroy(placementKey)
            preloadScreens.remove(placementKey)
            readyWaiters.resolve(placementKey, false)
            AdsLog.d("$placementKey -> native preload stopped")
        }
    }

    @MainThread
    fun isReady(placementKey: String): Boolean =
        placementKey in preloading && NativeAdPreloader.isAdAvailable(placementKey)

    /** Like FullScreenAds.whenReady: `true` once a preloaded ad is available, `false` on failure/timeout. */
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
     * Loads an ad and binds it into [view]; hides the view if there is no ad.
     * @param screen Log label of the screen, if not the placement's own (e.g. "OB3").
     */
    @MainThread
    fun loadInto(
        view: NativeAdTemplateView,
        lifecycleOwner: LifecycleOwner,
        placementKey: String,
        listener: NativeAdListener? = null,
        screen: String? = null,
    ) {
        view.showPlaceholder()
        load(lifecycleOwner, placementKey, screen = screen, listener = object : NativeAdListener {
            override fun onAdLoaded(ad: NativeAd) {
                view.bind(ad)
                listener?.onAdLoaded(ad)
            }

            override fun onAdFailedToLoad(reason: String) {
                view.hide()
                listener?.onAdFailedToLoad(reason)
            }

            override fun onAdClicked() {
                listener?.onAdClicked()
            }
        })
    }

    /** Loads an ad for your own NativeAdView layout. */
    @MainThread
    fun load(lifecycleOwner: LifecycleOwner, placementKey: String, listener: NativeAdListener, screen: String? = null) {
        val placement = nativePlacement(placementKey).forScreen(screen)
        AdsSdk.whenSdkReady { loadWhenReady(lifecycleOwner, placement, listener) }
    }

    private fun loadWhenReady(lifecycleOwner: LifecycleOwner, placement: AdPlacement, listener: NativeAdListener) {
        val placementKey = placement.key
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        AdsSdk.blockReason(placement)?.let { reason ->
            AdsFlowLog.log(placement, AdsFlowLog.Event.SKIPPED, reason)
            return failed(placement, reason, listener)
        }

        // The ad this screen had last time (kept when it closed): show it again at once. If it was seen a
        // while ago, also request a new one that replaces it when it arrives (KeptAds).
        KeptAds.take<NativeAd>(placementKey, variant = null)?.let { kept ->
            AdsLog.d("$placementKey -> native kept from last time (refresh = ${kept.needsRefresh})")
            if (!kept.needsRefresh) AdsFlowLog.log(placement, AdsFlowLog.Event.REUSED, KeptAds.agoText(kept.seenAgoMillis))
            deliver(lifecycleOwner, placement, kept.ad, listener, kept.times)
            if (kept.needsRefresh) ShownAds.refreshIfSeenLongAgo(lifecycleOwner) // logs REFRESHING
            return
        }

        when {
            isReady(placementKey) -> pollPreloaded(lifecycleOwner, placement, listener)
            placementKey in preloading -> {
                // Still loading from an earlier preload(): wait for it rather than sending a second request.
                AdsLog.d("$placementKey -> native waiting for preload")
                whenReady(placementKey, PRELOAD_WAIT_MILLIS) { ready ->
                    when (ready) {
                        true -> pollPreloaded(lifecycleOwner, placement, listener)
                        false -> loadNow(lifecycleOwner, placement, listener)
                    }
                }
            }
            else -> loadNow(lifecycleOwner, placement, listener)
        }
    }

    private fun pollPreloaded(lifecycleOwner: LifecycleOwner, placement: AdPlacement, listener: NativeAdListener) {
        val ad = (NativeAdPreloader.pollAd(placement.key) as? NativeAdLoadResult.NativeAdSuccess)?.ad
        when (ad) {
            null -> loadNow(lifecycleOwner, placement, listener)
            else -> {
                AdsLog.d("${placement.key} -> native from preload")
                AdsFlowLog.log(placement, AdsFlowLog.Event.FROM_CACHE)
                deliver(lifecycleOwner, placement, ad, listener)
            }
        }
    }

    private fun loadNow(lifecycleOwner: LifecycleOwner, placement: AdPlacement, listener: NativeAdListener) {
        AdsLog.d("${placement.key} -> native loading")
        AdsFlowLog.log(placement, AdsFlowLog.Event.LOADING)
        NativeAdLoader.load(request(placement), object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) = MainDispatch.post {
                AdsFlowLog.log(placement, AdsFlowLog.Event.LOADED)
                deliver(lifecycleOwner, placement, nativeAd, listener)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) = MainDispatch.post {
                AdsFlowLog.log(placement, AdsFlowLog.Event.FAILED, "${adError.code}: ${adError.message}")
                failed(placement, "${adError.code}: ${adError.message}", listener)
            }
        })
    }

    /**
     * The ad on screen was seen a while ago (kept from last time, or a tab shown again): load a new one
     * (from the preload cache if it has one) and swap it in. On failure the old ad simply stays. If the
     * screen closes first, the new, never-seen ad is the one kept for the screen's return.
     */
    private fun refresh(
        lifecycleOwner: LifecycleOwner,
        placement: AdPlacement,
        listener: NativeAdListener,
        old: Shown,
        onDone: () -> Unit = {},
    ) {
        AdsSdk.blockReason(placement)?.let { return onDone() }
        fun swapIn(ad: NativeAd) {
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                KeptAds.keep(placement.key, ad, variant = null, KeptAds.AdTimes()) { ad.destroy() } // replaces the old one
                AdsLog.d("${placement.key} -> native refresh arrived after the screen closed: kept")
                return
            }
            lifecycleOwner.lifecycle.removeObserver(old.observer)
            AdsFlowLog.log(placement, AdsFlowLog.Event.SWAPPED)
            deliver(lifecycleOwner, placement, ad, listener)
            old.ad.destroy()
            onDone()
        }

        if (isReady(placement.key)) {
            (NativeAdPreloader.pollAd(placement.key) as? NativeAdLoadResult.NativeAdSuccess)?.ad?.let { return swapIn(it) }
        }
        AdsLog.d("${placement.key} -> native refreshing")
        NativeAdLoader.load(request(placement), object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) = MainDispatch.post { swapIn(nativeAd) }

            override fun onAdFailedToLoad(adError: LoadAdError) = MainDispatch.post {
                AdsFlowLog.log(placement, AdsFlowLog.Event.FAILED, "refresh: ${adError.code}: ${adError.message} (old ad stays)")
                onDone()
            }
        })
    }

    /** An ad on screen, and the observer that keeps it when the screen closes. */
    private class Shown(val ad: NativeAd, val observer: LifecycleObserver)

    /**
     * Main thread. Hands the ad to the caller and ties its lifetime to [lifecycleOwner].
     * Returns null when the screen is already gone (the ad is then kept for its return).
     */
    private fun deliver(
        lifecycleOwner: LifecycleOwner,
        placement: AdPlacement,
        ad: NativeAd,
        listener: NativeAdListener,
        times: KeptAds.AdTimes = KeptAds.AdTimes(),
    ): Shown? {
        val lifecycle = lifecycleOwner.lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            KeptAds.keep(placement.key, ad, variant = null, times) { ad.destroy() } // screen closed while loading
            return null
        }

        ad.adEventCallback = object : NativeAdEventCallback {
            override fun onAdImpression() = MainDispatch.post {
                times.onImpression()
                AdsFlowLog.log(placement, AdsFlowLog.Event.IMPRESSION)
            }
            override fun onAdClicked() = MainDispatch.post {
                AdsFlowLog.log(placement, AdsFlowLog.Event.CLICKED)
                listener.onAdClicked()
            }
            override fun onAdPaid(value: AdValue) = AdsSdk.reportPaid(placement, value)
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // Kept for this placement: the screen shows it again when it reopens (KeptAds).
                KeptAds.keep(placement.key, ad, variant = null, times) { ad.destroy() }
                AdsLog.d("${placement.key} -> native kept for the screen's return")
            }
        }
        lifecycle.addObserver(observer)

        AdsLog.d("${placement.key} -> native loaded")
        listener.onAdLoaded(ad)
        val shown = Shown(ad, observer)
        // Same rules when the screen is shown again without being recreated (a tab): see AdSlotView.onShownAgain.
        ShownAds.register(lifecycleOwner, { times }) { onDone ->
            AdsFlowLog.log(placement, AdsFlowLog.Event.REFRESHING, KeptAds.agoText(times.lastImpressionAtMillis?.let { KeptAds.now() - it }))
            refresh(lifecycleOwner, placement, listener, shown, onDone)
        }
        return shown
    }

    private fun failed(placement: AdPlacement, reason: String, listener: NativeAdListener) {
        AdsLog.d("${placement.key} -> native not loaded: $reason")
        listener.onAdFailedToLoad(reason)
    }

    private fun request(placement: AdPlacement) =
        NativeAdRequest.Builder(placement.adUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build()

    private fun nativePlacement(placementKey: String): AdPlacement =
        AdsSdk.placement(placementKey).also {
            require(it.format == AdFormat.NATIVE) { "$placementKey is not a NATIVE placement" }
        }

    private fun preloadPlacement(key: String) = AdsSdk.placement(key).forScreen(preloadScreens[key])

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) = MainDispatch.post {
            AdsLog.d("$preloadId -> native preloaded")
            AdsFlowLog.log(preloadPlacement(preloadId), AdsFlowLog.Event.READY_IN_CACHE)
            readyWaiters.resolve(preloadId, true)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) = MainDispatch.post {
            AdsLog.w("$preloadId -> native failed to preload: ${adError.code} ${adError.message}")
            AdsFlowLog.log(preloadPlacement(preloadId), AdsFlowLog.Event.FAILED, "preload: ${adError.code}: ${adError.message}")
            readyWaiters.resolve(preloadId, false)
        }
    }

    private const val BUFFER_SIZE = 1
    private const val PRELOAD_WAIT_MILLIS = 10_000L
}
