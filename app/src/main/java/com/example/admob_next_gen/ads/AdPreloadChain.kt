package com.example.admob_next_gen.ads

import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.fullscreen.FullScreenAds

/**
 * Each screen of the first flow loads the next screen's ad while the user is still on it, so that
 * ad shows instantly. Only slots that are on in the ads control are loaded:
 *
 * - Splash          -> Language ad
 * - Language        -> onboarding page 1 ad
 * - Onboarding page -> next page's ad; on the last page -> the "Get Started" interstitial
 *
 * The main screen loads its own ads (nothing is preloaded for it).
 */
object AdPreloadChain {

    private val control get() = AdsControlStore.current

    /** From the splash: the Language ad, only while the first flow isn't finished (else Main comes next). */
    fun afterSplash(prefs: SharedPreferenceUtils) {
        if (!prefs.isFirstFlowDone) forLanguage()
    }

    fun forLanguage() = AppAdSlot.LANGUAGE.preload(control.language.bottom)

    /** The ad of onboarding page [index] (0 = the first page, preloaded by Language). */
    fun forOnboardingPage(index: Int) = AppAdSlot.ON_BOARDING.preload(control.onboarding.slotForPage(index))

    /** From the last onboarding page: the "Get Started" interstitial. */
    fun forGetStarted() {
        if (control.onboarding.getStartedInter) FullScreenAds.preload(AppAdPlacements.INTER_ON_BOARDING)
    }
}

/** Tab names used as keys in the ads control (`main.tabs`). */
object MainTabKeys {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}
