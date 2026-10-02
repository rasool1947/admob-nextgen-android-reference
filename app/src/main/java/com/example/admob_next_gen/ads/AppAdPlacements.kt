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
    /** Onboarding with one shared ad for all pages (`onboarding.mode = shared`). */
    const val NATIVE_ON_BOARDING = "native_on_boarding"
    const val BANNER_ON_BOARDING = "banner_on_boarding"

    /** Onboarding pages 1-4, each with its own ad units and cache (`onboarding.mode = per_page`). */
    val NATIVE_OB = List(4) { i -> "native_ob${i + 1}" }
    val BANNER_OB = List(4) { i -> "banner_ob${i + 1}" }
    const val NATIVE_MAIN = "native_main"
    const val BANNER_MAIN = "banner_main"
    /** Inside each main-screen tab: its own units and cache per tab. */
    const val NATIVE_HOME = "native_home"
    const val BANNER_HOME = "banner_home"
    const val NATIVE_EXPLORE = "native_explore"
    const val BANNER_EXPLORE = "banner_explore"
    const val NATIVE_HISTORY = "native_history"
    const val BANNER_HISTORY = "banner_history"
    const val NATIVE_SETTINGS = "native_settings"
    const val BANNER_SETTINGS = "banner_settings"

    /** Fixed native ad of the Feature screen (not driven by the ads control). */
    const val NATIVE_FEATURE = "native_feature"

    fun create(context: Context, prefs: SharedPreferenceUtils): List<AdPlacement> {
        fun id(resId: Int) = context.getString(resId).trim()
        fun placement(key: String, format: AdFormat, adUnitRes: Int, screen: String) =
            AdPlacement(key, format, id(adUnitRes), isEnabled = { prefs.isAdEnabled(key) }, screen = screen)

        return listOf(
            placement(APP_OPEN_SPLASH, AdFormat.APP_OPEN, R.string.admob_app_open_splash_id, "Splash"),
            placement(APP_OPEN_RESUME, AdFormat.APP_OPEN, R.string.admob_app_open_resume_id, "Resume"),

            placement(INTER_ON_BOARDING, AdFormat.INTERSTITIAL, R.string.admob_inter_on_boarding_id, "Get Started"),
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
            placement(NATIVE_ON_BOARDING, AdFormat.NATIVE, R.string.admob_native_on_boarding_id, "OB shared"),
            placement(BANNER_ON_BOARDING, AdFormat.BANNER, R.string.admob_banner_on_boarding_id, "OB shared"),
            placement(NATIVE_OB[0], AdFormat.NATIVE, R.string.admob_native_ob1_id, "OB1"),
            placement(BANNER_OB[0], AdFormat.BANNER, R.string.admob_banner_ob1_id, "OB1"),
            placement(NATIVE_OB[1], AdFormat.NATIVE, R.string.admob_native_ob2_id, "OB2"),
            placement(BANNER_OB[1], AdFormat.BANNER, R.string.admob_banner_ob2_id, "OB2"),
            placement(NATIVE_OB[2], AdFormat.NATIVE, R.string.admob_native_ob3_id, "OB3"),
            placement(BANNER_OB[2], AdFormat.BANNER, R.string.admob_banner_ob3_id, "OB3"),
            placement(NATIVE_OB[3], AdFormat.NATIVE, R.string.admob_native_ob4_id, "OB4"),
            placement(BANNER_OB[3], AdFormat.BANNER, R.string.admob_banner_ob4_id, "OB4"),
            placement(NATIVE_MAIN, AdFormat.NATIVE, R.string.admob_native_main_id, "Main bottom"),
            placement(BANNER_MAIN, AdFormat.BANNER, R.string.admob_banner_main_id, "Main bottom"),
            placement(NATIVE_HOME, AdFormat.NATIVE, R.string.admob_native_home_id, "Home tab"),
            placement(BANNER_HOME, AdFormat.BANNER, R.string.admob_banner_home_id, "Home tab"),
            placement(NATIVE_EXPLORE, AdFormat.NATIVE, R.string.admob_native_explore_id, "Explore tab"),
            placement(BANNER_EXPLORE, AdFormat.BANNER, R.string.admob_banner_explore_id, "Explore tab"),
            placement(NATIVE_HISTORY, AdFormat.NATIVE, R.string.admob_native_history_id, "History tab"),
            placement(BANNER_HISTORY, AdFormat.BANNER, R.string.admob_banner_history_id, "History tab"),
            placement(NATIVE_SETTINGS, AdFormat.NATIVE, R.string.admob_native_settings_id, "Settings tab"),
            placement(BANNER_SETTINGS, AdFormat.BANNER, R.string.admob_banner_settings_id, "Settings tab"),

            placement(NATIVE_FEATURE, AdFormat.NATIVE, R.string.admob_native_feature_id, "Feature"),
        )
    }
}
