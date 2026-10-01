package com.example.admob_next_gen.ads.interstitial.callbacks

/**
 * Date: 1/16/2025
 *
 */

interface InterstitialOnShowCallBack {
    fun onAdDismissedFullScreenContent() {}
    fun onAdFailedToShow()
    fun onAdShowedFullScreenContent() {}
    fun onAdImpression() {}
    fun onAdClicked() {}
    fun onAdImpressionDelayed() {}
}