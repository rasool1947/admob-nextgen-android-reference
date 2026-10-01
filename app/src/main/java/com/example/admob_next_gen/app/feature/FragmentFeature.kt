package com.example.admob_next_gen.app.feature

import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentFeatureBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.popFrom
import com.nextgen.ads.nativead.NativeAds

class FragmentFeature : BaseFragment<FragmentFeatureBinding>(FragmentFeatureBinding::inflate) {

    override fun onViewCreated() {
        // Destroyed automatically with this screen's view.
        NativeAds.loadInto(binding.nativeAdFeature, viewLifecycleOwner, AppAdPlacements.NATIVE_FEATURE)

        binding.mbBackFeature.setOnClickListener { popFrom(R.id.fragmentFeature) }
    }
}
