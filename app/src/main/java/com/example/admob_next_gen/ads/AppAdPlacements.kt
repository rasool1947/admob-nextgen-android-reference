package com.example.admob_next_gen.ads

import android.content.Context
import com.example.admob_next_gen.R
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement

/**
 * Every ad placement in this app. Ad unit ids come from app/build.gradle.kts (test ids for debug,
 * admob.properties for release); the on/off switches are stored per placement (see SharedPreferenceUtils.isAdEnabled).
 *
 * Non-full-screen positions are ad slots: the ads control picks native or banner at runtime, so each
 * slot has one native and one banner placement (see [AppAdSlot]).
 */
object AppAdPlacements {

    const val APP_OPEN = "app_open"

    const val INTER_ON_BOARDING = "inter_on_boarding"
    const val INTER_SPLASH = "inter_splash"

    /** Interstitial on navigation inside the main screen, paced by the ads control (`main.inter`). */
    const val INTER_MAIN = "inter_main"

    const val REWARDED_AI_FEATURE = "rewarded_ai_feature"
    const val REWARDED_INTER_AI_FEATURE = "rewarded_inter_ai_feature"

    const val NATIVE_SPLASH = "native_splash"
    const val BANNER_SPLASH = "banner_splash"
    const val NATIVE_LANGUAGE = "native_language"
    const val BANNER_LANGUAGE = "banner_language"
    const val NATIVE_ON_BOARDING = "native_on_boarding"
    const val BANNER_ON_BOARDING = "banner_on_boarding"
    const val NATIVE_MAIN = "native_main"
    const val BANNER_MAIN = "banner_main"
    const val NATIVE_TAB = "native_tab"
    const val BANNER_TAB = "banner_tab"

    /** Fixed native ad of the Feature screen (not driven by the ads control). */
    const val NATIVE_FEATURE = "native_feature"

    fun create(context: Context, prefs: SharedPreferenceUtils): List<AdPlacement> {
        fun id(resId: Int) = context.getString(resId).trim()
        fun placement(key: String, format: AdFormat, adUnitRes: Int) = AdPlacement(key, format, id(adUnitRes)) { prefs.isAdEnabled(key) }

        return listOf(
            placement(APP_OPEN, AdFormat.APP_OPEN, R.string.admob_app_open_id),

            placement(INTER_ON_BOARDING, AdFormat.INTERSTITIAL, R.string.admob_inter_on_boarding_id),
            placement(INTER_SPLASH, AdFormat.INTERSTITIAL, R.string.admob_inter_splash_id),
            placement(INTER_MAIN, AdFormat.INTERSTITIAL, R.string.admob_inter_main_id),

            placement(REWARDED_AI_FEATURE, AdFormat.REWARDED, R.string.admob_rewarded_ai_feature_id),
            placement(REWARDED_INTER_AI_FEATURE, AdFormat.REWARDED_INTERSTITIAL, R.string.admob_rewarded_inter_ai_feature_id),

            placement(NATIVE_SPLASH, AdFormat.NATIVE, R.string.admob_native_splash_id),
            placement(BANNER_SPLASH, AdFormat.BANNER, R.string.admob_banner_splash_id),
            placement(NATIVE_LANGUAGE, AdFormat.NATIVE, R.string.admob_native_language_id),
            placement(BANNER_LANGUAGE, AdFormat.BANNER, R.string.admob_banner_language_id),
            placement(NATIVE_ON_BOARDING, AdFormat.NATIVE, R.string.admob_native_on_boarding_id),
            placement(BANNER_ON_BOARDING, AdFormat.BANNER, R.string.admob_banner_on_boarding_id),
            placement(NATIVE_MAIN, AdFormat.NATIVE, R.string.admob_native_main_id),
            placement(BANNER_MAIN, AdFormat.BANNER, R.string.admob_banner_main_id),
            placement(NATIVE_TAB, AdFormat.NATIVE, R.string.admob_native_tab_id),
            placement(BANNER_TAB, AdFormat.BANNER, R.string.admob_banner_tab_id),

            placement(NATIVE_FEATURE, AdFormat.NATIVE, R.string.admob_native_feature_id),
        )
    }
}
