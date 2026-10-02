package com.nextgen.ads.internal

import android.os.SystemClock
import com.nextgen.ads.AdsSdk

/**
 * Native and banner ads whose screen was closed, kept per placement key, so that when the screen
 * opens again it shows the same ad instead of sending a new request (e.g. coming back to the main
 * screen from another screen, rotation, swiping back to an onboarding page).
 *
 * An ad older than [com.nextgen.ads.config.AdsConfig.keptAdMaxAgeMillis] is destroyed instead of
 * shown (AdMob ads go stale after about an hour). Main thread only.
 */
internal object KeptAds {

    private class Kept(val ad: Any, val variant: Any?, val loadedAtMillis: Long, val destroy: () -> Unit)

    class Taken<T>(val ad: T, val loadedAtMillis: Long)

    private val kept = mutableMapOf<String, Kept>()

    private val maxAgeMillis get() = AdsSdk.config.keptAdMaxAgeMillis

    /**
     * Keeps [ad] for [placementKey]. [variant] must match on [take] (e.g. a banner's size), so an ad
     * isn't reused where it no longer fits. [destroy] runs if the ad is never used again.
     */
    fun keep(placementKey: String, ad: Any, variant: Any?, loadedAtMillis: Long, destroy: () -> Unit) {
        kept.remove(placementKey)?.destroy?.invoke()
        if (maxAgeMillis <= 0 || isExpired(loadedAtMillis)) return destroy()
        kept[placementKey] = Kept(ad, variant, loadedAtMillis, destroy)
    }

    /** The kept ad of [placementKey], if it is still fresh and for the same [variant]; it is no longer kept. */
    @Suppress("UNCHECKED_CAST")
    fun <T> take(placementKey: String, variant: Any?): Taken<T>? {
        val entry = kept.remove(placementKey) ?: return null
        if (entry.variant != variant || isExpired(entry.loadedAtMillis)) {
            entry.destroy()
            return null
        }
        return Taken(entry.ad as T, entry.loadedAtMillis)
    }

    fun now(): Long = SystemClock.elapsedRealtime()

    private fun isExpired(loadedAtMillis: Long) = now() - loadedAtMillis >= maxAgeMillis
}
