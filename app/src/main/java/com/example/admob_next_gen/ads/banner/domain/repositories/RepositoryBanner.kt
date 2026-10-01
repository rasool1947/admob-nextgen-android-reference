package com.example.admob_next_gen.ads.banner.domain.repositories

import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.example.admob_next_gen.ads.banner.data.entities.ItemBannerAd
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdType

/**
 * Date: 1/17/2025
 *
 */

interface RepositoryBanner {
    fun fetchBannerAd(adView: AdView, adKey: String, adId: String, bannerAdType: BannerAdType, callback: (ItemBannerAd?) -> Unit)
    fun destroyBanner(adKey: String): Boolean
}