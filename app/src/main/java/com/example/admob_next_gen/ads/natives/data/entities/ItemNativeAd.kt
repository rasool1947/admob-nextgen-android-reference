package com.example.admob_next_gen.ads.natives.data.entities

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd



data class ItemNativeAd(
    val adId: String,
    val nativeAd: NativeAd,
    val impressionReceived: Boolean = false
)