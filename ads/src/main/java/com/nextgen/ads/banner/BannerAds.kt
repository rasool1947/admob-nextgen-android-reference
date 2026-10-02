package com.nextgen.ads.banner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement
import com.nextgen.ads.config.forScreen
import com.nextgen.ads.internal.AdsFlowLog
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.MainDispatch
import com.nextgen.ads.internal.PreloadWaiters
import java.util.UUID

/**
 * Banner ads bound to a screen's lifecycle.
 *
 * ```
 * BannerAds.load(binding.bannerContainer, viewLifecycleOwner, "banner_home")
 * ```
 * The banner is created for that screen only and destroyed with it (pass the Fragment's
 * `viewLifecycleOwner`, not the Fragment), so it can never leak an old Activity. The SDK
 * refreshes the banner on its own, at the rate set in the AdMob console.
 *
 * Optional [preload]: start loading a placement one screen early so it shows instantly; load()
 * then takes the preloaded banner if it has the same [BannerSize]. [stopPreload] it once it's no
 * longer needed.
 */
object BannerAds {

    /** Placements being preloaded, with the size they were preloaded for. Main thread only. */
    private val preloading = mutableMapOf<String, BannerSize>()
    private val readyWaiters = PreloadWaiters()

    /** Screen each preload is for (log label), e.g. the next onboarding page. Main thread only. */
    private val preloadScreens = mutableMapOf<String, String?>()

    /**
     * @param container Empty ViewGroup (usually a FrameLayout) the banner goes into. Its width
     *                  decides the banner width; its height is reserved before the ad arrives.
     */
    @MainThread
    fun load(
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        placementKey: String,
        size: BannerSize = BannerSize.Anchored,
        listener: BannerAdListener? = null,
        screen: String? = null,
    ) {
        val placement = bannerPlacement(placementKey).forScreen(screen)
        if (lifecycleOwner.isDestroyed) return

        clear(container)
        AdsSdk.whenSdkReady {
            if (lifecycleOwner.isDestroyed) return@whenSdkReady
            AdsSdk.blockReason(placement)?.let { reason ->
                AdsFlowLog.log(placement, AdsFlowLog.Event.SKIPPED, reason)
                return@whenSdkReady notLoaded(container, placement, reason, listener)
            }

            // Wait for the container's real width (it is 0 before the first layout pass).
            container.visibility = View.VISIBLE
            container.doOnLayout {
                // Posted: doOnLayout can run inside the parent's layout pass, and a preloaded banner is
                // added (and the caller's placeholder removed) right away, which must not happen mid-layout.
                MainDispatch.handler.post {
                    if (!lifecycleOwner.isDestroyed) loadWhenLaidOut(container, lifecycleOwner, placement, size, listener)
                }
            }
        }
    }

    /**
     * Starts loading [placementKey] in the background (screen width, [size]), so a later [load]
     * with the same size shows it at once. The SDK keeps one banner ready and refills it after use.
     */
    @MainThread
    fun preload(placementKey: String, size: BannerSize = BannerSize.Anchored, screen: String? = null) {
        val placement = bannerPlacement(placementKey)
        val isNewScreen = preloadScreens.put(placementKey, screen) != screen
        AdsSdk.whenSdkReady { startPreload(placement.forScreen(screen), size, isNewScreen) }
    }

    /** Stops preloading [placementKey] and discards its buffered banner. */
    @MainThread
    fun stopPreload(placementKey: String) {
        if (preloading.remove(placementKey) != null) {
            BannerAdPreloader.destroy(placementKey)
            preloadScreens.remove(placementKey)
            readyWaiters.resolve(placementKey, false)
            AdsLog.d("$placementKey -> banner preload stopped")
        }
    }

    /** Removes and destroys the banner in [container]. Normally not needed: it happens on destroy. */
    @MainThread
    fun clear(container: ViewGroup) {
        container.children.filterIsInstance<AdView>().toList().forEach { adView ->
            container.removeView(adView)
            adView.destroy()
        }
    }

    /* ------------------------------------------- Preload ------------------------------------------- */

    private fun startPreload(placement: AdPlacement, size: BannerSize, isNewScreen: Boolean) {
        val key = placement.key
        when (preloading[key]) {
            null -> Unit
            size -> {
                // Same cache, now filling for another screen (e.g. the next onboarding page): say so in the log.
                if (isNewScreen) {
                    AdsFlowLog.log(placement, AdsFlowLog.Event.PRELOADING, "$size")
                    if (isPreloadedReady(key)) AdsFlowLog.log(placement, AdsFlowLog.Event.READY_IN_CACHE)
                }
                return
            }
            else -> {
                stopPreload(key) // preloaded for another size (e.g. the next onboarding page differs)
                preloadScreens[key] = placement.screen
            }
        }
        AdsSdk.blockReason(placement)?.let { reason ->
            AdsLog.d("$key -> banner preload skipped: $reason")
            AdsFlowLog.log(placement, AdsFlowLog.Event.SKIPPED, "preload: $reason")
            return
        }

        val context = AdsSdk.appContext
        val widthDp = (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).toInt()
        val configuration = PreloadConfiguration(request(placement, adSizeFor(context, size, widthDp), size), BUFFER_SIZE)
        if (BannerAdPreloader.start(key, configuration, preloadCallback)) {
            preloading[key] = size
            AdsLog.d("$key -> banner preload started ($size)")
            AdsFlowLog.log(placement, AdsFlowLog.Event.PRELOADING, "$size")
        }
    }

    private fun preloadPlacement(key: String) = AdsSdk.placement(key).forScreen(preloadScreens[key])

    private val preloadCallback = object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) = MainDispatch.post {
            AdsLog.d("$preloadId -> banner preloaded")
            AdsFlowLog.log(preloadPlacement(preloadId), AdsFlowLog.Event.READY_IN_CACHE)
            readyWaiters.resolve(preloadId, true)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) = MainDispatch.post {
            AdsLog.w("$preloadId -> banner failed to preload: ${adError.code} ${adError.message}")
            AdsFlowLog.log(preloadPlacement(preloadId), AdsFlowLog.Event.FAILED, "preload: ${adError.code}: ${adError.message}")
            readyWaiters.resolve(preloadId, false)
        }
    }

    private fun isPreloadedReady(key: String) = BannerAdPreloader.isAdAvailable(key)

    /* -------------------------------------------- Load -------------------------------------------- */

    private fun loadWhenLaidOut(
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        placement: AdPlacement,
        size: BannerSize,
        listener: BannerAdListener?,
    ) {
        val key = placement.key
        when {
            preloading[key] != size -> loadNow(container, lifecycleOwner, placement, size, listener)
            isPreloadedReady(key) -> showPreloaded(container, lifecycleOwner, placement, size, listener)
            else -> {
                // Still loading from an earlier preload(): wait for it rather than sending a second request.
                AdsLog.d("$key -> banner waiting for preload")
                readyWaiters.await(key, PRELOAD_WAIT_MILLIS, { isPreloadedReady(key) }) { ready ->
                    if (lifecycleOwner.isDestroyed) return@await
                    if (ready) showPreloaded(container, lifecycleOwner, placement, size, listener)
                    else loadNow(container, lifecycleOwner, placement, size, listener)
                }
            }
        }
    }

    private fun showPreloaded(
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        placement: AdPlacement,
        size: BannerSize,
        listener: BannerAdListener?,
    ) {
        val activity = container.context.findActivity()
        val ad = activity?.let { BannerAdPreloader.pollAd(placement.key) }
            ?: return loadNow(container, lifecycleOwner, placement, size, listener)

        val adView = AdView(container.context)
        container.minimumHeight = ad.getAdSize().getHeightInPixels(container.context)
        container.addView(adView, bannerLayoutParams(size))
        adView.registerBannerAd(ad, activity)
        ad.adEventCallback = eventCallback(placement, listener)
        destroyWith(lifecycleOwner, container, adView, placement)

        AdsLog.d("${placement.key} -> banner from preload (collapsible = ${ad.isCollapsible()})")
        AdsFlowLog.log(placement, AdsFlowLog.Event.FROM_CACHE)
        listener?.onAdLoaded(ad.isCollapsible())
    }

    private fun loadNow(
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        placement: AdPlacement,
        size: BannerSize,
        listener: BannerAdListener?,
    ) {
        val context = container.context
        val density = context.resources.displayMetrics.density
        val availableWidthPx = container.width - container.paddingLeft - container.paddingRight
        val widthDp = ((if (availableWidthPx > 0) availableWidthPx else context.resources.displayMetrics.widthPixels) / density).toInt()
        val adSize = adSizeFor(context, size, widthDp)

        val adView = AdView(context)
        // Reserve the banner's height up front so the screen doesn't jump when the ad arrives.
        container.minimumHeight = adSize.getHeightInPixels(context)
        container.addView(adView, bannerLayoutParams(size))
        destroyWith(lifecycleOwner, container, adView, placement)

        AdsLog.d("${placement.key} -> banner loading (${adSize.width}x${adSize.height}dp, $size)")
        AdsFlowLog.log(placement, AdsFlowLog.Event.LOADING, "${adSize.width}x${adSize.height}dp")
        adView.loadAd(request(placement, adSize, size), object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                ad.adEventCallback = eventCallback(placement, listener)
                MainDispatch.post {
                    if (adView.parent == null) return@post // screen already destroyed
                    AdsLog.d("${placement.key} -> banner loaded (collapsible = ${ad.isCollapsible()})")
                    AdsFlowLog.log(placement, AdsFlowLog.Event.LOADED)
                    listener?.onAdLoaded(ad.isCollapsible())
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) = MainDispatch.post {
                if (adView.parent == null) return@post
                container.removeView(adView)
                adView.destroy()
                AdsFlowLog.log(placement, AdsFlowLog.Event.FAILED, "${adError.code}: ${adError.message}")
                notLoaded(container, placement, "${adError.code}: ${adError.message}", listener)
            }
        })
    }

    /* ------------------------------------------- Shared ------------------------------------------- */

    private fun request(placement: AdPlacement, adSize: AdSize, size: BannerSize): BannerAdRequest =
        BannerAdRequest.Builder(placement.adUnitId, adSize)
            .apply {
                if (size is BannerSize.Collapsible) {
                    setGoogleExtrasBundle(Bundle().apply {
                        putString("collapsible", if (size.fromTop) "top" else "bottom")
                        // Same id for the banner's lifetime, so refreshes don't re-expand it.
                        putString("collapsible_request_id", UUID.randomUUID().toString())
                    })
                }
            }
            .build()

    private fun adSizeFor(context: Context, size: BannerSize, widthDp: Int): AdSize = when (size) {
        BannerSize.Anchored, is BannerSize.Collapsible -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        BannerSize.LargeAnchored -> AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
        BannerSize.Standard -> AdSize.BANNER
        BannerSize.Large -> AdSize.LARGE_BANNER
        BannerSize.MediumRectangle -> AdSize.MEDIUM_RECTANGLE
        is BannerSize.Inline -> size.maxHeightDp
            ?.let { AdSize.getInlineAdaptiveBannerAdSize(widthDp, it) }
            ?: AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, widthDp)
    }

    private fun eventCallback(placement: AdPlacement, listener: BannerAdListener?) = object : BannerAdEventCallback {
        // Banners refresh every 30-60 s and report an impression each time; the first one is enough in the log.
        private var isImpressionLogged = false

        override fun onAdImpression() = MainDispatch.post {
            if (!isImpressionLogged) AdsFlowLog.log(placement, AdsFlowLog.Event.IMPRESSION)
            isImpressionLogged = true
        }
        override fun onAdClicked() = MainDispatch.post {
            AdsFlowLog.log(placement, AdsFlowLog.Event.CLICKED)
            listener?.onAdClicked()
        }
        override fun onAdPaid(value: AdValue) = AdsSdk.reportPaid(placement, value)
    }

    private fun destroyWith(lifecycleOwner: LifecycleOwner, container: ViewGroup, adView: AdView, placement: AdPlacement) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                container.removeView(adView)
                adView.destroy()
                AdsLog.d("${placement.key} -> banner destroyed")
            }
        })
    }

    /** Adaptive banners fill the width; fixed sizes keep their width and are centered (in a FrameLayout). */
    private fun bannerLayoutParams(size: BannerSize): ViewGroup.LayoutParams {
        val isFixed = size == BannerSize.Standard || size == BannerSize.Large || size == BannerSize.MediumRectangle
        val width = if (isFixed) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        return FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL)
    }

    private fun notLoaded(container: ViewGroup, placement: AdPlacement, reason: String, listener: BannerAdListener?) {
        AdsLog.d("${placement.key} -> banner not loaded: $reason")
        container.visibility = View.GONE
        listener?.onAdFailedToLoad(reason)
    }

    private fun bannerPlacement(placementKey: String): AdPlacement =
        AdsSdk.placement(placementKey).also {
            require(it.format == AdFormat.BANNER) { "$placementKey is not a BANNER placement" }
        }

    private val LifecycleOwner.isDestroyed get() = lifecycle.currentState == Lifecycle.State.DESTROYED

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private const val BUFFER_SIZE = 1
    private const val PRELOAD_WAIT_MILLIS = 10_000L
}
