package com.example.admob_next_gen.ads.banner.data.dataSources.local

import com.example.admob_next_gen.ads.banner.data.entities.ItemBannerAd

/**
 * Date: 1/17/2025
 *
 */

class DataSourceLocalBanner {

    private val bannerAdCache by lazy { BannerAdCache() }

    /**
     * Fetch a cached banner ad for the given key.
     *
     * Returns the ad cached under THIS key only, and only if it hasn't served an
     * impression yet (so it is still safe to reshow without double-counting).
     *
     * It intentionally never borrows another key's ad: an AdView can only have one
     * parent, so handing one screen's view to another reparents/blanks it — that was
     * the cause of the "wrong ad" and "ad disappeared" bugs.
     */
    fun getCachedBannerAd(adKey: String): ItemBannerAd? {
        return bannerAdCache.getImpressionFreeAd(adKey)
    }

    /**
     * Cache the given banner ad using the specified key.
     */
    fun putCachedBannerAd(adKey: String, itemBannerAd: ItemBannerAd) {
        bannerAdCache.putAd(adKey, itemBannerAd)
    }

    /**
     * Remove the cached banner ad for the given key if it has been used (impression received).
     * This ensures only used ads are removed from the cache.
     */
    fun destroyBanner(adKey: String): Boolean {
        return bannerAdCache.deleteAd(adKey)
    }
}