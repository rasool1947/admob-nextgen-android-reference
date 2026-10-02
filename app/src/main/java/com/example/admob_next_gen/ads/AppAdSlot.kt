package com.example.admob_next_gen.ads

import androidx.lifecycle.LifecycleOwner
import com.nextgen.ads.control.AdSlot
import com.nextgen.ads.slot.AdSlotListener
import com.nextgen.ads.slot.AdSlotView

/** The app's ad slots, each with the native and banner placement it uses (the ads control picks which). */
enum class AppAdSlot(val nativeKey: String, val bannerKey: String) {
    SPLASH(AppAdPlacements.NATIVE_SPLASH, AppAdPlacements.BANNER_SPLASH),
    LANGUAGE(AppAdPlacements.NATIVE_LANGUAGE, AppAdPlacements.BANNER_LANGUAGE),
    ON_BOARDING(AppAdPlacements.NATIVE_ON_BOARDING, AppAdPlacements.BANNER_ON_BOARDING),

    /** Below the bottom navigation. */
    MAIN(AppAdPlacements.NATIVE_MAIN, AppAdPlacements.BANNER_MAIN),

    /** Inside the content of a main-screen tab (shared by all tabs). */
    TAB(AppAdPlacements.NATIVE_TAB, AppAdPlacements.BANNER_TAB);

    /** Loads [slot]'s ad ahead of time, for the screen that comes next. Does nothing if the slot is off. */
    fun preload(slot: AdSlot) = AdSlotView.preload(slot, nativeKey, bannerKey)

    fun stopPreload() = AdSlotView.stopPreload(nativeKey, bannerKey)
}

/**
 * Loads [slot] (from the ads control) with the placements of [appSlot]. Uses the ad preloaded by
 * the previous screen if there is one; preloading then stops unless [keepPreloading] (a screen that
 * shows several ads from the same slot, like onboarding pages).
 */
fun AdSlotView.load(
    lifecycleOwner: LifecycleOwner,
    slot: AdSlot,
    appSlot: AppAdSlot,
    keepPreloading: Boolean = false,
    listener: AdSlotListener? = null,
) {
    val stopPreloadAfter = object : AdSlotListener {
        override fun onAdLoaded() {
            if (!keepPreloading) appSlot.stopPreload()
            listener?.onAdLoaded()
        }

        override fun onAdFailedToLoad(reason: String) {
            if (!keepPreloading) appSlot.stopPreload()
            listener?.onAdFailedToLoad(reason)
        }

        override fun onAdClicked() {
            listener?.onAdClicked()
        }
    }
    load(lifecycleOwner, slot, appSlot.nativeKey, appSlot.bannerKey, stopPreloadAfter)
}
