package com.example.admob_next_gen.utilities.firebase

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.nextgen.ads.config.AdRevenue

/**
 * Sends every paid ad impression to Firebase Analytics (wired through `AdsConfig.onAdPaid`).
 * Does nothing until a google-services.json is added to app/.
 *
 * Uses a custom event on purpose: when the AdMob app is linked to Firebase, Google can already
 * report `ad_impression` for AdMob on its own, and logging it again would double count revenue.
 * If your AdMob app isn't linked, you can switch the name to FirebaseAnalytics.Event.AD_IMPRESSION
 * to get GA4's built-in ad revenue reports.
 */
object FirebaseAdRevenue {

    private const val EVENT_NAME = "ad_paid"

    fun log(context: Context, revenue: AdRevenue) {
        if (FirebaseApp.getApps(context).isEmpty()) return

        Firebase.analytics.logEvent(EVENT_NAME) {
            param(FirebaseAnalytics.Param.AD_PLATFORM, "admob")
            param(FirebaseAnalytics.Param.AD_FORMAT, revenue.format.name)
            param(FirebaseAnalytics.Param.AD_UNIT_NAME, revenue.adUnitId)
            param("placement", revenue.placementKey)
            param("precision", revenue.precision)
            param(FirebaseAnalytics.Param.VALUE, revenue.valueMicros / 1_000_000.0)
            param(FirebaseAnalytics.Param.CURRENCY, revenue.currencyCode)
        }
    }
}
