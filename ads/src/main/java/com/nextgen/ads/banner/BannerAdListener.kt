package com.nextgen.ads.banner

/** Banner events. Every method is called on the main thread. */
interface BannerAdListener {
    fun onAdLoaded(isCollapsible: Boolean) {}

    /** Not loaded: not allowed (consent / premium / disabled) or no fill. The container is hidden. */
    fun onAdFailedToLoad(reason: String) {}

    fun onAdClicked() {}
}
