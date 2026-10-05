package com.example.admob_next_gen.app.main.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.example.admob_next_gen.BuildConfig
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.MainTabKeys
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentSettingsBinding
import com.example.admob_next_gen.databinding.ViewSettingsRowBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.example.admob_next_gen.utilities.language.AppLanguage
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.control.AdsControlStore

/** Settings tab: language, privacy (consent) options, share, rate, and the Ad Inspector in debug builds. */
class FragmentSettings : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    override fun onViewCreated() {
        binding.adSlotSettings.load(viewLifecycleOwner, AdsControlStore.current.main.tab(MainTabKeys.SETTINGS), AppAdSlot.SETTINGS_TAB)

        binding.rowLanguage.bind(R.drawable.ic_svg_language, R.string.settings_language, AppLanguage.current().nativeName) {
            navigateTo(R.id.fragmentMain, R.id.action_fragmentMain_to_fragmentLanguage)
        }
        binding.rowPrivacy.bind(R.drawable.ic_svg_privacy, R.string.settings_privacy) {
            AdsSdk.showPrivacyOptionsForm(requireActivity())
        }
        binding.rowShare.bind(R.drawable.ic_svg_share, R.string.settings_share) { shareApp() }
        binding.rowRate.bind(R.drawable.ic_svg_star, R.string.settings_rate) { openStorePage() }
        binding.rowAdInspector.bind(R.drawable.ic_svg_bug, R.string.settings_ad_inspector) { AdsSdk.openAdInspector() }

        // Required by Google for users who gave consent through the UMP form (e.g. in the EEA).
        binding.rowPrivacy.root.isVisible = AdsSdk.isPrivacyOptionsRequired
        binding.rowAdInspector.root.isVisible = BuildConfig.DEBUG
        binding.mtvVersionSettings.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    private fun ViewSettingsRowBinding.bind(icon: Int, title: Int, value: String? = null, onClick: () -> Unit) {
        ivIconRow.setImageResource(icon)
        mtvTitleRow.setText(title)
        mtvValueRow.text = value
        mtvValueRow.isVisible = value != null
        root.setOnClickListener { onClick() }
    }

    private fun shareApp() {
        val text = getString(R.string.settings_share_text, getString(R.string.app_display_name), requireContext().packageName)
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, getString(R.string.settings_share)))
    }

    private fun openStorePage() {
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }

    // Tabs are hidden/shown, not recreated: a new ad comes if this one was seen a while ago.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) binding.adSlotSettings.onShownAgain()
    }
}
