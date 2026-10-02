package com.nextgen.ads.control

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsControlParserTest {

    @Test
    fun `empty object gives defaults without warnings`() {
        val result = AdsControlParser.parse("{}")

        assertEquals(AdsControl(), result.control)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `reads every section`() {
        val json = """
            {
              "ads_enabled": true,
              "app_open_resume": false,
              "splash": { "fullscreen": "inter", "timeout_sec": 10, "bottom_first_ms": 1500,
                          "bottom": { "type": "banner", "style": "large" } },
              "language": { "bottom": "off" },
              "onboarding": { "mode": "shared",
                              "pages": [ { "type": "native", "style": "large" }, { "type": "off" }, { "type": "banner", "style": "medium_rectangle" } ],
                              "shared": { "type": "native", "style": "small" },
                              "get_started_inter": false },
              "main": { "bottom": { "type": "banner", "style": "collapsible_bottom" },
                        "tabs": { "home": { "type": "native", "style": "medium" }, "explore": "off" },
                        "inter": { "enabled": true, "every_nth": 2, "show_on_first_click": true, "min_interval_sec": 0 } }
            }
        """.trimIndent()

        val result = AdsControlParser.parse(json)
        val control = result.control

        assertTrue(result.warnings.toString(), result.warnings.isEmpty())
        assertFalse(control.appOpenOnResume)
        assertEquals(
            SplashControl(SplashFullScreen.INTERSTITIAL, AdSlot.Banner(BannerStyle.LARGE), timeoutMillis = 10_000, bottomFirstMillis = 1_500),
            control.splash,
        )
        assertEquals(AdSlot.Off, control.language.bottom)
        assertEquals(OnboardingAdMode.SHARED, control.onboarding.mode)
        assertEquals(3, control.onboarding.pageCount)
        assertEquals(AdSlot.Native(NativeStyle.LARGE), control.onboarding.page(0))
        assertEquals(AdSlot.Off, control.onboarding.page(1))
        assertEquals(AdSlot.Banner(BannerStyle.MEDIUM_RECTANGLE), control.onboarding.page(2))
        assertEquals(AdSlot.Off, control.onboarding.page(99))
        assertEquals(AdSlot.Native(NativeStyle.SMALL), control.onboarding.shared)
        assertFalse(control.onboarding.getStartedInter)
        assertEquals(AdSlot.Banner(BannerStyle.COLLAPSIBLE_BOTTOM), control.main.bottom)
        assertEquals(AdSlot.Native(NativeStyle.MEDIUM), control.main.tab("home"))
        assertEquals(AdSlot.Off, control.main.tab("explore"))
        assertEquals(AdSlot.Off, control.main.tab("unknown"))
        assertEquals(MainInterControl(enabled = true, everyNth = 2, showOnFirstClick = true, minIntervalMillis = 0), control.main.inter)
    }

    @Test
    fun `invalid values keep defaults and are reported`() {
        val json = """
            {
              "ads_enabled": "yes",
              "splash": { "fullscreen": "video", "timeout_sec": -5, "bottom": { "type": "popup" } },
              "language": { "bottom": { "type": "native", "style": "huge" } },
              "onboarding": { "pages": "four" },
              "main": { "inter": { "every_nth": 0 } }
            }
        """.trimIndent()

        val result = AdsControlParser.parse(json)
        val defaults = AdsControl()

        assertTrue(result.control.adsEnabled)
        assertEquals(defaults.splash, result.control.splash)
        assertEquals(AdSlot.Native(NativeStyle.MEDIUM), result.control.language.bottom)
        assertEquals(defaults.onboarding.pages, result.control.onboarding.pages)
        assertEquals(1, result.control.main.inter.everyNth)
        assertEquals(result.warnings.toString(), 6, result.warnings.size)
    }

    @Test(expected = JSONException::class)
    fun `not json at all throws`() {
        AdsControlParser.parse("ads on please")
    }

    @Test
    fun `allOff turns every ad off but keeps page count`() {
        val control = AdsControl(main = MainControl(tabs = mapOf("home" to AdSlot.Native(NativeStyle.SMALL)))).allOff()

        assertFalse(control.adsEnabled)
        assertFalse(control.appOpenOnResume)
        assertEquals(SplashFullScreen.OFF, control.splash.fullScreen)
        assertFalse(control.splash.bottom.isOn)
        assertFalse(control.language.bottom.isOn)
        assertEquals(OnboardingControl.DEFAULT_PAGE_COUNT, control.onboarding.pageCount)
        assertTrue(control.onboarding.pages.none { it.isOn })
        assertFalse(control.onboarding.shared.isOn)
        assertFalse(control.onboarding.getStartedInter)
        assertFalse(control.main.bottom.isOn)
        assertFalse(control.main.tab("home").isOn)
        assertFalse(control.main.inter.enabled)
    }
}
