package com.example.admob_next_gen.ads.rewarded

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.admob_next_gen.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.admob_next_gen.ads.rewarded.enums.RewardedInterAdKey
import com.example.admob_next_gen.ads.rewarded.managers.RewardedInterManager
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
class RewardedInterAdsConfig(
    private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : RewardedInterManager() {

    fun loadRewardedInterAd(adType: RewardedInterAdKey, listener: RewardedOnLoadCallBack? = null) {
        if (!AdsSdk.canLoadAds) {
            Log.e(TAG_ADS, "${adType.value} -> loadRewardedInter: Ads not allowed (no consent / SDK not initialized / premium)")
            listener?.onResponse(false)
            return
        }

        var rewardedInterAdId = ""
        var isRemoteEnable = false

        when (adType) {
            RewardedInterAdKey.AI_FEATURE -> {
                rewardedInterAdId = getResString(R.string.admob_rewarded_inter_ai_feature_id)
                isRemoteEnable = sharedPreferenceUtils.rcRewardedInterAiFeature != 0
            }
        }

        loadRewardedInter(
            adType = adType.value,
            rewardedInterId = rewardedInterAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showRewardedInterAd(activity: Activity?, adType: RewardedInterAdKey, listener: RewardedOnShowCallBack? = null) {
        showRewardedInter(
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