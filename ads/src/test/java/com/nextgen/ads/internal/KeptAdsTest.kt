package com.nextgen.ads.internal

import com.nextgen.ads.control.CacheControl
import com.nextgen.ads.internal.KeptAds.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class KeptAdsTest {

    private val cache = CacheControl(reuseShownMillis = 30_000, maxAgeMillis = 50 * 60_000)

    @Test
    fun `never seen ad is reused without a request`() {
        val times = KeptAds.AdTimes(loadedAtMillis = 0)

        assertEquals(Decision.REUSE, KeptAds.decide(times, nowMillis = 10 * 60_000, cache))
    }

    @Test
    fun `seen recently is reused, seen long ago is refreshed`() {
        val times = KeptAds.AdTimes(loadedAtMillis = 0).apply { onImpression(atMillis = 1_000) }

        assertEquals(Decision.REUSE, KeptAds.decide(times, nowMillis = 30_999, cache))
        assertEquals(Decision.REUSE_AND_REFRESH, KeptAds.decide(times, nowMillis = 31_000, cache))
    }

    @Test
    fun `too old since loading expires, seen or not`() {
        val seen = KeptAds.AdTimes(loadedAtMillis = 0).apply { onImpression(atMillis = 1_000) }
        val unseen = KeptAds.AdTimes(loadedAtMillis = 0)

        assertEquals(Decision.EXPIRED, KeptAds.decide(seen, nowMillis = 50 * 60_000, cache))
        assertEquals(Decision.EXPIRED, KeptAds.decide(unseen, nowMillis = 50 * 60_000, cache))
    }

    @Test
    fun `banner refresh restarts the age, a native impression does not`() {
        val banner = KeptAds.AdTimes(loadedAtMillis = 0).apply {
            onImpression(isNewCreative = true, atMillis = 1_000)
            onImpression(isNewCreative = true, atMillis = 49 * 60_000)
        }
        val native = KeptAds.AdTimes(loadedAtMillis = 0).apply { onImpression(atMillis = 49 * 60_000) }

        assertEquals(Decision.REUSE_AND_REFRESH, KeptAds.decide(banner, nowMillis = 51 * 60_000, cache))
        assertEquals(Decision.EXPIRED, KeptAds.decide(native, nowMillis = 51 * 60_000, cache))
    }

    @Test
    fun `max age 0 keeps nothing`() {
        val times = KeptAds.AdTimes(loadedAtMillis = 0)

        assertEquals(Decision.EXPIRED, KeptAds.decide(times, nowMillis = 1, cache.copy(maxAgeMillis = 0)))
    }

    @Test
    fun `reuse 0 refreshes on every return after an impression`() {
        val times = KeptAds.AdTimes(loadedAtMillis = 0).apply { onImpression(atMillis = 1_000) }

        assertEquals(Decision.REUSE_AND_REFRESH, KeptAds.decide(times, nowMillis = 1_000, cache.copy(reuseShownMillis = 0)))
    }
}
