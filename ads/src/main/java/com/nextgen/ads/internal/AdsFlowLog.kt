package com.nextgen.ads.internal

import android.util.Log
import com.nextgen.ads.config.AdFormat
import com.nextgen.ads.config.AdPlacement

/**
 * One readable line per ad event, under its own logcat tag ([TAG]), so the life of every ad can be
 * followed screen by screen without the technical noise of [AdsLog]:
 * ```
 * Language      │ native_language           │ Native        │ ⏳ LOADING
 * Language      │ native_language           │ Native        │ ♻️ LOADED from cache
 * Language      │ native_language           │ Native        │ 👁 IMPRESSION
 * Onboarding    │ inter_on_boarding         │ Interstitial  │ ❌ FAILED  3: No fill
 * ```
 * Debug builds only (same switch as [AdsLog.verbose]). Filter logcat by `AdsFlow`.
 */
internal object AdsFlowLog {

    const val TAG = "AdsFlow"

    enum class Event(val label: String) {
        /** Started loading in the background for later (SDK preloader = the ad cache). */
        PRELOADING("📦 PRELOADING"),
        /** An ad is waiting in the cache, ready to show instantly. */
        READY_IN_CACHE("📦 READY in cache"),
        /** Full-screen ads are always loaded through the cache: this is their "loaded". */
        LOADED_IN_CACHE("✅ LOADED (in cache)"),
        /** A request for this screen went out (nothing in the cache). */
        LOADING("⏳ LOADING"),
        LOADED("✅ LOADED"),
        /** Taken from the cache: no waiting. */
        FROM_CACHE("♻️ LOADED from cache"),
        FAILED("❌ FAILED"),
        /** Not requested at all: slot off, no consent, premium user, placement disabled… */
        SKIPPED("⛔ SKIPPED"),
        /** A full-screen ad was asked to show but couldn't (none ready, another ad on screen…). */
        NOT_SHOWN("🚫 NOT SHOWN"),
        IMPRESSION("👁 IMPRESSION"),
        CLICKED("👆 CLICKED"),
        CLOSED("✖️ CLOSED"),
        /** Rewarded formats: the user earned the reward. */
        REWARDED("🎁 REWARD earned"),
    }

    /** Placements whose cache was already reported ready since their preload started (main thread only). */
    private val readyAnnounced = mutableSetOf<String>()

    /**
     * Logcat level = colour in Android Studio: loaded ads are warnings (yellow), failures errors (red),
     * everything else debug.
     */
    fun log(placement: AdPlacement, event: Event, detail: String? = null) {
        if (!AdsLog.verbose) return
        when (event) {
            Event.PRELOADING -> readyAnnounced -= placement.key
            // The SDK refills the cache after every use; only the first fill is news.
            Event.READY_IN_CACHE, Event.LOADED_IN_CACHE -> if (!readyAnnounced.add(placement.key)) return
            else -> Unit
        }

        val screen = (placement.screen ?: "-").take(SCREEN_WIDTH).padEnd(SCREEN_WIDTH)
        val key = placement.key.take(KEY_WIDTH).padEnd(KEY_WIDTH)
        val format = placement.format.label.padEnd(FORMAT_WIDTH)
        val line = "$screen │ $key │ $format │ ${event.label}" + (detail?.let { "  $it" } ?: "")
        when (event) {
            Event.LOADED, Event.FROM_CACHE, Event.LOADED_IN_CACHE -> Log.w(TAG, line)
            Event.FAILED -> Log.e(TAG, line)
            else -> Log.d(TAG, line)
        }
    }

    /** An ad slot turned off in the ads control: nothing is requested for it. */
    fun slotOff(slotPlacement: AdPlacement, nativeKey: String, bannerKey: String) {
        if (!AdsLog.verbose) return
        val screen = (slotPlacement.screen ?: "-").take(SCREEN_WIDTH).padEnd(SCREEN_WIDTH)
        val keys = "$nativeKey/$bannerKey".take(KEY_WIDTH).padEnd(KEY_WIDTH)
        Log.d(TAG, "$screen │ $keys │ ${"Slot".padEnd(FORMAT_WIDTH)} │ ${Event.SKIPPED.label}  slot is off (ads control)")
    }

    private val AdFormat.label: String
        get() = when (this) {
            AdFormat.APP_OPEN -> "App Open"
            AdFormat.BANNER -> "Banner"
            AdFormat.INTERSTITIAL -> "Interstitial"
            AdFormat.REWARDED -> "Rewarded"
            AdFormat.REWARDED_INTERSTITIAL -> "Rewarded Int."
            AdFormat.NATIVE -> "Native"
        }

    private const val SCREEN_WIDTH = 13
    private const val KEY_WIDTH = 25
    private const val FORMAT_WIDTH = 13
}
