package com.example.admob_next_gen.utilities.manager

import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferenceUtils(private val sharedPreferences: SharedPreferences) {

    /* ---------- First-run flow ---------- */

    /**
     * True once the user reached the main screen from onboarding. Until then every launch runs the
     * whole first flow again (Splash -> Language -> Onboarding -> Main), even if a language was
     * already picked before the app was closed.
     */
    var isFirstFlowDone: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_FLOW_DONE, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_FIRST_FLOW_DONE, value) }

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
        const val KEY_FIRST_FLOW_DONE = "isFirstFlowDone"
        const val KEY_APP_PURCHASED = "isAppPurchased"
    }
}
