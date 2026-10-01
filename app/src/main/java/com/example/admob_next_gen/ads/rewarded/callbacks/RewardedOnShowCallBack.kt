package com.example.admob_next_gen.ads.rewarded.callbacks

/**
 * Date: 1/17/2025
 *
 */

interface RewardedOnShowCallBack {
    fun onAdDismissedFullScreenContent() {}
    fun onAdFailedToShow()
    fun onAdShowedFullScreenContent() {}
    fun onAdImpression() {}
    fun onUserEarnedReward()
}