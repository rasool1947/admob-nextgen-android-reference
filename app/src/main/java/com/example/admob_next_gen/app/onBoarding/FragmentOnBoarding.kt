package com.example.admob_next_gen.app.onBoarding

import android.view.View
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.admob_next_gen.ads.interstitial.enums.InterAdKey
import com.example.admob_next_gen.ads.natives.presentation.enums.NativeAdKey
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.databinding.FragmentOnBoardingBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentOnBoarding : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private val viewModelNative by viewModel<ViewModelNative>()

    override fun onViewCreated() {
        loadNativeAd()
        loadInterstitialAd()
        initObservers()

        binding.mbContinueOnBoarding.setOnClickListener { checkInterstitial() }
    }

    private fun loadNativeAd() {
        viewModelNative.loadNativeAd(NativeAdKey.ON_BOARDING)
    }

    private fun loadInterstitialAd() {
        diComponent.interstitialAdsConfig.loadInterstitialAd(InterAdKey.ON_BOARDING)
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

    private fun checkInterstitial() {
        when (diComponent.interstitialAdsConfig.isInterstitialLoaded()) {
            true -> showInterstitialAd()
            false -> navigateScreen()
        }
    }

    private fun showInterstitialAd() {
        diComponent.interstitialAdsConfig.showInterstitialAd(activity, InterAdKey.ON_BOARDING, object : InterstitialOnShowCallBack {
            override fun onAdFailedToShow() = navigateScreen()
            override fun onAdImpressionDelayed() = navigateScreen()
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