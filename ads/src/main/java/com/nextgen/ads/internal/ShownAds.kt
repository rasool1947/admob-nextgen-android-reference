package com.nextgen.ads.internal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nextgen.ads.control.AdsControlStore

/**
 * The native/banner ad currently on each screen (per lifecycle owner, e.g. an ad slot), so a screen
 * that becomes visible again WITHOUT being recreated (a tab shown with FragmentTransaction.show)
 * follows the same rules as one that was recreated ([KeptAds]): seen a while ago → a new ad is
 * requested and swapped in. Main thread only.
 */
internal object ShownAds {

    private class Entry(val times: () -> KeptAds.AdTimes?, val refresh: (onDone: () -> Unit) -> Unit) {
        var isRefreshing = false
    }

    private val entries = mutableMapOf<LifecycleOwner, Entry>()

    /**
     * [owner] now shows an ad with [times]; [refresh] loads a new one into the same place and calls
     * onDone when it ends (swapped in or failed). Replaces the owner's previous entry (e.g. after a swap).
     */
    fun register(owner: LifecycleOwner, times: () -> KeptAds.AdTimes?, refresh: (onDone: () -> Unit) -> Unit) {
        if (entries.put(owner, Entry(times, refresh)) != null) return
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                entries.remove(owner)
            }
        })
    }

    /** Refreshes [owner]'s ad if it was seen longer than `cache.reuse_shown_sec` ago. True if it did. */
    fun refreshIfSeenLongAgo(owner: LifecycleOwner): Boolean {
        val entry = entries[owner] ?: return false
        val times = entry.times() ?: return false
        if (entry.isRefreshing) return false
        val decision = KeptAds.decide(times, KeptAds.now(), AdsControlStore.current.cache)
        if (decision == KeptAds.Decision.REUSE) return false
        entry.isRefreshing = true
        entry.refresh { entry.isRefreshing = false }
        return true
    }
}
