package com.nextgen.ads.fullscreen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader
import com.nextgen.ads.config.AdFormat

/**
 * Hides the per-format preloader classes and event-callback types behind one interface, so
 * [FullScreenAds] is written once for all four full-screen formats.
 */
internal sealed interface FullScreenFormat {
    fun start(preloadId: String, configuration: PreloadConfiguration, callback: PreloadCallback): Boolean
    fun isAdAvailable(preloadId: String): Boolean
    fun destroy(preloadId: String): Boolean

    /** Polls the next preloaded ad and shows it. Returns false if no ad was available. */
    fun pollAndShow(preloadId: String, activity: Activity, events: AdEventCallback, onReward: (RewardItem) -> Unit): Boolean

    data object AppOpen : FullScreenFormat {
        override fun start(preloadId: String, configuration: PreloadConfiguration, callback: PreloadCallback) =
            AppOpenAdPreloader.start(preloadId, configuration, callback)

        override fun isAdAvailable(preloadId: String) = AppOpenAdPreloader.isAdAvailable(preloadId)
        override fun destroy(preloadId: String) = AppOpenAdPreloader.destroy(preloadId)

        override fun pollAndShow(preloadId: String, activity: Activity, events: AdEventCallback, onReward: (RewardItem) -> Unit): Boolean {
            val ad: AppOpenAd = AppOpenAdPreloader.pollAd(preloadId) ?: return false
            ad.adEventCallback = object : AppOpenAdEventCallback, AdEventCallback by events {}
            ad.show(activity)
            return true
        }
    }

    data object Interstitial : FullScreenFormat {
        override fun start(preloadId: String, configuration: PreloadConfiguration, callback: PreloadCallback) =
            InterstitialAdPreloader.start(preloadId, configuration, callback)

        override fun isAdAvailable(preloadId: String) = InterstitialAdPreloader.isAdAvailable(preloadId)
        override fun destroy(preloadId: String) = InterstitialAdPreloader.destroy(preloadId)

        override fun pollAndShow(preloadId: String, activity: Activity, events: AdEventCallback, onReward: (RewardItem) -> Unit): Boolean {
            val ad: InterstitialAd = InterstitialAdPreloader.pollAd(preloadId) ?: return false
            ad.adEventCallback = object : InterstitialAdEventCallback, AdEventCallback by events {}
            ad.show(activity)
            return true
        }
    }

    data object Rewarded : FullScreenFormat {
        override fun start(preloadId: String, configuration: PreloadConfiguration, callback: PreloadCallback) =
            RewardedAdPreloader.start(preloadId, configuration, callback)

        override fun isAdAvailable(preloadId: String) = RewardedAdPreloader.isAdAvailable(preloadId)
        override fun destroy(preloadId: String) = RewardedAdPreloader.destroy(preloadId)

        override fun pollAndShow(preloadId: String, activity: Activity, events: AdEventCallback, onReward: (RewardItem) -> Unit): Boolean {
            val ad: RewardedAd = RewardedAdPreloader.pollAd(preloadId) ?: return false
            ad.adEventCallback = object : RewardedAdEventCallback, AdEventCallback by events {}
            ad.show(activity) { reward -> onReward(reward) }
            return true
        }
    }

    data object RewardedInterstitial : FullScreenFormat {
        override fun start(preloadId: String, configuration: PreloadConfiguration, callback: PreloadCallback) =
            RewardedInterstitialAdPreloader.start(preloadId, configuration, callback)

        override fun isAdAvailable(preloadId: String) = RewardedInterstitialAdPreloader.isAdAvailable(preloadId)
        override fun destroy(preloadId: String) = RewardedInterstitialAdPreloader.destroy(preloadId)

        override fun pollAndShow(preloadId: String, activity: Activity, events: AdEventCallback, onReward: (RewardItem) -> Unit): Boolean {
            val ad: RewardedInterstitialAd = RewardedInterstitialAdPreloader.pollAd(preloadId) ?: return false
            ad.adEventCallback = object : RewardedInterstitialAdEventCallback, AdEventCallback by events {}
            ad.show(activity) { reward -> onReward(reward) }
            return true
        }
    }

    companion object {
        fun of(format: AdFormat): FullScreenFormat = when (format) {
            AdFormat.APP_OPEN -> AppOpen
            AdFormat.INTERSTITIAL -> Interstitial
            AdFormat.REWARDED -> Rewarded
            AdFormat.REWARDED_INTERSTITIAL -> RewardedInterstitial
            AdFormat.BANNER, AdFormat.NATIVE -> throw IllegalArgumentException("$format is not a full-screen format")
        }
    }
}
