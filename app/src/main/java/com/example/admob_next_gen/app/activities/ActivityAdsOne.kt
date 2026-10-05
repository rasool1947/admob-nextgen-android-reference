package com.example.admob_next_gen.app.activities

import android.content.Intent
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.ActivityAdsOneBinding
import com.example.admob_next_gen.utilities.base.activities.BaseActivity
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.control.AdSlot
import com.nextgen.ads.control.NativeStyle
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds

/**
 * Activities ads test, screen 1 (opened from Settings in debug builds): how the ads module is used
 * in an Activity-based app. Native ad in the content; an interstitial before opening [ActivityAdsTwo].
 */
class ActivityAdsOne : BaseActivity<ActivityAdsOneBinding>(ActivityAdsOneBinding::inflate) {

    override fun onCreated() {
        // An Activity-based app calls this in its launcher Activity; it runs once per process.
        AdsSdk.gatherConsent(this)

        // Activity: pass `this` as the lifecycle owner (a Fragment passes viewLifecycleOwner).
        binding.adSlotActivityOne.load(this, AdSlot.Native(NativeStyle.MEDIUM), AppAdSlot.ACTIVITY_ONE)
        FullScreenAds.preload(AppAdPlacements.INTER_ACTIVITY_ONE)

        binding.mbOpenTwoActivityOne.setOnClickListener { openTwo() }
        binding.mbCloseActivityOne.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun openTwo() {
        FullScreenAds.showWithLoading(this, AppAdPlacements.INTER_ACTIVITY_ONE, object : FullScreenAdListener {
            override fun onAdFinished() {
                if (!isFinishing) startActivity(Intent(this@ActivityAdsOne, ActivityAdsTwo::class.java))
            }
        })
    }

    // Coming back to this Activity (back from another one) doesn't recreate it: like a tab shown
    // again, its ad is refreshed if it was seen longer than cache.reuse_shown_sec ago.
    override fun onRestart() {
        super.onRestart()
        binding.adSlotActivityOne.onShownAgain()
    }

    override fun onDestroy() {
        if (isFinishing) FullScreenAds.stop(AppAdPlacements.INTER_ACTIVITY_ONE)
        super.onDestroy()
    }
}
