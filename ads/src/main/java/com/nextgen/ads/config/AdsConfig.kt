package com.nextgen.ads.config

/**
 * Everything the ads module needs to know about the host app. Build it once in
 * `Application.onCreate()` and pass it to [com.nextgen.ads.AdsSdk.configure].
 *
 * @param appId          AdMob App ID (`ca-app-pub-xxx~yyy`). Must match the manifest's
 *                       `com.google.android.gms.ads.APPLICATION_ID` meta-data.
 * @param placements     All ad placements of the app.
 * @param isPremium      Return true to suppress every ad (e.g. user bought "remove ads").
 * @param testDeviceIds  Hashed device ids that always receive test ads and are treated as consent
 *                       test devices. Logcat prints the id of the current device on first request.
 * @param isDebug        Pass `BuildConfig.DEBUG`. Enables the consent debug options below and
 *                       verbose logging; has no effect on release builds.
 * @param debugGeographyEea  Debug only: make test devices behave as if in the EEA, so the
 *                           consent form and privacy options can be tested from anywhere.
 * @param resetConsentOnLaunch  Debug only: forget stored consent on every launch.
 */
data class AdsConfig(
    val appId: String,
    val placements: List<AdPlacement>,
    val isPremium: () -> Boolean = { false },
    val testDeviceIds: List<String> = emptyList(),
    val isDebug: Boolean = false,
    val debugGeographyEea: Boolean = false,
    val resetConsentOnLaunch: Boolean = false,
) {
    init {
        val duplicates = placements.groupBy { it.key }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate AdPlacement keys: $duplicates" }
    }

    fun placement(key: String): AdPlacement =
        placements.firstOrNull { it.key == key } ?: error("No AdPlacement registered with key '$key'")
}
