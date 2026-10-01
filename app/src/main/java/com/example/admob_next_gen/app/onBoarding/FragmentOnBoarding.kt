package com.example.admob_next_gen.app.onBoarding

import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentOnBoardingBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import com.nextgen.ads.nativead.NativeAds

class FragmentOnBoarding : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    override fun onViewCreated() {
        NativeAds.loadInto(binding.nativeAdOnBoarding, viewLifecycleOwner, AppAdPlacements.NATIVE_ON_BOARDING)
        FullScreenAds.preload(AppAdPlacements.INTER_ON_BOARDING)

        binding.mbContinueOnBoarding.setOnClickListener { showInterstitialAd() }
    }

    private fun showInterstitialAd() {
        FullScreenAds.show(requireActivity(), AppAdPlacements.INTER_ON_BOARDING, object : FullScreenAdListener {
            override fun onAdFinished() {
                // Onboarding is shown once; don't keep an ad preloaded for it.
                FullScreenAds.stop(AppAdPlacements.INTER_ON_BOARDING)
                navigateScreen()
            }
        })
    }

    private fun navigateScreen() {
        navigateTo(R.id.fragmentOnBoarding, R.id.action_fragmentOnBoarding_to_fragmentHome)
    }
}
