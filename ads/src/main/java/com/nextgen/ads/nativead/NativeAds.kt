package com.nextgen.ads.nativead

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
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
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.MainDispatch
import com.nextgen.ads.internal.PreloadWaiters

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

    @MainThread
    fun preload(placementKey: String) {
        val placement = nativePlacement(placementKey)
        AdsSdk.whenSdkReady { startPreload(placement) }
    }

    private fun startPreload(placement: AdPlacement) {
        val placementKey = placement.key
        if (placementKey in preloading) return
        AdsSdk.blockReason(placement)?.let { reason ->
            AdsLog.d("$placementKey -> native preload skipped: $reason")
            return
        }
        val configuration = PreloadConfiguration(request(placement), BUFFER_SIZE)
        if (NativeAdPreloader.start(placementKey, configuration, preloadCallback)) {
            preloading += placementKey
            AdsLog.d("$placementKey -> native preload started")
        }
    }

    @MainThread
    fun stop(placementKey: String) {
        if (preloading.remove(placementKey)) {
            NativeAdPreloader.destroy(placementKey)
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

    /** Loads an ad and binds it into [view]; hides the view if there is no ad. */
    @MainThread
    fun loadInto(view: NativeAdTemplateView, lifecycleOwner: LifecycleOwner, placementKey: String, listener: NativeAdListener? = null) {
        view.showPlaceholder()
        load(lifecycleOwner, placementKey, object : NativeAdListener {
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
    fun load(lifecycleOwner: LifecycleOwner, placementKey: String, listener: NativeAdListener) {
        val placement = nativePlacement(placementKey)
        AdsSdk.whenSdkReady { loadWhenReady(lifecycleOwner, placement, listener) }
    }

    private fun loadWhenReady(lifecycleOwner: LifecycleOwner, placement: AdPlacement, listener: NativeAdListener) {
        val placementKey = placement.key
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        AdsSdk.blockReason(placement)?.let { reason -> return failed(placement, reason, listener) }

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
                deliver(lifecycleOwner, placement, ad, listener)
            }
        }
    }

    private fun loadNow(lifecycleOwner: LifecycleOwner, placement: AdPlacement, listener: NativeAdListener) {
        AdsLog.d("${placement.key} -> native loading")
        NativeAdLoader.load(request(placement), object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) = MainDispatch.post {
                deliver(lifecycleOwner, placement, nativeAd, listener)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) = MainDispatch.post {
                failed(placement, "${adError.code}: ${adError.message}", listener)
            }
        })
    }

    /** Main thread. Hands the ad to the caller and ties its lifetime to [lifecycleOwner]. */
    private fun deliver(lifecycleOwner: LifecycleOwner, placement: AdPlacement, ad: NativeAd, listener: NativeAdListener) {
        val lifecycle = lifecycleOwner.lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            ad.destroy() // screen closed while loading
            return
        }

        ad.adEventCallback = object : NativeAdEventCallback {
            override fun onAdClicked() = MainDispatch.post { listener.onAdClicked() }
            override fun onAdPaid(value: AdValue) = AdsSdk.reportPaid(placement, value)
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                ad.destroy()
                AdsLog.d("${placement.key} -> native destroyed")
            }
        })

        AdsLog.d("${placement.key} -> native loaded")
        listener.onAdLoaded(ad)
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

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) = MainDispatch.post {
            AdsLog.d("$preloadId -> native preloaded")
            readyWaiters.resolve(preloadId, true)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) = MainDispatch.post {
            AdsLog.w("$preloadId -> native failed to preload: ${adError.code} ${adError.message}")
            readyWaiters.resolve(preloadId, false)
        }
    }

    private const val BUFFER_SIZE = 1
    private const val PRELOAD_WAIT_MILLIS = 10_000L
}
