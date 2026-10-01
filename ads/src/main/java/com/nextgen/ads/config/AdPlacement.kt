package com.nextgen.ads.config

enum class AdFormat {
    APP_OPEN,
    BANNER,
    INTERSTITIAL,
    REWARDED,
    REWARDED_INTERSTITIAL,
    NATIVE,
}

/**
 * One place in the app where an ad can appear, e.g. "interstitial after onboarding".
 *
 * Every placement should have its own ad unit in the AdMob console so its revenue is reported
 * separately. Placements are declared by the app and registered through [AdsConfig.placements].
 *
 * @param key       Unique, stable id. Used for logs and per-placement ad caching.
 * @param format    Ad format this placement serves.
 * @param adUnitId  AdMob ad unit id (`ca-app-pub-xxx/yyy`).
 * @param isEnabled Evaluated on every load, so it can follow remote config at runtime.
 */
data class AdPlacement(
    val key: String,
    val format: AdFormat,
    val adUnitId: String,
    val isEnabled: () -> Boolean = { true },
) {
    init {
        require(key.isNotBlank()) { "AdPlacement key must not be blank" }
    }
}
