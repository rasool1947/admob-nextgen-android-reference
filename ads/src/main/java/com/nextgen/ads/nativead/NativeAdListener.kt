package com.nextgen.ads.nativead

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

/** Native ad events. Every method is called on the main thread. */
interface NativeAdListener {
    /**
     * Bind it to a NativeAdView now. It is destroyed automatically with the lifecycle passed to load().
     * Can be called a second time on the same screen: a kept ad that was seen a while ago shows first,
     * then the new ad that replaces it (bind that one in its place).
     */
    fun onAdLoaded(ad: NativeAd) {}

    /** Not loaded: not allowed (consent / premium / disabled) or no fill. */
    fun onAdFailedToLoad(reason: String) {}

    fun onAdClicked() {}
}
