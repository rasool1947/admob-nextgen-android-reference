package com.example.admob_next_gen.utilities.manager

import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferenceUtils(private val sharedPreferences: SharedPreferences) {

    /* ---------- First-run flow ---------- */

    /** True once the user picked a language; the splash then skips the Language screen. */
    var isLanguageSelected: Boolean
        get() = sharedPreferences.getBoolean(KEY_LANGUAGE_SELECTED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_LANGUAGE_SELECTED, value) }

    /** True once onboarding was finished; the splash then goes straight to the main screen. */
    var isOnboardingDone: Boolean
        get() = sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    /* ---------- Billing ---------- */

    /** True after the user bought "remove ads"; the ads module then stops serving every ad. */
    var isAppPurchased: Boolean
        get() = sharedPreferences.getBoolean(KEY_APP_PURCHASED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_APP_PURCHASED, value) }

    /* ---------- Ads (remote on/off switches) ---------- */

    /** Per-placement switch, read on every ad load. Defaults to enabled. */
    fun isAdEnabled(placementKey: String): Boolean =
        sharedPreferences.getBoolean(adEnabledKey(placementKey), true)

    /** Store the value fetched from your remote config (e.g. Firebase Remote Config) here. */
    fun setAdEnabled(placementKey: String, enabled: Boolean) =
        sharedPreferences.edit { putBoolean(adEnabledKey(placementKey), enabled) }

    private fun adEnabledKey(placementKey: String) = "ad_enabled_$placementKey"

    private companion object {
        const val KEY_LANGUAGE_SELECTED = "isLanguageSelected"
        const val KEY_ONBOARDING_DONE = "isOnboardingDone"
        const val KEY_APP_PURCHASED = "isAppPurchased"
    }
}
