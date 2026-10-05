package com.nextgen.ads.internal

import android.os.SystemClock
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.control.CacheControl

/**
 * Native and banner ads whose screen was closed, kept per placement key, so that when the screen
 * opens again it shows an ad at once instead of a shimmer (back from another screen, a tab switch,
 * rotation, swiping back to an onboarding page). Rules: [CacheControl].
 *
 * - Never seen (no impression yet), or seen less than [CacheControl.reuseShownMillis] ago:
 *   shown again, no request.
 * - Seen longer ago: shown again AND a new ad is requested, swapped in when it arrives
 *   ([Taken.needsRefresh]), for a fresh impression.
 * - Older than [CacheControl.maxAgeMillis] since it loaded (AdMob ads go stale after about an
 *   hour): destroyed, the screen loads normally.
 *
 * Main thread only.
 */
internal object KeptAds {

    /** When an ad loaded and when it was last seen; the ad's impression callback calls [onImpression]. */
    class AdTimes(loadedAtMillis: Long = now()) {
        var loadedAtMillis: Long = loadedAtMillis
            private set
        var lastImpressionAtMillis: Long? = null
            private set

        /**
         * @param isNewCreative A banner's automatic refresh: every impression after the first is a newly
         *                      loaded ad in the same BannerAd, so its age starts again too.
         */
        fun onImpression(isNewCreative: Boolean = false, atMillis: Long = now()) {
            if (isNewCreative && lastImpressionAtMillis != null) loadedAtMillis = atMillis
            lastImpressionAtMillis = atMillis
        }
    }

    private class Kept(val ad: Any, val variant: Any?, val times: AdTimes, val destroy: () -> Unit)

    /**
     * @param needsRefresh Show [ad] now, and request a new one to replace it.
     * @param seenAgoMillis Time since the ad's last impression, null if it was never seen (for the log).
     */
    class Taken<T>(val ad: T, val times: AdTimes, val needsRefresh: Boolean, val seenAgoMillis: Long?)

    enum class Decision { REUSE, REUSE_AND_REFRESH, EXPIRED }

    private val kept = mutableMapOf<String, Kept>()

    private val cache: CacheControl get() = AdsControlStore.current.cache

    /**
     * Keeps [ad] for [placementKey], replacing (and destroying) the one kept before. [variant] must
     * match on [take] (e.g. a banner's size), so an ad isn't reused where it no longer fits.
     * [destroy] runs if the ad is never used again.
     */
    fun keep(placementKey: String, ad: Any, variant: Any?, times: AdTimes, destroy: () -> Unit) {
        kept.remove(placementKey)?.destroy?.invoke()
        if (decide(times, now(), cache) == Decision.EXPIRED) return destroy()
        kept[placementKey] = Kept(ad, variant, times, destroy)
    }

    /** The kept ad of [placementKey], unless expired or for another [variant]; it is no longer kept. */
    @Suppress("UNCHECKED_CAST")
    fun <T> take(placementKey: String, variant: Any?): Taken<T>? {
        val entry = kept.remove(placementKey) ?: return null
        val now = now()
        val decision = decide(entry.times, now, cache)
        if (entry.variant != variant || decision == Decision.EXPIRED) {
            entry.destroy()
            return null
        }
        val seenAgo = entry.times.lastImpressionAtMillis?.let { now - it }
        return Taken(entry.ad as T, entry.times, decision == Decision.REUSE_AND_REFRESH, seenAgo)
    }

    fun now(): Long = SystemClock.elapsedRealtime()

    /** What to do with a kept ad when its screen opens again. */
    fun decide(times: AdTimes, nowMillis: Long, cache: CacheControl): Decision {
        if (cache.maxAgeMillis <= 0 || nowMillis - times.loadedAtMillis >= cache.maxAgeMillis) return Decision.EXPIRED
        val lastImpression = times.lastImpressionAtMillis ?: return Decision.REUSE // never seen: still worth an impression
        return if (nowMillis - lastImpression < cache.reuseShownMillis) Decision.REUSE else Decision.REUSE_AND_REFRESH
    }

    /** "12s" / "3m 5s", for the log. */
    fun agoText(millis: Long?): String {
        if (millis == null) return "not seen yet"
        val seconds = millis / 1000
        return "seen " + (if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s") + " ago"
    }
}
