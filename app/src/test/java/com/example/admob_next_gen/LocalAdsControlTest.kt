package com.example.admob_next_gen

import com.example.admob_next_gen.ads.LocalAdsControl
import com.nextgen.ads.control.AdsControlParser
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAdsControlTest {

    /** Catches typos in the shipped JSON: any unknown value would silently fall back to a default. */
    @Test
    fun `local ads control parses without warnings`() {
        val result = AdsControlParser.parse(LocalAdsControl.JSON)

        assertTrue(result.warnings.toString(), result.warnings.isEmpty())
    }
}
