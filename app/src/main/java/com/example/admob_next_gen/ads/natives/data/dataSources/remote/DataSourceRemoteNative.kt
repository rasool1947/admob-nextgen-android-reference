package com.example.admob_next_gen.ads.natives.data.dataSources.remote

import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.example.admob_next_gen.utilities.utils.Constants.TAG_ADS
import com.example.admob_next_gen.ads.natives.data.entities.ItemNativeAd



class DataSourceRemoteNative {

    fun fetchNativeAd(adKey: String, adId: String, callback: (ItemNativeAd?) -> Unit) {
        var nativeAd: NativeAd? = null

        val adRequest = NativeAdRequest
            .Builder(adId, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        NativeAdLoader.load(
            adRequest,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(ad: NativeAd) {
                    Log.i(TAG_ADS, "$adKey -> loadNative: onAdLoaded")
                    nativeAd = ad
                    ad.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdImpression() {
                            Log.v(TAG_ADS, "$adKey -> loadNative: onAdImpression")
                            nativeAd?.let { callback.invoke(ItemNativeAd(adId, it, true)) }
                        }
                    }
                    callback.invoke(ItemNativeAd(adId, ad))
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG_ADS, "$adKey -> loadNative: onAdFailedToLoad: ${loadAdError.message}")
                    callback.invoke(null)
                }
            }
        )
        Log.d(TAG_ADS, "$adKey -> loadNative: Requesting admob server for ad...")
    }
}