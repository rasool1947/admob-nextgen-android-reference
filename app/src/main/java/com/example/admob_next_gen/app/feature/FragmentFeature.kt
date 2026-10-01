package com.example.admob_next_gen.app.feature

import android.view.View
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.natives.presentation.enums.NativeAdKey
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.databinding.FragmentFeatureBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.popFrom
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentFeature : BaseFragment<FragmentFeatureBinding>(FragmentFeatureBinding::inflate) {

    private val viewModelNative by viewModel<ViewModelNative>()

    override fun onViewCreated() {
        loadNative()
        initObservers()

        binding.mbBackFeature.setOnClickListener { popFrom(R.id.fragmentFeature) }
    }

    private fun loadNative() {
        viewModelNative.loadNativeAd(NativeAdKey.FEATURE)
    }

    private fun initObservers() {
        viewModelNative.adViewLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdFeature.setNativeAd(it)
        }
        viewModelNative.loadFailedLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdFeature.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelNative.destroyNative(NativeAdKey.FEATURE)
    }
}