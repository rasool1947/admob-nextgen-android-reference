package com.example.admob_next_gen.ads.banner.data.repositories

import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.example.admob_next_gen.ads.banner.data.dataSources.local.DataSourceLocalBanner
import com.example.admob_next_gen.ads.banner.data.dataSources.remote.DataSourceRemoteBanner
import com.example.admob_next_gen.ads.banner.data.entities.ItemBannerAd
import com.example.admob_next_gen.utilities.utils.Constants
import com.example.admob_next_gen.ads.banner.domain.repositories.RepositoryBanner
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdType

/**
 * Date: 1/17/2025
 *
 */

class RepositoryBannerImpl(
    private val dataSourceLocalBanner: DataSourceLocalBanner,
    private val dataSourceRemoteBanner: DataSourceRemoteBanner
) : RepositoryBanner {

    override fun fetchBannerAd(adView: AdView, adKey: String, adId: String, bannerAdType: BannerAdType, callback: (ItemBannerAd?) -> Unit) {

        // Check cache resource
        dataSourceLocalBanner.getCachedBannerAd(adKey)?.let { cachedAd ->
            Log.d(Constants.TAG_ADS, "$adKey -> fetchBannerAd: Reshowing Ad")
            callback.invoke(cachedAd)
            return
        }

        dataSourceRemoteBanner.fetchBannerAd(adView = adView, adKey = adKey, adId = adId, bannerAdType = bannerAdType) { remoteAd ->
            remoteAd?.let {
                dataSourceLocalBanner.putCachedBannerAd(adKey, it)
            }
            callback.invoke(remoteAd)
        }
    }

    override fun destroyBanner(adKey: String): Boolean {
        return dataSourceLocalBanner.destroyBanner(adKey)
    }
}