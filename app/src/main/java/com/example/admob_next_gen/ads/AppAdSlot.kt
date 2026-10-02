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
    TAB(AppAdPlacements.NATIVE_TAB, AppAdPlacements.BANNER_TAB),
}

/** Loads [slot] (from the ads control) with the placements of [appSlot]. */
fun AdSlotView.load(lifecycleOwner: LifecycleOwner, slot: AdSlot, appSlot: AppAdSlot, listener: AdSlotListener? = null) =
    load(lifecycleOwner, slot, appSlot.nativeKey, appSlot.bannerKey, listener)
