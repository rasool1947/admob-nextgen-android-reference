package com.example.admob_next_gen.ads.banner.data.entities

import com.google.android.libraries.ads.mobile.sdk.banner.AdView

/**
 * Date: 1/17/2025
 *
 */

data class ItemBannerAd(
    val adId: String,
    val adView: AdView,
    var impressionReceived: Boolean = false
)