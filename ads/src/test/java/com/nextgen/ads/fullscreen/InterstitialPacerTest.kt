package com.nextgen.ads.fullscreen

import com.nextgen.ads.control.MainInterControl
import org.junit.Assert.assertEquals
import org.junit.Test

class InterstitialPacerTest {

    private var now = 1_000_000L
    private var lastShown = 0L

    private fun pacer(rules: MainInterControl) = InterstitialPacer({ rules }, { lastShown }, { now })

    /** Clicks [count] times; returns the 1-based clicks that showed an ad (shown ads are reported back). */
    private fun InterstitialPacer.click(count: Int, secondsBetween: Long = 0): List<Int> =
        (1..count).filter { click ->
            now += secondsBetween * 1000
            onClick().also { show ->
                if (show) {
                    onShown()
                    lastShown = now
                }
            }
        }

    @Test
    fun `every third click`() {
        val pacer = pacer(MainInterControl(everyNth = 3, showOnFirstClick = false, minIntervalMillis = 0))
        assertEquals(listOf(3, 6, 9), pacer.click(9))
    }

    @Test
    fun `first click then every second`() {
        val pacer = pacer(MainInterControl(everyNth = 2, showOnFirstClick = true, minIntervalMillis = 0))
        assertEquals(listOf(1, 3, 5), pacer.click(6))
    }

    @Test
    fun `every click when nth is 1`() {
        val pacer = pacer(MainInterControl(everyNth = 1, showOnFirstClick = false, minIntervalMillis = 0))
        assertEquals(listOf(1, 2, 3), pacer.click(3))
    }

    @Test
    fun `disabled never shows`() {
        val pacer = pacer(MainInterControl(enabled = false, everyNth = 1, showOnFirstClick = true))
        assertEquals(emptyList<Int>(), pacer.click(5))
    }

    @Test
    fun `min interval delays a due ad until it is over, then the next click shows`() {
        val pacer = pacer(MainInterControl(everyNth = 2, showOnFirstClick = false, minIntervalMillis = 30_000))
        // Clicks 10 s apart: due on 2 (first ad), then due on 4 but only 20 s passed -> waits, 5 is 30 s -> shows.
        assertEquals(listOf(2, 5), pacer.click(6, secondsBetween = 10))
    }

    @Test
    fun `a recent app open ad counts for the interval`() {
        lastShown = now // e.g. App Open just closed
        val pacer = pacer(MainInterControl(everyNth = 1, showOnFirstClick = true, minIntervalMillis = 30_000))
        // Click 1 at 15 s is too soon; click 2 at 30 s shows; click 3 is 15 s after that ad.
        assertEquals(listOf(2), pacer.click(3, secondsBetween = 15))
    }

    @Test
    fun `a due click whose ad is not ready keeps the turn`() {
        val pacer = pacer(MainInterControl(everyNth = 2, showOnFirstClick = false, minIntervalMillis = 0))
        assertEquals(false, pacer.onClick())
        assertEquals(true, pacer.onClick()) // due, but say no ad was ready: onShown() not called
        assertEquals(true, pacer.onClick()) // still due on the next click
    }
}
