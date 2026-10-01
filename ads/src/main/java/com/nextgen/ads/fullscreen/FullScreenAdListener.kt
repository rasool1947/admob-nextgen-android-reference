package com.nextgen.ads.fullscreen

import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

/**
 * Events of one [FullScreenAds.show] call. Every method is called on the main thread.
 *
 * [onAdFinished] is called exactly once per show() call, after the ad was dismissed or couldn't be
 * shown at all; continue your flow (e.g. navigate) there.
 */
interface FullScreenAdListener {
    fun onAdShowed() {}
    fun onAdImpression() {}
    fun onAdClicked() {}

    /** Rewarded / rewarded interstitial only. Grant the reward here, not in [onAdFinished]. */
    fun onUserEarnedReward(reward: RewardItem) {}

    fun onAdDismissed() {}

    /** Not ready, not allowed (consent / premium / disabled), another ad is showing, or the SDK failed. */
    fun onAdFailedToShow(reason: String) {}

    fun onAdFinished() {}
}
