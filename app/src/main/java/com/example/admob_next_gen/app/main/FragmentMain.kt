package com.example.admob_next_gen.app.main

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.MainTabKeys
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.app.main.explore.FragmentExplore
import com.example.admob_next_gen.app.main.history.FragmentHistory
import com.example.admob_next_gen.app.main.home.FragmentHome
import com.example.admob_next_gen.app.main.settings.FragmentSettings
import com.example.admob_next_gen.databinding.FragmentMainBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.fullscreen.AppOpenOnResume

/**
 * Main screen: bottom navigation with 4 tabs and an ad slot below it.
 * Tabs are child fragments that are shown/hidden (not recreated), so each keeps its state and ad.
 */
class FragmentMain : BaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {

    private enum class Tab(val menuId: Int, val tag: String, val create: () -> Fragment) {
        HOME(R.id.tabHome, MainTabKeys.HOME, ::FragmentHome),
        EXPLORE(R.id.tabExplore, MainTabKeys.EXPLORE, ::FragmentExplore),
        HISTORY(R.id.tabHistory, MainTabKeys.HISTORY, ::FragmentHistory),
        SETTINGS(R.id.tabSettings, MainTabKeys.SETTINGS, ::FragmentSettings),
    }

    private var currentTab = Tab.HOME

    /** Back on another tab goes to Home first; back on Home leaves the app. */
    private val backToHome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.bnvMain.selectedItemId = Tab.HOME.menuId
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(KEY_TAB)?.let { name -> currentTab = Tab.valueOf(name) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TAB, currentTab.name)
    }

    override fun onViewCreated() {
        // Destroyed automatically with this screen's view.
        binding.adSlotMain.load(viewLifecycleOwner, AdsControlStore.current.main.bottom, AppAdSlot.MAIN)
        // Normally enabled by the splash; repeated here for when Android restores the app on this screen.
        AppOpenOnResume.enable(AppAdPlacements.APP_OPEN)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backToHome)
        initTabs()
    }

    private fun initTabs() {
        binding.bnvMain.setOnItemSelectedListener { item ->
            Tab.entries.firstOrNull { it.menuId == item.itemId }?.let(::showTab)
            true
        }
        binding.bnvMain.setOnItemReselectedListener { } // no reload on reselect

        // The fragment instance outlives its view (back stack) and [currentTab] is saved for rotation and
        // process death; the child fragments themselves come back on their own.
        binding.bnvMain.selectedItemId = currentTab.menuId
        showTab(currentTab)
    }

    private fun showTab(tab: Tab) {
        currentTab = tab
        childFragmentManager.commit {
            setReorderingAllowed(true)
            Tab.entries.forEach { other ->
                val fragment = childFragmentManager.findFragmentByTag(other.tag)
                when {
                    other == tab && fragment == null -> add(R.id.fcvTabsMain, tab.create(), tab.tag)
                    other == tab -> show(fragment!!)
                    fragment != null -> hide(fragment)
                }
            }
        }
        backToHome.isEnabled = tab != Tab.HOME
    }

    private companion object {
        const val KEY_TAB = "currentTab"
    }
}
