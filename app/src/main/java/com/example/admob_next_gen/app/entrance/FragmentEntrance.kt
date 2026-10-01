package com.example.admob_next_gen.app.entrance

import android.view.View
import androidx.fragment.app.viewModels
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentEntranceBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.fullscreen.AppOpenOnResume
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import com.nextgen.ads.nativead.NativeAds

class FragmentEntrance : BaseFragment<FragmentEntranceBinding>(FragmentEntranceBinding::inflate) {

    private val viewModel by viewModels<ViewModelEntrance>()

    override fun onViewCreated() {
        initConsentForm()
        initObservers()

        binding.mbNavigateEntrance.setOnClickListener { showAppOpen() }
    }

    private fun initConsentForm() {
        if (viewModel.isConsentRequested) return
        viewModel.isConsentRequested = true

        // ActivityMain already started the check; this just waits for its result (shows the form if required).
        val viewModel = viewModel
        AdsSdk.gatherConsent(requireActivity()) { canLoadAds -> viewModel.onConsentResult(canLoadAds) }
    }

    private fun initObservers() {
        viewModel.consentResultLiveData.observe(viewLifecycleOwner) { canLoadAds ->
            binding.mtvConsentTextEntrance.setText(if (canLoadAds) R.string.consent_ads_enabled else R.string.consent_ads_disabled)
        }
        viewModel.loadAdsLiveData.observe(viewLifecycleOwner) { loadAds() }
        viewModel.navigateLiveData.observe(viewLifecycleOwner) { showButton() }
    }

    private fun loadAds() {
        loadNative()
        loadAppOpen()
    }

    private fun loadNative() {
        binding.mtvNativeTextEntrance.visibility = View.VISIBLE
        // Preloaded here so the Language screen shows its native ad instantly.
        NativeAds.preload(AppAdPlacements.NATIVE_LANGUAGE)
        NativeAds.whenReady(AppAdPlacements.NATIVE_LANGUAGE, ViewModelEntrance.ADS_TIMEOUT) {
            if (view != null) onNativeResponse()
        }
    }

    private fun loadAppOpen() {
        binding.mtvAppOpenTextEntrance.visibility = View.VISIBLE
        FullScreenAds.preload(AppAdPlacements.APP_OPEN)
        FullScreenAds.whenReady(AppAdPlacements.APP_OPEN, ViewModelEntrance.ADS_TIMEOUT) {
            if (view != null) onAppOpenResponse()
        }
    }

    private fun onNativeResponse() {
        binding.mtvNativeTextEntrance.setText(R.string.native_response)
        viewModel.onAdResponse()
    }

    private fun onAppOpenResponse() {
        binding.mtvAppOpenTextEntrance.setText(R.string.app_open_response)
        viewModel.onAdResponse()
    }

    private fun showButton() {
        binding.mbNavigateEntrance.isEnabled = true
    }

    /** Cold-start App Open ad; continues to the next screen once it's closed (or wasn't available). */
    private fun showAppOpen() {
        FullScreenAds.show(requireActivity(), AppAdPlacements.APP_OPEN, object : FullScreenAdListener {
            override fun onAdFinished() = navigateScreen()
        })
    }

    private fun navigateScreen() {
        // Launch flow is over: from now on, returning to the app may show an App Open ad.
        AppOpenOnResume.enable(AppAdPlacements.APP_OPEN)
        navigateTo(R.id.fragmentEntrance, R.id.action_fragmentEntrance_to_fragmentLanguage)
    }
}
