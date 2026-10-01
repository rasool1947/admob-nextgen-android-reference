package com.example.admob_next_gen.app.settings

import android.view.View
import androidx.core.view.isVisible
import com.example.admob_next_gen.BuildConfig
import com.example.admob_next_gen.R
import com.example.admob_next_gen.databinding.FragmentSettingsBinding
import com.example.admob_next_gen.ads.natives.presentation.enums.NativeAdKey
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.popFrom
import com.nextgen.ads.AdsSdk
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentSettings : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModelNative by viewModel<ViewModelNative>()

    override fun onViewCreated() {
        loadNative()
        initObservers()
        initAdsOptions()

        binding.mbBackSettings.setOnClickListener { popFrom(R.id.fragmentSettings) }
    }

    private fun initAdsOptions() {
        binding.mbPrivacySettings.isVisible = AdsSdk.isPrivacyOptionsRequired
        binding.mbPrivacySettings.setOnClickListener { AdsSdk.showPrivacyOptionsForm(requireActivity()) }

        binding.mbAdInspectorSettings.isVisible = BuildConfig.DEBUG
        binding.mbAdInspectorSettings.setOnClickListener { AdsSdk.openAdInspector() }
    }

    private fun loadNative() {
        viewModelNative.loadNativeAd(NativeAdKey.Settings)
    }

    private fun initObservers() {
        viewModelNative.adViewLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdSettings.setNativeAd(it)
        }
        viewModelNative.loadFailedLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdSettings.visibility = View.GONE
        }
    }
}
