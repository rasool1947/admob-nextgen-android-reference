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

    /** App Open at launch (splash) and on returning to the app: two separate ads with their own units. */
    const val APP_OPEN_SPLASH = "app_open_splash"
    const val APP_OPEN_RESUME = "app_open_resume"

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
        fun placement(key: String, format: AdFormat, adUnitRes: Int, screen: String) =
            AdPlacement(key, format, id(adUnitRes), isEnabled = { prefs.isAdEnabled(key) }, screen = screen)

        return listOf(
            placement(APP_OPEN_SPLASH, AdFormat.APP_OPEN, R.string.admob_app_open_splash_id, "Splash"),
            placement(APP_OPEN_RESUME, AdFormat.APP_OPEN, R.string.admob_app_open_resume_id, "Resume"),

            placement(INTER_ON_BOARDING, AdFormat.INTERSTITIAL, R.string.admob_inter_on_boarding_id, "Onboarding"),
            placement(INTER_SPLASH, AdFormat.INTERSTITIAL, R.string.admob_inter_splash_id, "Splash"),
            placement(INTER_MAIN, AdFormat.INTERSTITIAL, R.string.admob_inter_main_id, "Main"),

            placement(REWARDED_AI_FEATURE, AdFormat.REWARDED, R.string.admob_rewarded_ai_feature_id, "Premium"),
            placement(
                REWARDED_INTER_AI_FEATURE, AdFormat.REWARDED_INTERSTITIAL, R.string.admob_rewarded_inter_ai_feature_id, "Premium",
            ),

            placement(NATIVE_SPLASH, AdFormat.NATIVE, R.string.admob_native_splash_id, "Splash"),
            placement(BANNER_SPLASH, AdFormat.BANNER, R.string.admob_banner_splash_id, "Splash"),
            placement(NATIVE_LANGUAGE, AdFormat.NATIVE, R.string.admob_native_language_id, "Language"),
            placement(BANNER_LANGUAGE, AdFormat.BANNER, R.string.admob_banner_language_id, "Language"),
            placement(NATIVE_ON_BOARDING, AdFormat.NATIVE, R.string.admob_native_on_boarding_id, "Onboarding"),
            placement(BANNER_ON_BOARDING, AdFormat.BANNER, R.string.admob_banner_on_boarding_id, "Onboarding"),
            placement(NATIVE_MAIN, AdFormat.NATIVE, R.string.admob_native_main_id, "Main bottom"),
            placement(BANNER_MAIN, AdFormat.BANNER, R.string.admob_banner_main_id, "Main bottom"),
            placement(NATIVE_TAB, AdFormat.NATIVE, R.string.admob_native_tab_id, "Main tab"),
            placement(BANNER_TAB, AdFormat.BANNER, R.string.admob_banner_tab_id, "Main tab"),

            placement(NATIVE_FEATURE, AdFormat.NATIVE, R.string.admob_native_feature_id, "Feature"),
        )
    }
}
