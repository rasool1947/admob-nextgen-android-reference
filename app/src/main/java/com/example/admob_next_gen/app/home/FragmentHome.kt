package com.example.admob_next_gen.app.home

import android.view.View
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdKey
import com.example.admob_next_gen.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.admob_next_gen.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.admob_next_gen.ads.interstitial.enums.InterAdKey
import com.example.admob_next_gen.ads.rewarded.RewardedAdsConfig
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.admob_next_gen.ads.rewarded.enums.RewardedAdKey
import com.example.admob_next_gen.databinding.FragmentHomeBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.addCleanView
import com.example.admob_next_gen.utilities.extensions.navigateTo
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentHome : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModelBanner by viewModel<ViewModelBanner>()

    private val rewardedAdsConfig by inject<RewardedAdsConfig>()

    override fun onViewCreated() {
        loadBanner()
        loadInterstitial()
        loadRewarded()
        initObservers()

        binding.mbPremiumHome.setOnClickListener { onPremiumClick() }
        binding.mbFeaturesHome.setOnClickListener { checkInterstitial(0) }
        binding.mbSettingsHome.setOnClickListener { checkInterstitial(1) }
    }

    private fun loadBanner() {
        context?.let {
            val adView = AdView(it)
            viewModelBanner.loadBannerAd(adView, BannerAdKey.SPLASH)
        }
    }

    private fun loadInterstitial() {
        diComponent.interstitialAdsConfig.loadInterstitialAd(InterAdKey.FEATURE)
    }

    private fun loadRewarded() {
        rewardedAdsConfig.loadRewardedAd(RewardedAdKey.AI_FEATURE, object : RewardedOnLoadCallBack {
            override fun onResponse(isSuccess: Boolean) {
                // ad loaded silently in background
            }
        })
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

    private fun onPremiumClick() {
        when (rewardedAdsConfig.isRewardedLoaded()) {
            true -> showRewarded()
            false -> navigateAfterReward()
        }
    }

    private fun showRewarded() {
        rewardedAdsConfig.showRewardedAd(activity, RewardedAdKey.AI_FEATURE, object : RewardedOnShowCallBack {
            override fun onAdFailedToShow() = navigateAfterReward()
            override fun onUserEarnedReward() = navigateAfterReward()
            override fun onAdDismissedFullScreenContent() {}
        })
    }

    private fun navigateAfterReward() {
        viewModelBanner.destroyBanner(BannerAdKey.HOME_TAB)
        navigateTo(R.id.fragmentHome, R.id.action_fragmentHome_to_fragmentFeature)
    }

    private fun checkInterstitial(caseType: Int) {
        when (diComponent.interstitialAdsConfig.isInterstitialLoaded()) {
            true -> showInterstitial(caseType)
            false -> navigateScreen(caseType)
        }
    }

    private fun showInterstitial(caseType: Int) {
        diComponent.interstitialAdsConfig.showInterstitialAd(activity, InterAdKey.FEATURE, object : InterstitialOnShowCallBack {
            override fun onAdFailedToShow() = navigateScreen(caseType)
            override fun onAdImpressionDelayed() = navigateScreen(caseType)
        })
    }

    private fun navigateScreen(caseType: Int) {
        when (caseType) {
            0 -> navigateTo(R.id.fragmentHome, R.id.action_fragmentHome_to_fragmentFeature)
            1 -> navigateTo(R.id.fragmentHome, R.id.action_fragmentHome_to_fragmentSettings)
        }
    }
}