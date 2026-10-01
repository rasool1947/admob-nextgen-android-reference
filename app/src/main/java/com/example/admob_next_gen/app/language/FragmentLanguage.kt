package com.example.admob_next_gen.app.language

import android.view.View
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.natives.presentation.enums.NativeAdKey
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.databinding.FragmentLanguageBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentLanguage : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private val viewModelNative by viewModel<ViewModelNative>()

    override fun onViewCreated() {
        loadNative()
        initObservers()

        binding.mbContinueLanguage.setOnClickListener { navigateScreen() }
    }

    private fun loadNative() {
        viewModelNative.loadNativeAd(NativeAdKey.LANGUAGE)
    }

    private fun initObservers() {
        viewModelNative.adViewLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdLanguage.setNativeAd(it)
        }
        viewModelNative.loadFailedLiveData.observe(viewLifecycleOwner) {
            binding.nativeAdLanguage.visibility = View.GONE
        }
    }

    private fun navigateScreen() {
        navigateTo(R.id.fragmentLanguage, R.id.action_fragmentLanguage_to_fragmentOnBoarding)
    }
}
