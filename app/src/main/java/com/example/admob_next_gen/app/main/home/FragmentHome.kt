package com.example.admob_next_gen.app.main.home

import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.MainInterstitial
import com.example.admob_next_gen.ads.MainTabKeys
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentHomeBinding
import com.example.admob_next_gen.databinding.ViewHomeCardBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.fullscreen.FullScreenAds

/** Home tab. Its screens open on the root navigation graph (above the bottom navigation). */
class FragmentHome : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated() {
        binding.adSlotHome.load(viewLifecycleOwner, AdsControlStore.current.main.tab(MainTabKeys.HOME), AppAdSlot.HOME_TAB)

        MainInterstitial.preload()
        // Warm up the rewarded ad so it's ready when the user opens the Premium screen.
        FullScreenAds.preload(AppAdPlacements.REWARDED_AI_FEATURE)

        binding.cardInterstitialHome.bind("🎬", R.string.home_interstitial_title, R.string.home_interstitial_desc) {
            openWithInterstitial(R.id.action_fragmentMain_to_fragmentFeature)
        }
        binding.cardRewardedHome.bind("🎁", R.string.home_rewarded_title, R.string.home_rewarded_desc) {
            openWithInterstitial(R.id.action_fragmentMain_to_fragmentPremium)
        }
    }

    private fun ViewHomeCardBinding.bind(icon: String, title: Int, description: Int, onOpen: () -> Unit) {
        mtvIconCard.text = icon
        mtvTitleCard.setText(title)
        mtvDescCard.setText(description)
        mbOpenCard.setOnClickListener { onOpen() }
        root.setOnClickListener { onOpen() }
    }

    /** Main-screen navigation: the ads control decides whether an interstitial comes first. */
    private fun openWithInterstitial(action: Int) {
        MainInterstitial.showThen(requireActivity()) { navigateTo(R.id.fragmentMain, action) }
    }
}
