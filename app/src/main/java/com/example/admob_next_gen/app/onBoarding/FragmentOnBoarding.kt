package com.example.admob_next_gen.app.onBoarding

import android.view.View
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.natives.presentation.enums.NativeAdKey
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.databinding.FragmentOnBoardingBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentOnBoarding : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private val viewModelNative by viewModel<ViewModelNative>()

    override fun onViewCreated() {
        loadNativeAd()
        FullScreenAds.preload(AppAdPlacements.INTER_ON_BOARDING)
        initObservers()

        binding.mbContinueOnBoarding.setOnClickListener { showInterstitialAd() }
    }

    private fun loadNativeAd() {
        viewModelNative.loadNativeAd(NativeAdKey.ON_BOARDING)
    }

    private fun initObservers() {
        viewModelNative.adViewLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdOnBoarding.setNativeAd(it)
        }
        viewModelNative.loadFailedLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdOnBoarding.visibility = View.GONE
        }
        viewModelNative.clearViewLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdOnBoarding.clearView()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        viewModelNative.destroyNative(NativeAdKey.ON_BOARDING)
    }
}
