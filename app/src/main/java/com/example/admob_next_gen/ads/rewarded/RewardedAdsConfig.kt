package com.example.admob_next_gen.ads.rewarded

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.admob_next_gen.ads.rewarded.enums.RewardedAdKey
import com.example.admob_next_gen.ads.rewarded.managers.RewardedManager
import com.example.admob_next_gen.utilities.manager.InternetManager
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.example.admob_next_gen.utilities.utils.Constants.TAG_ADS
import com.nextgen.ads.AdsSdk

/**
 * Date: 1/17/2025
 *
 */


/**
 * @param context: Can be of application class
 */
class RewardedAdsConfig(
    private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : RewardedManager() {

    fun loadRewardedAd(adType: RewardedAdKey, listener: RewardedOnLoadCallBack? = null) {
        if (!AdsSdk.canLoadAds) {
            Log.e(TAG_ADS, "${adType.value} -> loadRewarded: Ads not allowed (no consent / SDK not initialized / premium)")
            listener?.onResponse(false)
            return
        }

        var rewardedAdId = ""
        var isRemoteEnable = false

        when (adType) {
            RewardedAdKey.AI_FEATURE -> {
                rewardedAdId = getResString(R.string.admob_rewarded_ai_feature_id)
                isRemoteEnable = sharedPreferenceUtils.rcRewardedAiFeature != 0
            }
        }

        loadRewarded(
            adType = adType.value,
            rewardedId = rewardedAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showRewardedAd(activity: Activity?, adType: RewardedAdKey, listener: RewardedOnShowCallBack? = null) {
        showRewarded(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener
        )
    }

    private fun getResString(@StringRes resId: Int): String {
        return context?.resources?.getString(resId) ?: ""
    }
}