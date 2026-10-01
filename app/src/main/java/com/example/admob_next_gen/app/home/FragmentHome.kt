package com.example.admob_next_gen.app.home

import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentHomeBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.banner.BannerAds
import com.nextgen.ads.banner.BannerSize
import com.nextgen.ads.fullscreen.AppOpenOnResume
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds

class FragmentHome : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated() {
        // Destroyed automatically with this screen's view.
        BannerAds.load(binding.bannerAdViewHome, viewLifecycleOwner, AppAdPlacements.BANNER_HOME, BannerSize.Anchored)
        FullScreenAds.preload(AppAdPlacements.INTER_FEATURE)
        // Warm up the rewarded ad so it's ready when the user opens the Premium screen.
        FullScreenAds.preload(AppAdPlacements.REWARDED_AI_FEATURE)
        // Normally enabled by the splash; repeated here for when Android restores the app on Home.
        AppOpenOnResume.enable(AppAdPlacements.APP_OPEN)

        binding.mbPremiumHome.setOnClickListener { navigateTo(R.id.fragmentHome, R.id.action_fragmentHome_to_fragmentPremium) }
        binding.mbFeaturesHome.setOnClickListener { showInterstitial(R.id.action_fragmentHome_to_fragmentFeature) }
        binding.mbSettingsHome.setOnClickListener { showInterstitial(R.id.action_fragmentHome_to_fragmentSettings) }
    }

    private fun showInterstitial(action: Int) {
        FullScreenAds.show(requireActivity(), AppAdPlacements.INTER_FEATURE, object : FullScreenAdListener {
            override fun onAdFinished() = navigateTo(R.id.fragmentHome, action)
        })
    }
}
