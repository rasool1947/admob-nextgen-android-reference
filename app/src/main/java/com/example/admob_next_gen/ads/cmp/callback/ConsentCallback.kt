package com.example.admob_next_gen.ads.cmp.callback

/**
 * Date: 2/10/2025
 *
 */

interface ConsentCallback {
    fun onAdsLoad(canRequestAd: Boolean) {}
    fun onConsentFormLoaded() {}
    fun onConsentFormDismissed() {}
    fun onPolicyStatus(required: Boolean) {}
}