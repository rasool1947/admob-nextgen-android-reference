package com.example.admob_next_gen.app.splash

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.fragment.app.viewModels
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AdPreloadChain
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentSplashBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.control.SplashFullScreen
import com.nextgen.ads.fullscreen.AppOpenOnResume
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import org.koin.android.ext.android.inject

/**
 * Branding + progress bar while consent and the launch ads load. The bar fills over the ads
 * control timeout (25 s by default) and jumps to the end as soon as the full-screen ad is ready.
 */
class FragmentSplash : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private val viewModel by viewModels<ViewModelSplash>()
    private val prefs by inject<SharedPreferenceUtils>()
    private var progressAnimator: ValueAnimator? = null

    override fun onViewCreated() {
        binding.adSlotSplash.load(viewLifecycleOwner, AdsControlStore.current.splash.bottom, AppAdSlot.SPLASH)
        initConsent()
        initObservers()
    }

    private fun initConsent() {
        if (viewModel.isConsentRequested) return
        viewModel.isConsentRequested = true

        // ActivityMain already started the check; this just waits for its result (shows the form if required).
        val viewModel = viewModel
        AdsSdk.gatherConsent(requireActivity()) { canLoadAds -> viewModel.onConsentResult(canLoadAds) }
    }

    private fun initObservers() {
        viewModel.loadAdsLiveData.observe(viewLifecycleOwner) {
            startProgress()
            loadAds()
        }
        viewModel.doneLiveData.observe(viewLifecycleOwner) { showAd -> completeProgress { onWaitingDone(showAd) } }
        viewModel.navigateLiveData.observe(viewLifecycleOwner) { navigateNext() }
    }

    /* ------------------------------------------- Ads ------------------------------------------- */

    private fun loadAds() {
        // Warm up the next screen's ad (Language, onboarding or main, whichever comes next).
        AdPreloadChain.afterSplash(prefs)

        val viewModel = viewModel
        when (AdsControlStore.current.splash.fullScreen) {
            SplashFullScreen.APP_OPEN -> {
                FullScreenAds.preload(AppAdPlacements.APP_OPEN)
                FullScreenAds.whenReady(AppAdPlacements.APP_OPEN, viewModel.remainingMillis) { isReady ->
                    viewModel.onFullScreenAdResult(isReady)
                }
            }
            // Splash interstitial comes with the splash ad flow step.
            SplashFullScreen.INTERSTITIAL, SplashFullScreen.OFF -> viewModel.onFullScreenAdResult(false)
        }
    }

    private fun onWaitingDone(showAd: Boolean) {
        if (!showAd) return viewModel.onAdFlowFinished()
        if (viewModel.isAdShowStarted) return // already showing (screen was recreated)
        viewModel.isAdShowStarted = true

        val viewModel = viewModel
        FullScreenAds.show(requireActivity(), AppAdPlacements.APP_OPEN, object : FullScreenAdListener {
            override fun onAdFinished() = viewModel.onAdFlowFinished()
        })
    }

    /* ----------------------------------------- Progress ----------------------------------------- */

    /**
     * Fills the bar from where it is now (the timer survives rotation) to the end over the remaining time.
     * Until consent is known the bar is indeterminate.
     */
    private fun startProgress() {
        val bar = binding.progressSplash
        bar.isIndeterminate = false
        val from = (bar.max * viewModel.elapsedMillis / viewModel.timeoutMillis.coerceAtLeast(1)).toInt().coerceAtMost(bar.max)
        progressAnimator = ValueAnimator.ofInt(from, bar.max).apply {
            duration = viewModel.remainingMillis
            interpolator = LinearInterpolator()
            addUpdateListener { bar.progress = it.animatedValue as Int }
            start()
        }
    }

    private fun completeProgress(onEnd: () -> Unit) {
        val bar = binding.progressSplash
        bar.isIndeterminate = false
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(bar.progress, bar.max).apply {
            duration = COMPLETE_MILLIS
            addUpdateListener { bar.progress = it.animatedValue as Int }
            doOnEnd { if (view != null) onEnd() }
            start()
        }
    }

    override fun onDestroyView() {
        progressAnimator?.removeAllListeners()
        progressAnimator?.cancel()
        progressAnimator = null
        super.onDestroyView()
    }

    /* ---------------------------------------- Navigation ---------------------------------------- */

    private fun navigateNext() {
        // Launch flow is over: from now on, returning to the app may show an App Open ad.
        AppOpenOnResume.enable(AppAdPlacements.APP_OPEN)

        // The whole first flow runs again until the user reaches Main from onboarding once.
        val action = when (prefs.isFirstFlowDone) {
            true -> R.id.action_fragmentSplash_to_fragmentMain
            false -> R.id.action_fragmentSplash_to_fragmentLanguage
        }
        navigateTo(R.id.fragmentSplash, action)
    }

    private companion object {
        const val COMPLETE_MILLIS = 400L
    }
}
