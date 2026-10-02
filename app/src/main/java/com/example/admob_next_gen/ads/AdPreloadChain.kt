package com.example.admob_next_gen.ads

import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.fullscreen.FullScreenAds

/**
 * Each screen of the launch flow loads the next screen's ad while the user is still on it, so
 * that ad shows instantly. Only slots that are on in the ads control are loaded.
 *
 * Splash -> Language -> Onboarding page 1 -> page 2 -> ... -> last page -> Main
 */
object AdPreloadChain {

    private val control get() = AdsControlStore.current

    /** From the splash: the next screen depends on how far the first run got. */
    fun afterSplash(prefs: SharedPreferenceUtils) = when {
        !prefs.isLanguageSelected -> forLanguage()
        !prefs.isOnboardingDone -> forOnboarding()
        else -> forMain()
    }

    fun forLanguage() = AppAdSlot.LANGUAGE.preload(control.language.bottom)

    /** First onboarding page, plus the "Get Started" interstitial. */
    fun forOnboarding() {
        AppAdSlot.ON_BOARDING.preload(control.onboarding.slotForPage(0))
        if (control.onboarding.getStartedInter) FullScreenAds.preload(AppAdPlacements.INTER_ON_BOARDING)
    }

    fun forOnboardingPage(index: Int) = AppAdSlot.ON_BOARDING.preload(control.onboarding.slotForPage(index))

    /** The ad below the bottom navigation and the one in the first tab (Home). */
    fun forMain() {
        AppAdSlot.MAIN.preload(control.main.bottom)
        AppAdSlot.TAB.preload(control.main.tab(MainTabKeys.HOME))
    }
}

/** Tab names used as keys in the ads control (`main.tabs`). */
object MainTabKeys {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}
