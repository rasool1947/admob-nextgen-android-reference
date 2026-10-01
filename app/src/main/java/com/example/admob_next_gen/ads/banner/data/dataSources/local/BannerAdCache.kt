package com.example.admob_next_gen.ads.banner.data.dataSources.local

import android.view.ViewGroup
import com.example.admob_next_gen.ads.banner.data.entities.ItemBannerAd
import java.util.concurrent.ConcurrentHashMap

/**
 * Date: 1/17/2025
 *
 */

class BannerAdCache {

    private val adCache = ConcurrentHashMap<String, ItemBannerAd>()

    /**
     * Return the ad cached under this key only if it hasn't served an impression yet.
     */
    fun getImpressionFreeAd(adKey: String): ItemBannerAd? {
        return adCache[adKey]?.takeIf { !it.impressionReceived }
    }

    /**
     * Store ad for cache
     */
    fun putAd(adKey: String, itemBannerAd: ItemBannerAd) {
        adCache[adKey] = itemBannerAd
    }

    /**
     * Detach the ad's view from its (now dead) parent so it can be safely reshown
     * later, and fully destroy + evict it once it has served an impression.
     *
     * Always detaching prevents a stale parent reference from lingering on a cached
     * AdView, which is what let one screen's banner get "stolen"/blanked by another.
     */
    fun deleteAd(adKey: String): Boolean {
        val item = adCache[adKey] ?: return false
        (item.adView.parent as? ViewGroup)?.removeView(item.adView)
        if (item.impressionReceived) {
            item.adView.destroy()
            adCache.remove(adKey)
            return true
        }
        return false
    }
}