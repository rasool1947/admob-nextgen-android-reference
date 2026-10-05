package com.example.admob_next_gen.app.main.history

import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.MainTabKeys
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentHistoryBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.nextgen.ads.control.AdsControlStore

/** History tab: empty state for now. */
class FragmentHistory : BaseFragment<FragmentHistoryBinding>(FragmentHistoryBinding::inflate) {

    override fun onViewCreated() {
        binding.adSlotHistory.load(viewLifecycleOwner, AdsControlStore.current.main.tab(MainTabKeys.HISTORY), AppAdSlot.HISTORY_TAB)
    }

    // Tabs are hidden/shown, not recreated: a new ad comes if this one was seen a while ago.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) binding.adSlotHistory.onShownAgain()
    }
}
