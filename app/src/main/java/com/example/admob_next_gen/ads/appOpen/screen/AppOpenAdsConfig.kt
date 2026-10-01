package com.example.admob_next_gen.ads.appOpen.screen

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.admob_next_gen.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.admob_next_gen.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.admob_next_gen.ads.appOpen.screen.manager.AppOpenManager
import com.example.admob_next_gen.utilities.manager.InternetManager
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.example.admob_next_gen.utilities.utils.Constants.TAG_ADS
import com.nextgen.ads.AdsSdk

/**
 * Date: 1/17/2025
 *
 */

class AppOpenAdsConfig(
    private val context: Context,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : AppOpenManager() {

    fun loadAppOpenAd(adType: AppOpenAdKey, listener: AppOpenOnLoadCallBack? = null) {
        if (!AdsSdk.canLoadAds) {
            Log.e(TAG_ADS, "${adType.value} -> loadAppOpen: Ads not allowed (no consent / SDK not initialized / premium)")
            listener?.onResponse(false)
            return
        }

        var interAdId = ""
        var isRemoteEnable = false

        when (adType) {
            AppOpenAdKey.SPLASH -> {
                interAdId = context.getString(R.string.admob_app_open_id)
                isRemoteEnable = sharedPreferenceUtils.rcAppOpen != 0
            }
        }

        loadAppOpen(
            adType = adType.value,
            appOpenId = interAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showAppOpenAd(activity: Activity?, adType: AppOpenAdKey, listener: AppOpenOnShowCallBack? = null) {
        showAppOpen(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener
        )
    }
}