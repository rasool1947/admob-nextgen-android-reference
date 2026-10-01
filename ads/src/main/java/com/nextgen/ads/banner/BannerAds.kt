package com.nextgen.ads.banner

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement
import com.nextgen.ads.internal.AdsLog
import com.nextgen.ads.internal.MainDispatch
import java.util.UUID

/**
 * Banner ads bound to a screen's lifecycle.
 *
 * ```
 * BannerAds.load(binding.bannerContainer, viewLifecycleOwner, "banner_home")
 * ```
 * The AdView is created for that screen only and destroyed with it (pass the Fragment's
 * `viewLifecycleOwner`, not the Fragment), so it can never leak an old Activity. The SDK
 * refreshes the banner on its own, at the rate set in the AdMob console.
 */
object BannerAds {

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
    ) {
        val placement = AdsSdk.placement(placementKey)
        require(placement.format == AdFormat.BANNER) { "$placementKey is not a BANNER placement" }
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return

        clear(container)
        AdsSdk.whenSdkReady {
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return@whenSdkReady
            AdsSdk.blockReason(placement)?.let { reason -> return@whenSdkReady notLoaded(container, placement, reason, listener) }

            // Wait for the container's real width (it is 0 before the first layout pass).
            container.visibility = View.VISIBLE
            container.doOnLayout {
                if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    loadInto(container, lifecycleOwner, placement, size, listener)
                }
            }
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

    private fun loadInto(
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

        val adSize = when (size) {
            BannerSize.Anchored, is BannerSize.Collapsible -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            BannerSize.LargeAnchored -> AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
            is BannerSize.Inline -> size.maxHeightDp
                ?.let { AdSize.getInlineAdaptiveBannerAdSize(widthDp, it) }
                ?: AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, widthDp)
        }

        val request = BannerAdRequest.Builder(placement.adUnitId, adSize)
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

        val adView = AdView(context)
        // Reserve the banner's height up front so the screen doesn't jump when the ad arrives.
        container.minimumHeight = adSize.getHeightInPixels(context)
        container.addView(adView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                container.removeView(adView)
                adView.destroy()
                AdsLog.d("${placement.key} -> banner destroyed")
            }
        })

        AdsLog.d("${placement.key} -> banner loading (${adSize.width}x${adSize.height}dp, $size)")
        adView.loadAd(request, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                ad.adEventCallback = object : BannerAdEventCallback {
                    override fun onAdClicked() = MainDispatch.post { listener?.onAdClicked() }
                    override fun onAdPaid(value: AdValue) = AdsSdk.reportPaid(placement, value)
                }
                MainDispatch.post {
                    if (adView.parent == null) return@post // screen already destroyed
                    AdsLog.d("${placement.key} -> banner loaded (collapsible = ${ad.isCollapsible()})")
                    listener?.onAdLoaded(ad.isCollapsible())
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) = MainDispatch.post {
                if (adView.parent == null) return@post
                container.removeView(adView)
                adView.destroy()
                notLoaded(container, placement, "${adError.code}: ${adError.message}", listener)
            }
        })
    }

    private fun notLoaded(container: ViewGroup, placement: AdPlacement, reason: String, listener: BannerAdListener?) {
        AdsLog.d("${placement.key} -> banner not loaded: $reason")
        container.visibility = View.GONE
        listener?.onAdFailedToLoad(reason)
    }
}
