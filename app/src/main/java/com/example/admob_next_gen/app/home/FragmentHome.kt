package com.example.admob_next_gen.app.home

import android.view.View
import android.widget.Toast
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdKey
import com.example.admob_next_gen.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.admob_next_gen.databinding.FragmentHomeBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.addCleanView
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentHome : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModelBanner by viewModel<ViewModelBanner>()

    override fun onViewCreated() {
        loadBanner()
        FullScreenAds.preload(AppAdPlacements.INTER_FEATURE)
        FullScreenAds.preload(AppAdPlacements.REWARDED_AI_FEATURE)
        initObservers()

        binding.mbPremiumHome.setOnClickListener { showRewarded() }
        binding.mbFeaturesHome.setOnClickListener { showInterstitial(R.id.action_fragmentHome_to_fragmentFeature) }
        binding.mbSettingsHome.setOnClickListener { showInterstitial(R.id.action_fragmentHome_to_fragmentSettings) }
    }

    private fun loadBanner() {
        context?.let {
            val adView = AdView(it)
            viewModelBanner.loadBannerAd(adView, BannerAdKey.SPLASH)
        }
    }

    private fun initObservers() {
        viewModelBanner.adViewLiveData.observe(viewLifecycleOwner) {
            binding.bannerAdViewHome.addCleanView(it)
        }
        viewModelBanner.loadFailedLiveData.observe(viewLifecycleOwner) {
            binding.bannerAdViewHome.visibility = View.GONE
        }
        viewModelBanner.clearViewLiveData.observe(viewLifecycleOwner) {
            binding.bannerAdViewHome.removeAllViews()
        }
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
