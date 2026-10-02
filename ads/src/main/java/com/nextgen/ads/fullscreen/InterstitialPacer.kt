package com.nextgen.ads.fullscreen

import android.os.SystemClock
import com.nextgen.ads.control.MainInterControl

/**
 * Decides on which clicks an interstitial may show, following [MainInterControl]:
 * - `every_nth`: show on every nth click (counted since the last interstitial),
 * - `show_on_first_click`: the very first click may show one too,
 * - `min_interval_sec`: never closer than this to the previous full-screen ad of any kind
 *   (so it doesn't follow right after an App Open or onboarding ad).
 *
 * A click that is "due" but can't show (interval not over, no ad ready) keeps the turn: the next
 * click tries again instead of waiting another n clicks.
 *
 * ```
 * if (pacer.onClick()) FullScreenAds.showWithLoading(activity, key, listener)  // pacer.onShown() in onAdShowed
 * ```
 */
class InterstitialPacer(
    private val control: () -> MainInterControl,
    private val lastAdShownAtMillis: () -> Long = { FullScreenAds.lastShownAtMillis },
    private val nowMillis: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private var clicksSinceLastAd = 0
    private var hasShownOnce = false

    /** Counts a click; true if an interstitial should be shown for it. */
    fun onClick(): Boolean {
        val rules = control()
        if (!rules.enabled) return false

        clicksSinceLastAd++
        val isDue = (rules.showOnFirstClick && !hasShownOnce) || clicksSinceLastAd >= rules.everyNth
        if (!isDue) return false

        val lastShown = lastAdShownAtMillis()
        return lastShown == 0L || nowMillis() - lastShown >= rules.minIntervalMillis
    }

    /** Call when the interstitial actually appeared; the count starts again. */
    fun onShown() {
        clicksSinceLastAd = 0
        hasShownOnce = true
    }
}
