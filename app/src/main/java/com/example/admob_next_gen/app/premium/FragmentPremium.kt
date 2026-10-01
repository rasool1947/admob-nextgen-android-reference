package com.example.admob_next_gen.app.premium

import com.example.admob_next_gen.R
import com.example.admob_next_gen.databinding.FragmentPremiumBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.popFrom

class FragmentPremium : BaseFragment<FragmentPremiumBinding>(FragmentPremiumBinding::inflate) {

    override fun onViewCreated() {
        binding.mbBackPremium.setOnClickListener { popFrom(R.id.fragmentPremium) }
    }

}