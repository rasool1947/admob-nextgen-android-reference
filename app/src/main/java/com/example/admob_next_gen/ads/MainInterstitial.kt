package com.example.admob_next_gen.ads

import android.app.Activity
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import com.nextgen.ads.fullscreen.InterstitialPacer

/**
 * The interstitial on navigation inside the main screen. Call [showThen] on every navigation click;
 * the ads control (`main.inter`: every nth click, first click, minimum interval) decides whether an
 * ad comes first. [onDone] always runs once, after the ad or right away.
 */
object MainInterstitial {

    private val pacer = InterstitialPacer({ AdsControlStore.current.main.inter })

    fun preload() {
        if (AdsControlStore.current.main.inter.enabled) FullScreenAds.preload(AppAdPlacements.INTER_MAIN)
    }

    fun showThen(activity: Activity, onDone: () -> Unit) {
        if (!pacer.onClick() || !FullScreenAds.isReady(AppAdPlacements.INTER_MAIN)) return onDone()

        FullScreenAds.showWithLoading(activity, AppAdPlacements.INTER_MAIN, object : FullScreenAdListener {
            override fun onAdShowed() = pacer.onShown()
            override fun onAdFinished() = onDone()
        })
    }
}
