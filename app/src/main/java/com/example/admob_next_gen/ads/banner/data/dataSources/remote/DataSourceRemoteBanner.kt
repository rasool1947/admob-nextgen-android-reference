package com.example.admob_next_gen.ads.banner.data.dataSources.remote

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.content.getSystemService
import com.example.admob_next_gen.utilities.utils.Constants.TAG_ADS
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.example.admob_next_gen.ads.banner.data.entities.ItemBannerAd
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdType

/**
 * Date: 1/17/2025
 * Fixed: collapsible extras, inline sizes differentiated
 */

class DataSourceRemoteBanner(private val context: Context) {

    fun fetchBannerAd(adView: AdView, adKey: String, adId: String, bannerAdType: BannerAdType, callback: (ItemBannerAd?) -> Unit) {

        val adRequest = when (bannerAdType) {

            BannerAdType.ADAPTIVE -> {
                // Anchored adaptive: fills width, auto height ~50-90dp
                val adSize = getAdaptiveSize() ?: AdSize.BANNER
                BannerAdRequest.Builder(adId, adSize).build()
            }

            BannerAdType.INLINE_FULL -> {
                // Full inline adaptive: variable tall height, best for scrollable content
                val adSize = getInlineFullSize() ?: AdSize.BANNER
                BannerAdRequest.Builder(adId, adSize).build()
            }

            BannerAdType.INLINE_SIMPLE -> {
                // FIX: was using getAdaptiveSize() same as ADAPTIVE — now uses fixed ~50dp height
                val adSize = getInlineSimpleSize() ?: AdSize.BANNER
                BannerAdRequest.Builder(adId, adSize).build()
            }

            BannerAdType.INLINE_120 -> {
                // Inline with fixed 120dp height
                val adSize = getInline120Size() ?: AdSize.BANNER
                BannerAdRequest.Builder(adId, adSize).build()
            }

            BannerAdType.COLLAPSIBLE_TOP -> {
                val adSize = getAdaptiveSize() ?: AdSize.BANNER
                val extras = Bundle()
                extras.putString("collapsible", "top")
                BannerAdRequest.Builder(adId, adSize)
                    .setGoogleExtrasBundle(extras)
                    .build()
            }

            BannerAdType.COLLAPSIBLE_BOTTOM -> {
                val adSize = getAdaptiveSize() ?: AdSize.BANNER
                val extras = Bundle()
                extras.putString("collapsible", "bottom")
                BannerAdRequest.Builder(adId, adSize)
                    .setGoogleExtrasBundle(extras)
                    .build()
            }
        }

        adView.loadAd(
            adRequest,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Log.i(TAG_ADS, "$adKey -> loadBanner: onAdLoaded")
                    // Single source of truth for this ad. The impression callback mutates
                    // this same instance so the cached entry reflects it, without
                    // re-emitting (which re-attached the AdView and caused flicker).
                    val item = ItemBannerAd(adId = adId, adView = adView)
                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() {
                            Log.v(TAG_ADS, "$adKey -> loadBanner: onAdImpression")
                            item.impressionReceived = true
                        }

                        override fun onAdClicked() {
                            Log.d(TAG_ADS, "$adKey -> loadBanner: onAdClicked")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG_ADS, "$adKey -> loadBanner: onAdShowedFullScreenContent")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG_ADS, "$adKey -> loadBanner: onAdDismissedFullScreenContent")
                        }

                        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                            Log.e(TAG_ADS, "$adKey -> loadBanner: onAdFailedToShowFullScreenContent: ${error.message}")
                        }
                    }
                    callback.invoke(item)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG_ADS, "$adKey -> loadBanner: onAdFailedToLoad: ${adError.message}")
                    callback.invoke(null)
                }
            }
        )
        Log.d(TAG_ADS, "$adKey -> loadBanner: Requesting admob server for ad...")
    }

    @Suppress("DEPRECATION")
    private fun getScreenWidthDp(): Int {
        val density = context.resources.displayMetrics.density
        val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService<WindowManager>()
            val bounds = windowManager?.currentWindowMetrics?.bounds
            bounds?.width()?.toFloat()
        } else {
            val display: Display? = context.getSystemService<DisplayManager>()?.getDisplay(Display.DEFAULT_DISPLAY)
            val outMetrics = DisplayMetrics()
            display?.getMetrics(outMetrics)
            outMetrics.widthPixels.toFloat()
        }
        return ((adWidthPixels ?: return 0) / density).toInt()
    }

    /** Standard anchored adaptive banner (fills width, auto height ~50-90dp) */
    private fun getAdaptiveSize(): AdSize? {
        val adWidth = getScreenWidthDp()
        if (adWidth == 0) return null
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    /**
     * Inline full banner - variable height, Google picks best for performance.
     * Good for mid-scroll placements in RecyclerView/LazyColumn.
     */
    private fun getInlineFullSize(): AdSize? {
        val adWidth = getScreenWidthDp()
        if (adWidth == 0) return null
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, adWidth)
    }

    /**
     * FIX: Inline simple — fixed ~50dp height, compact banner.
     * Previously was identical to ADAPTIVE (wrong).
     */
    private fun getInlineSimpleSize(): AdSize? {
        val adWidth = getScreenWidthDp()
        if (adWidth == 0) return null
        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, 70)
    }

    /** Inline banner with fixed 120dp max height */
    private fun getInline120Size(): AdSize? {
        val adWidth = getScreenWidthDp()
        if (adWidth == 0) return null
        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, 120)
    }
}