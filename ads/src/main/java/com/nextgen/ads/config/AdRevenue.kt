package com.nextgen.ads.config

/**
 * One paid impression, reported through [AdsConfig.onAdPaid]. Forward it to your analytics
 * (e.g. Firebase `ad_impression`) for ad-revenue / LTV reporting.
 *
 * @param valueMicros   Revenue in micros of [currencyCode] (1_000_000 = 1.00).
 * @param precision     How exact the value is: UNKNOWN, ESTIMATED, PUBLISHER_PROVIDED or PRECISE.
 */
data class AdRevenue(
    val placementKey: String,
    val format: AdFormat,
    val adUnitId: String,
    val valueMicros: Long,
    val currencyCode: String,
    val precision: String,
)
