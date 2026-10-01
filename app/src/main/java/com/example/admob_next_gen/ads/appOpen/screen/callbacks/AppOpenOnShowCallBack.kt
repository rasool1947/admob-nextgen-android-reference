package com.example.admob_next_gen.ads.appOpen.screen.callbacks

/**
 * Date: 1/17/2025
 *
 */

interface AppOpenOnShowCallBack {
    fun onAdDismissedFullScreenContent() {}
    fun onAdFailedToShow()
    fun onAdShowedFullScreenContent() {}
    fun onAdImpression() {}
    fun onAdImpressionDelayed() {}
    fun onAdClicked() {}
}