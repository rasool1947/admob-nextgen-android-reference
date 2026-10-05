package com.nextgen.ads.control

/**
 * Runtime switches for every ad in the standard app flow:
 * Splash -> Language -> Onboarding -> Main (bottom navigation).
 *
 * Built from a JSON document (see [AdsControlParser] for the format): a local default today,
 * the same JSON from one Firebase Remote Config key in a real app. Ad unit ids are NOT part of
 * it; they stay in Gradle / admob.properties.
 *
 * Read the current value from [AdsControlStore.current], which already applies [adsEnabled].
 */
data class AdsControl(
    /** Master switch: false turns every ad off at once. */
    val adsEnabled: Boolean = true,
    /** App open ad when the user comes back to the app. */
    val appOpenOnResume: Boolean = true,
    val splash: SplashControl = SplashControl(),
    val language: LanguageControl = LanguageControl(),
    val onboarding: OnboardingControl = OnboardingControl(),
    val main: MainControl = MainControl(),
    val cache: CacheControl = CacheControl(),
) {
    /** Same switches with every ad turned off; what the app sees when [adsEnabled] is false. */
    fun allOff(): AdsControl = AdsControl(
        adsEnabled = false,
        appOpenOnResume = false,
        splash = splash.copy(fullScreen = SplashFullScreen.OFF, bottom = AdSlot.Off),
        language = LanguageControl(bottom = AdSlot.Off),
        onboarding = onboarding.copy(pages = onboarding.pages.map { AdSlot.Off }, shared = AdSlot.Off, getStartedInter = false),
        main = main.copy(bottom = AdSlot.Off, tabs = main.tabs.mapValues { AdSlot.Off }, inter = main.inter.copy(enabled = false)),
    )
}

/** What a non-full-screen ad position shows: a native ad, a banner, or nothing. */
sealed interface AdSlot {
    data object Off : AdSlot
    data class Native(val style: NativeStyle) : AdSlot
    data class Banner(val style: BannerStyle) : AdSlot

    val isOn: Boolean get() = this != Off
}

enum class NativeStyle(val jsonName: String) {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
}

enum class BannerStyle(val jsonName: String) {
    /** 320x50 */
    STANDARD("standard"),
    /** 320x100 */
    LARGE("large"),
    /** 300x250 */
    MEDIUM_RECTANGLE("medium_rectangle"),
    /** Full width, height picked by the SDK. The usual choice for a bottom banner. */
    ADAPTIVE("adaptive"),
    /** Full width, taller ads; for banners inside scrolling content. */
    INLINE_ADAPTIVE("inline_adaptive"),
    /** Adaptive banner that opens larger and collapses; anchored at the top of the screen. */
    COLLAPSIBLE_TOP("collapsible_top"),
    /** Adaptive banner that opens larger and collapses; anchored at the bottom of the screen. */
    COLLAPSIBLE_BOTTOM("collapsible_bottom"),
}

enum class SplashFullScreen(val jsonName: String) {
    APP_OPEN("app_open"),
    INTERSTITIAL("inter"),
    OFF("off"),
}

data class SplashControl(
    val fullScreen: SplashFullScreen = SplashFullScreen.APP_OPEN,
    val bottom: AdSlot = AdSlot.Native(NativeStyle.MEDIUM),
    /** Longest the splash waits for its ads before moving on. */
    val timeoutMillis: Long = 25_000L,
    /** When the bottom ad is ready first, it stays visible this long before the full-screen ad. */
    val bottomFirstMillis: Long = 2_000L,
)

data class LanguageControl(
    val bottom: AdSlot = AdSlot.Native(NativeStyle.MEDIUM),
)

enum class OnboardingAdMode(val jsonName: String) {
    /** Every page shows its own ad from [OnboardingControl.pages]. */
    PER_PAGE("per_page"),
    /** One ad below the pager, the same for all pages: [OnboardingControl.shared]. */
    SHARED("shared"),
}

data class OnboardingControl(
    val mode: OnboardingAdMode = OnboardingAdMode.PER_PAGE,
    /** One entry per onboarding page; its size is the page count. */
    val pages: List<AdSlot> = List(DEFAULT_PAGE_COUNT) { AdSlot.Native(NativeStyle.MEDIUM) },
    val shared: AdSlot = AdSlot.Native(NativeStyle.MEDIUM),
    /** Interstitial when "Get Started" on the last page is tapped. */
    val getStartedInter: Boolean = true,
) {
    val pageCount: Int get() = pages.size

    /** The ad of page [index] (0-based) in [OnboardingAdMode.PER_PAGE] mode. */
    fun page(index: Int): AdSlot = pages.getOrElse(index) { AdSlot.Off }

    /** The ad shown while page [index] is on screen, for either [mode]. */
    fun slotForPage(index: Int): AdSlot = when (mode) {
        OnboardingAdMode.PER_PAGE -> page(index)
        OnboardingAdMode.SHARED -> shared
    }

    companion object {
        const val DEFAULT_PAGE_COUNT = 4
    }
}

data class MainControl(
    /** Ad below the bottom navigation. */
    val bottom: AdSlot = AdSlot.Banner(BannerStyle.ADAPTIVE),
    /** Ad inside each tab's content, keyed by a tab name the app chooses (e.g. "home"). */
    val tabs: Map<String, AdSlot> = emptyMap(),
    val inter: MainInterControl = MainInterControl(),
) {
    fun tab(key: String): AdSlot = tabs[key] ?: AdSlot.Off
}

/** Interstitial on navigation inside the main screen. */
data class MainInterControl(
    val enabled: Boolean = true,
    /** Show on every nth eligible click (2 = every second click). 1 = every click. */
    val everyNth: Int = 3,
    /** Also show on the very first click, then continue counting from there. */
    val showOnFirstClick: Boolean = false,
    /** Never show two interstitials closer together than this. 0 = no limit. */
    val minIntervalMillis: Long = 30_000L,
)

/**
 * How native/banner ads are kept for their screen (per placement) once the screen closes, so that
 * coming back shows an ad at once and no request is wasted.
 */
data class CacheControl(
    /**
     * Coming back within this time after the kept ad's last impression shows it again with no new
     * request (tab switches, quick back and forth). Later, the kept ad still shows at once, a new one
     * is requested and swapped in when it arrives (a fresh impression); if that fails the kept one
     * stays. A kept ad that never got an impression is always shown as is.
     */
    val reuseShownMillis: Long = 30_000L,
    /** A kept ad older than this (since it loaded) is destroyed instead of shown. 0 = keep nothing. */
    val maxAgeMillis: Long = 50 * 60_000L,
)
