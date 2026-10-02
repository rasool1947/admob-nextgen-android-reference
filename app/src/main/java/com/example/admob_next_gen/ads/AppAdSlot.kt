package com.example.admob_next_gen.ads

import androidx.lifecycle.LifecycleOwner
import com.nextgen.ads.control.AdSlot
import com.nextgen.ads.control.OnboardingAdMode
import com.nextgen.ads.slot.AdSlotListener
import com.nextgen.ads.slot.AdSlotView

/** The app's ad slots, each with the native and banner placement it uses (the ads control picks which). */
enum class AppAdSlot(val nativeKey: String, val bannerKey: String) {
    SPLASH(AppAdPlacements.NATIVE_SPLASH, AppAdPlacements.BANNER_SPLASH),
    LANGUAGE(AppAdPlacements.NATIVE_LANGUAGE, AppAdPlacements.BANNER_LANGUAGE),

    /** One ad for all onboarding pages (`onboarding.mode = shared`). */
    ON_BOARDING(AppAdPlacements.NATIVE_ON_BOARDING, AppAdPlacements.BANNER_ON_BOARDING),

    /** Onboarding pages 1-4 (`per_page`): each page has its own ad units, so its own cache. */
    OB1(AppAdPlacements.NATIVE_OB[0], AppAdPlacements.BANNER_OB[0]),
    OB2(AppAdPlacements.NATIVE_OB[1], AppAdPlacements.BANNER_OB[1]),
    OB3(AppAdPlacements.NATIVE_OB[2], AppAdPlacements.BANNER_OB[2]),
    OB4(AppAdPlacements.NATIVE_OB[3], AppAdPlacements.BANNER_OB[3]),

    /** Below the bottom navigation. */
    MAIN(AppAdPlacements.NATIVE_MAIN, AppAdPlacements.BANNER_MAIN),

    /** Inside the content of a main-screen tab (shared by all tabs). */
    TAB(AppAdPlacements.NATIVE_TAB, AppAdPlacements.BANNER_TAB);

    /** Loads [slot]'s ad ahead of time, for the screen that comes next. Nothing if the slot is off. */
    fun preload(slot: AdSlot) = AdSlotView.preload(slot, nativeKey, bannerKey)

    fun stopPreload() = AdSlotView.stopPreload(nativeKey, bannerKey)

    companion object {
        private val onboardingPages = listOf(OB1, OB2, OB3, OB4)

        /** Slot of onboarding page [index] (0-based): its own in per-page mode, the shared one otherwise. */
        fun onboardingPage(index: Int, mode: OnboardingAdMode): AppAdSlot = when (mode) {
            OnboardingAdMode.PER_PAGE -> onboardingPages.getOrElse(index) { onboardingPages.last() }
            OnboardingAdMode.SHARED -> ON_BOARDING
        }

        val allOnboarding: List<AppAdSlot> get() = onboardingPages + ON_BOARDING
    }
}

/**
 * Loads [slot] (from the ads control) with the placements of [appSlot]. Uses the ad the previous
 * screen preloaded, if any; that preload then stops (each slot shows one ad on its screen).
 */
fun AdSlotView.load(lifecycleOwner: LifecycleOwner, slot: AdSlot, appSlot: AppAdSlot, listener: AdSlotListener? = null) {
    val stopPreloadAfter = object : AdSlotListener {
        override fun onAdLoaded() {
            appSlot.stopPreload()
            listener?.onAdLoaded()
        }

        override fun onAdFailedToLoad(reason: String) {
            appSlot.stopPreload()
            listener?.onAdFailedToLoad(reason)
        }

        override fun onAdClicked() {
            listener?.onAdClicked()
        }
    }
    load(lifecycleOwner, slot, appSlot.nativeKey, appSlot.bannerKey, stopPreloadAfter)
}
