package com.example.admob_next_gen.app.language

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AdPreloadChain
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentLanguageBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.example.admob_next_gen.utilities.extensions.popFrom
import com.example.admob_next_gen.utilities.language.AppLanguage
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.control.AdsControlStore
import org.koin.android.ext.android.inject

/**
 * First run: pick a language, then onboarding. From Settings: pick a language, then back.
 * "Done" applies the language right away (AppCompat recreates the screen in the new language).
 */
class FragmentLanguage : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    /** Keeps the picked (not yet applied) language across rotation. */
    class SelectionViewModel : ViewModel() {
        var selectedCode: String? = null
    }

    private val selection by viewModels<SelectionViewModel>()
    private val prefs by inject<SharedPreferenceUtils>()
    private val isFromSettings get() = arguments?.getBoolean(ARG_FROM_SETTINGS) ?: false

    override fun onViewCreated() {
        initList()
        loadAdSlot()

        binding.mbBackLanguage.isVisible = isFromSettings
        binding.mbBackLanguage.setOnClickListener { popFrom(R.id.fragmentLanguage) }
        binding.mbDoneLanguage.setOnClickListener { onDone() }
    }

    private fun initList() {
        val selectedCode = selection.selectedCode ?: AppLanguage.current().code
        binding.rvLanguages.adapter = AdapterLanguage(AppLanguage.all, selectedCode) { language ->
            selection.selectedCode = language.code
        }
    }

    private fun onDone() {
        val code = selection.selectedCode ?: AppLanguage.current().code
        val language = AppLanguage.all.first { it.code == code }

        if (isFromSettings) {
            popFrom(R.id.fragmentLanguage)
        } else {
            prefs.isLanguageSelected = true
            navigateTo(R.id.fragmentLanguage, R.id.action_fragmentLanguage_to_fragmentOnBoarding)
        }
        // After navigating, so the recreated activity restores the next screen.
        AppLanguage.apply(language)
    }

    /** Bottom slot (preloaded by the splash). On first run, warms up the first onboarding ad too. */
    private fun loadAdSlot() {
        binding.adSlotLanguage.load(viewLifecycleOwner, AdsControlStore.current.language.bottom, AppAdSlot.LANGUAGE)
        if (!isFromSettings) AdPreloadChain.forOnboarding()
    }

    private companion object {
        /** Navigation argument (nav_graph.xml). */
        const val ARG_FROM_SETTINGS = "fromSettings"
    }
}
