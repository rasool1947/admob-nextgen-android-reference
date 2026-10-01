package com.example.admob_next_gen.ads.rewarded.managers

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.admob_next_gen.utilities.utils.Constants.TAG_ADS

/**
 * Date: 1/17/2025
 *
 */

abstract class RewardedInterManager {

    private var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isRewardedInterLoading = false

    protected fun loadRewardedInter(
        adType: String,
        rewardedInterId: String,
        adEnable: Boolean,
        isAppPurchased: Boolean,
        isInternetConnected: Boolean,
        listener: RewardedOnLoadCallBack?,
    ) {

        if (isRewardedInterLoaded()) {
            Log.i(TAG_ADS, "$adType -> loadRewardedInter: Already loaded")
            listener?.onResponse(true)
            return
        }

        if (isRewardedInterLoading) {
            Log.d(TAG_ADS, "$adType -> loadRewardedInter: Ad is already loading...")
            // No need to invoke callback, in some cases (e.g. activity recreation) it interrupts our response, as we are waiting for response in Splash
            // listener?.onResponse(false)  // Uncomment if u still need to listen this case
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Premium user")
            listener?.onResponse(false)
            return
        }

        if (adEnable.not()) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Remote config is off")
            listener?.onResponse(false)
            return
        }

        if (isInternetConnected.not()) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Internet is not connected")
            listener?.onResponse(false)
            return
        }

        if (rewardedInterId.trim().isEmpty()) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Ad id is empty")
            listener?.onResponse(false)
            return
        }

        Log.d(TAG_ADS, "$adType -> loadRewardedInter: Requesting admob server for ad...")
        isRewardedInterLoading = true

        RewardedInterstitialAd.load(
            AdRequest.Builder(rewardedInterId.trim()).build(),
            object : AdLoadCallback<RewardedInterstitialAd> {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG_ADS, "$adType -> loadRewardedInter: onAdFailedToLoad: ${adError.message}")
                    isRewardedInterLoading = false
                    mRewardedInterstitialAd = null
                    Handler(Looper.getMainLooper()).post { listener?.onResponse(false) }
                }

                override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                    Log.i(TAG_ADS, "$adType -> loadRewardedInter: onAdLoaded")
                    isRewardedInterLoading = false
                    mRewardedInterstitialAd = rewardedInterstitialAd
                    Handler(Looper.getMainLooper()).post { listener?.onResponse(true) }
                }
            })
    }

    protected fun showRewardedInter(
        activity: Activity?,
        adType: String,
        isAppPurchased: Boolean,
        listener: RewardedOnShowCallBack?
    ) {

        if (isRewardedInterLoaded().not()) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: RewardedInter is not loaded yet")
            listener?.onAdFailedToShow()
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: Premium user")
            if (isRewardedInterLoaded()) {
                Log.d(TAG_ADS, "$adType -> Destroying loaded RewardedInter ad due to Premium user")
                mRewardedInterstitialAd = null
            }
            listener?.onAdFailedToShow()
            return
        }

        if (activity == null) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: activity reference is null")
            listener?.onAdFailedToShow()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: activity is finishing or destroyed")
            listener?.onAdFailedToShow()
            return
        }

        mRewardedInterstitialAd?.adEventCallback = object : RewardedInterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG_ADS, "admob RewardedInter onAdDismissedFullScreenContent")
                listener?.onAdDismissedFullScreenContent()
                mRewardedInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.e(TAG_ADS, "admob RewardedInter onAdFailedToShowFullScreenContent: ${fullScreenContentError.message}")
                listener?.onAdFailedToShow()
                mRewardedInterstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG_ADS, "admob RewardedInter onAdShowedFullScreenContent")
                listener?.onAdShowedFullScreenContent()
                mRewardedInterstitialAd = null
            }

            override fun onAdImpression() {
                Log.v(TAG_ADS, "admob RewardedInter onAdImpression")
                listener?.onAdImpression()
            }
        }

        Log.d(TAG_ADS, "$adType -> RewardedInter: showing ad")
        mRewardedInterstitialAd?.show(
            activity,
            OnUserEarnedRewardListener {
                Log.d(TAG_ADS, "admob RewardedInter onUserEarnedReward")
                listener?.onUserEarnedReward()
            }
        )
    }

    fun isRewardedInterLoaded(): Boolean {
        return mRewardedInterstitialAd != null
    }
}