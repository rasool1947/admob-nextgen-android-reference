package com.example.admob_next_gen.app.onBoarding

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AdPreloadChain
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentOnBoardingBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.control.OnboardingAdMode
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import org.koin.android.ext.android.inject

/** Swipeable intro pages; "Get Started" on the last page shows an interstitial, then the main screen. */
class FragmentOnBoarding : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private val prefs by inject<SharedPreferenceUtils>()

    /** Page count comes from the ads control (`onboarding.pages`), limited to the content we have. */
    private val pages by lazy {
        OnBoardingPage.all.take(AdsControlStore.current.onboarding.pageCount.coerceIn(1, OnBoardingPage.all.size))
    }

    /** Page whose ad is in the slot now, so swiping back and forth doesn't reload it. */
    private var shownAdPage = NO_PAGE

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = onPageShown(position)
    }

    override fun onViewCreated() {
        shownAdPage = NO_PAGE
        AdPreloadChain.forOnboarding() // normally done by the previous screen; covers a restored app
        initPager()

        binding.mbSkipOnBoarding.setOnClickListener { binding.vpOnBoarding.currentItem = pages.lastIndex }
        binding.mbNextOnBoarding.setOnClickListener { onNextClick() }
    }

    private fun initPager() {
        binding.vpOnBoarding.adapter = AdapterOnBoarding(pages)
        binding.vpOnBoarding.registerOnPageChangeCallback(pageCallback)
        initDots()
        onPageShown(binding.vpOnBoarding.currentItem)
    }

    private fun initDots() {
        repeat(pages.size) {
            binding.llDotsOnBoarding.addView(View(requireContext()).apply { setBackgroundResource(R.drawable.bg_ob_dot) })
        }
    }

    private fun onPageShown(position: Int) {
        val isLast = position == pages.lastIndex
        binding.mbNextOnBoarding.setText(if (isLast) R.string.ob_get_started else R.string.ob_next)
        binding.mbSkipOnBoarding.isInvisible = isLast
        loadAdSlot(position)
        val dotSize = resources.getDimensionPixelSize(R.dimen.ob_dot_size)
        val selectedWidth = resources.getDimensionPixelSize(R.dimen.ob_dot_selected_width)
        val gap = resources.getDimensionPixelSize(R.dimen.ob_dot_gap)
        for (i in 0 until binding.llDotsOnBoarding.childCount) {
            val dot = binding.llDotsOnBoarding.getChildAt(i)
            dot.isSelected = i == position
            dot.updateLayoutParams<LinearLayout.LayoutParams> {
                width = if (dot.isSelected) selectedWidth else dotSize
                height = dotSize
                marginStart = gap / 2
                marginEnd = gap / 2
            }
        }
    }

    /**
     * Per-page mode: each page shows its own ad (onboarding.pages). Shared mode: one ad for all pages.
     * While a page is on screen, the next page's ad (or, on the last page, the main screen's) is preloaded.
     */
    private fun loadAdSlot(position: Int) {
        val control = AdsControlStore.current.onboarding
        if (position < pages.lastIndex) AdPreloadChain.forOnboardingPage(position + 1) else AdPreloadChain.forMain()

        val page = if (control.mode == OnboardingAdMode.SHARED) 0 else position
        if (page == shownAdPage) return
        shownAdPage = page
        // Keep preloading: the following pages take their ads from the same slot.
        binding.adSlotOnBoarding.load(viewLifecycleOwner, control.slotForPage(position), AppAdSlot.ON_BOARDING, keepPreloading = true)
    }

    private fun onNextClick() {
        val pager = binding.vpOnBoarding
        if (pager.currentItem < pages.lastIndex) {
            pager.currentItem += 1
        } else {
            showInterstitialAd()
        }
    }

    private fun showInterstitialAd() {
        if (!AdsControlStore.current.onboarding.getStartedInter) return navigateScreen()
        FullScreenAds.showWithLoading(requireActivity(), AppAdPlacements.INTER_ON_BOARDING, object : FullScreenAdListener {
            override fun onAdFinished() {
                // Onboarding is shown once; don't keep an ad preloaded for it.
                FullScreenAds.stop(AppAdPlacements.INTER_ON_BOARDING)
                navigateScreen()
            }
        })
    }

    private fun navigateScreen() {
        AppAdSlot.ON_BOARDING.stopPreload()
        prefs.isFirstFlowDone = true // only now: closing the app earlier restarts the first flow
        navigateTo(R.id.fragmentOnBoarding, R.id.action_fragmentOnBoarding_to_fragmentMain)
    }

    override fun onDestroyView() {
        binding.vpOnBoarding.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroyView()
    }

    private companion object {
        const val NO_PAGE = -1
    }
}
