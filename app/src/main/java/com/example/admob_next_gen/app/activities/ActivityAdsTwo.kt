package com.example.admob_next_gen.app.activities

import android.widget.Toast
import androidx.core.view.isVisible
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.ActivityAdsTwoBinding
import com.example.admob_next_gen.utilities.base.activities.BaseActivity
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.nextgen.ads.control.AdSlot
import com.nextgen.ads.control.BannerStyle
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds

/** Activities ads test, screen 2: bottom banner and a rewarded ad started by the user. */
class ActivityAdsTwo : BaseActivity<ActivityAdsTwoBinding>(ActivityAdsTwoBinding::inflate) {

    override fun onCreated() {
        binding.adSlotActivityTwo.load(this, AdSlot.Banner(BannerStyle.ADAPTIVE), AppAdSlot.ACTIVITY_TWO)
        FullScreenAds.preload(AppAdPlacements.REWARDED_ACTIVITY_TWO)

        binding.mbRewardedActivityTwo.setOnClickListener { showRewarded() }
        binding.mbCloseActivityTwo.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun showRewarded() {
        if (!FullScreenAds.isReady(AppAdPlacements.REWARDED_ACTIVITY_TWO)) {
            return Toast.makeText(this, R.string.reward_ad_not_available, Toast.LENGTH_SHORT).show()
        }
        var earnedReward: RewardItem? = null
        FullScreenAds.showWithLoading(this, AppAdPlacements.REWARDED_ACTIVITY_TWO, object : FullScreenAdListener {
            override fun onUserEarnedReward(reward: RewardItem) {
                earnedReward = reward
            }

            override fun onAdFinished() {
                val earned = earnedReward ?: return
                binding.mtvRewardActivityTwo.text = getString(R.string.activity_two_reward_earned, earned.amount, earned.type)
                binding.mtvRewardActivityTwo.isVisible = true
            }
        })
    }

    // Back here from another Activity, or after a full-screen ad: refresh the banner if it was seen a while ago.
    override fun onRestart() {
        super.onRestart()
        binding.adSlotActivityTwo.onShownAgain()
    }

    override fun onDestroy() {
        if (isFinishing) FullScreenAds.stop(AppAdPlacements.REWARDED_ACTIVITY_TWO)
        super.onDestroy()
    }
}
