package com.example.admob_next_gen.app.settings

import android.view.View
import com.example.admob_next_gen.R
import com.example.admob_next_gen.databinding.FragmentSettingsBinding
import com.example.admob_next_gen.ads.natives.presentation.enums.NativeAdKey
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.popFrom
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentSettings : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModelNative by viewModel<ViewModelNative>()

    override fun onViewCreated() {
        loadNative()
        initObservers()

        binding.mbBackSettings.setOnClickListener { popFrom(R.id.fragmentSettings) }
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