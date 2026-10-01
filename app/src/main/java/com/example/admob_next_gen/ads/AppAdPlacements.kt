package com.example.admob_next_gen.ads

import android.content.Context
import com.example.admob_next_gen.R
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement

/**
 * Every ad placement in this app. Ad unit ids come from app/build.gradle.kts (test ids for debug,
 * admob.properties for release); the on/off switches come from remote config values.
 */
object AppAdPlacements {

    const val APP_OPEN = "app_open"

    const val BANNER_HOME = "banner_home"

    const val INTER_ON_BOARDING = "inter_on_boarding"
    const val INTER_FEATURE = "inter_feature"

    const val REWARDED_AI_FEATURE = "rewarded_ai_feature"
    const val REWARDED_INTER_AI_FEATURE = "rewarded_inter_ai_feature"

    const val NATIVE_LANGUAGE = "native_language"
    const val NATIVE_ON_BOARDING = "native_on_boarding"
    const val NATIVE_HOME = "native_home"
    const val NATIVE_FEATURE = "native_feature"
    const val NATIVE_SETTINGS = "native_settings"

    fun create(context: Context, prefs: SharedPreferenceUtils): List<AdPlacement> {
        fun id(resId: Int) = context.getString(resId).trim()

        return listOf(
            AdPlacement(APP_OPEN, AdFormat.APP_OPEN, id(R.string.admob_app_open_id)) { prefs.rcAppOpen != 0 },

            AdPlacement(BANNER_HOME, AdFormat.BANNER, id(R.string.admob_banner_home_id)) { prefs.rcBannerHome != 0 },

            AdPlacement(INTER_ON_BOARDING, AdFormat.INTERSTITIAL, id(R.string.admob_inter_on_boarding_id)) { prefs.rcInterOnBoarding != 0 },
            AdPlacement(INTER_FEATURE, AdFormat.INTERSTITIAL, id(R.string.admob_inter_feature_id)) { prefs.rcInterFeature != 0 },

            AdPlacement(REWARDED_AI_FEATURE, AdFormat.REWARDED, id(R.string.admob_rewarded_ai_feature_id)) { prefs.rcRewardedAiFeature != 0 },
            AdPlacement(REWARDED_INTER_AI_FEATURE, AdFormat.REWARDED_INTERSTITIAL, id(R.string.admob_rewarded_inter_ai_feature_id)) { prefs.rcRewardedInterAiFeature != 0 },

            AdPlacement(NATIVE_LANGUAGE, AdFormat.NATIVE, id(R.string.admob_native_language_id)) { prefs.rcNativeLanguage != 0 },
            AdPlacement(NATIVE_ON_BOARDING, AdFormat.NATIVE, id(R.string.admob_native_on_boarding_id)) { prefs.rcNativeOnBoarding != 0 },
            AdPlacement(NATIVE_HOME, AdFormat.NATIVE, id(R.string.admob_native_home_id)) { prefs.rcNativeHome != 0 },
            AdPlacement(NATIVE_FEATURE, AdFormat.NATIVE, id(R.string.admob_native_full_screen_id)) { prefs.rcNativeFeature != 0 },
            AdPlacement(NATIVE_SETTINGS, AdFormat.NATIVE, id(R.string.admob_native_settings_id)) { prefs.rcNativeExit != 0 },
        )
    }
}
