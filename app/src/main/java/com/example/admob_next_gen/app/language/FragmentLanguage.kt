package com.example.admob_next_gen.app.language

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentLanguageBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.nativead.NativeAdListener
import com.nextgen.ads.nativead.NativeAds

class FragmentLanguage : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    override fun onViewCreated() {
        loadNative()

        binding.mbContinueLanguage.setOnClickListener { navigateScreen() }
    }

    /** Uses the ad preloaded on the splash screen, then stops preloading (this screen is shown once). */
    private fun loadNative() {
        NativeAds.loadInto(binding.nativeAdLanguage, viewLifecycleOwner, AppAdPlacements.NATIVE_LANGUAGE, object : NativeAdListener {
            override fun onAdLoaded(ad: NativeAd) = NativeAds.stop(AppAdPlacements.NATIVE_LANGUAGE)
            override fun onAdFailedToLoad(reason: String) = NativeAds.stop(AppAdPlacements.NATIVE_LANGUAGE)
        })
    }

    private fun navigateScreen() {
        navigateTo(R.id.fragmentLanguage, R.id.action_fragmentLanguage_to_fragmentOnBoarding)
    }
}
