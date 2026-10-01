package com.example.admob_next_gen.app.settings

import androidx.core.view.isVisible
import com.example.admob_next_gen.BuildConfig
import com.example.admob_next_gen.R
import com.example.admob_next_gen.databinding.FragmentSettingsBinding
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.popFrom
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.nativead.NativeAds

class FragmentSettings : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    override fun onViewCreated() {
        NativeAds.loadInto(binding.nativeAdSettings, viewLifecycleOwner, AppAdPlacements.NATIVE_SETTINGS)
        initAdsOptions()

        binding.mbBackSettings.setOnClickListener { popFrom(R.id.fragmentSettings) }
    }

    private fun initAdsOptions() {
        binding.mbPrivacySettings.isVisible = AdsSdk.isPrivacyOptionsRequired
        binding.mbPrivacySettings.setOnClickListener { AdsSdk.showPrivacyOptionsForm(requireActivity()) }

        binding.mbAdInspectorSettings.isVisible = BuildConfig.DEBUG
        binding.mbAdInspectorSettings.setOnClickListener { AdsSdk.openAdInspector() }
    }
}
