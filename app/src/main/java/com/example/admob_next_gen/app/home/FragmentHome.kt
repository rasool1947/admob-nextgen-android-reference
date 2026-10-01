package com.example.admob_next_gen.app.home

import android.widget.Toast
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentHomeBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.banner.BannerAds
import com.nextgen.ads.banner.BannerSize
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds

class FragmentHome : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated() {
        // Destroyed automatically with this screen's view.
        BannerAds.load(binding.bannerAdViewHome, viewLifecycleOwner, AppAdPlacements.BANNER_HOME, BannerSize.Anchored)
        FullScreenAds.preload(AppAdPlacements.INTER_FEATURE)
        FullScreenAds.preload(AppAdPlacements.REWARDED_AI_FEATURE)

        binding.mbPremiumHome.setOnClickListener { showRewarded() }
        binding.mbFeaturesHome.setOnClickListener { showInterstitial(R.id.action_fragmentHome_to_fragmentFeature) }
        binding.mbSettingsHome.setOnClickListener { showInterstitial(R.id.action_fragmentHome_to_fragmentSettings) }
    }

    /** The reward (opening the feature) is granted only if the user watched the ad. */
    private fun showRewarded() {
        var isRewardEarned = false
        var wasShown = false
        FullScreenAds.show(requireActivity(), AppAdPlacements.REWARDED_AI_FEATURE, object : FullScreenAdListener {
            override fun onAdShowed() {
                wasShown = true
            }

            override fun onUserEarnedReward(reward: RewardItem) {
                isRewardEarned = true
            }

            override fun onAdFinished() {
                val message = when {
                    isRewardEarned -> return navigateTo(R.id.fragmentHome, R.id.action_fragmentHome_to_fragmentFeature)
                    wasShown -> R.string.reward_not_earned
                    else -> R.string.reward_ad_not_available
                }
                context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun showInterstitial(action: Int) {
        FullScreenAds.show(requireActivity(), AppAdPlacements.INTER_FEATURE, object : FullScreenAdListener {
            override fun onAdFinished() = navigateTo(R.id.fragmentHome, action)
        })
    }
}
