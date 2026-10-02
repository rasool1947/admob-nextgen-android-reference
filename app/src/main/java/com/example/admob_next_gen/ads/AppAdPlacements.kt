package com.example.admob_next_gen.ads

import android.content.Context
import com.example.admob_next_gen.R
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement

/**
 * Every ad placement in this app. Ad unit ids come from app/build.gradle.kts (test ids for debug,
 * admob.properties for release); the on/off switches are stored per placement (see SharedPreferenceUtils.isAdEnabled).
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
    const val NATIVE_FEATURE = "native_feature"
    const val NATIVE_SETTINGS = "native_settings"

    fun create(context: Context, prefs: SharedPreferenceUtils): List<AdPlacement> {
        fun id(resId: Int) = context.getString(resId).trim()

        return listOf(
            AdPlacement(APP_OPEN, AdFormat.APP_OPEN, id(R.string.admob_app_open_id)) { prefs.isAdEnabled(APP_OPEN) },

            AdPlacement(BANNER_HOME, AdFormat.BANNER, id(R.string.admob_banner_home_id)) { prefs.isAdEnabled(BANNER_HOME) },

            AdPlacement(INTER_ON_BOARDING, AdFormat.INTERSTITIAL, id(R.string.admob_inter_on_boarding_id)) { prefs.isAdEnabled(INTER_ON_BOARDING) },
            AdPlacement(INTER_FEATURE, AdFormat.INTERSTITIAL, id(R.string.admob_inter_feature_id)) { prefs.isAdEnabled(INTER_FEATURE) },

            AdPlacement(REWARDED_AI_FEATURE, AdFormat.REWARDED, id(R.string.admob_rewarded_ai_feature_id)) { prefs.isAdEnabled(REWARDED_AI_FEATURE) },
            AdPlacement(REWARDED_INTER_AI_FEATURE, AdFormat.REWARDED_INTERSTITIAL, id(R.string.admob_rewarded_inter_ai_feature_id)) { prefs.isAdEnabled(REWARDED_INTER_AI_FEATURE) },

            AdPlacement(NATIVE_LANGUAGE, AdFormat.NATIVE, id(R.string.admob_native_language_id)) { prefs.isAdEnabled(NATIVE_LANGUAGE) },
            AdPlacement(NATIVE_ON_BOARDING, AdFormat.NATIVE, id(R.string.admob_native_on_boarding_id)) { prefs.isAdEnabled(NATIVE_ON_BOARDING) },
            AdPlacement(NATIVE_FEATURE, AdFormat.NATIVE, id(R.string.admob_native_feature_id)) { prefs.isAdEnabled(NATIVE_FEATURE) },
            AdPlacement(NATIVE_SETTINGS, AdFormat.NATIVE, id(R.string.admob_native_settings_id)) { prefs.isAdEnabled(NATIVE_SETTINGS) },
        )
    }
}
