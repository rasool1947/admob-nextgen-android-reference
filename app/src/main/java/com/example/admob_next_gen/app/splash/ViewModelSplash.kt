package com.example.admob_next_gen.app.splash

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admob_next_gen.ads.AppAdPlacements
import com.nextgen.ads.control.AdSlot
import com.nextgen.ads.control.AdsControlStore
import com.nextgen.ads.control.SplashFullScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash ad flow, kept here so a rotation doesn't restart it (ads control `splash`):
 * 1. consent (and Remote Config, briefly), then the full-screen ad (App Open or interstitial) and
 *    the bottom ad load together;
 * 2. the splash waits for both answers, at most `timeout_sec` (25 s) counted from consent;
 * 3. a loaded bottom ad stays on screen at least `bottom_first_ms` (2 s) before anything else;
 * 4. the full-screen ad shows (if it is ready), then the app moves on.
 */
class ViewModelSplash : ViewModel() {

    /** Read when the ads start (after consent and Remote Config), then fixed for this launch. */
    private var control = AdsControlStore.current.splash
    private val createdAt = SystemClock.elapsedRealtime()
    private var adsStartedAt = 0L

    /** Full-screen placement of this launch, or null when the control turned it off. */
    val fullScreenKey: String? get() = when (control.fullScreen) {
        SplashFullScreen.APP_OPEN -> AppAdPlacements.APP_OPEN_SPLASH
        SplashFullScreen.INTERSTITIAL -> AppAdPlacements.INTER_SPLASH
        SplashFullScreen.OFF -> null
    }

    /** Bottom native/banner/off of this launch. */
    val bottomSlot: AdSlot get() = control.bottom

    /** Longest the splash waits for its ads (ads control `splash.timeout_sec`, default 25 s). */
    val timeoutMillis: Long get() = control.timeoutMillis

    /** True once consent is known and ads are loading; the progress bar fills from then on. */
    val isLoadingAds: Boolean get() = adsStartedAt != 0L
    val elapsedMillis: Long get() = if (isLoadingAds) SystemClock.elapsedRealtime() - adsStartedAt else 0L
    val remainingMillis: Long get() = (timeoutMillis - elapsedMillis).coerceAtLeast(0)

    /** Survives rotation, so consent is gathered once per launch. */
    var isConsentRequested = false

    /** Set once the full-screen ad was asked to show, so a rotation doesn't show it twice. */
    var isAdShowStarted = false

    private val _loadAdsLiveData = MutableLiveData<Unit>()
    val loadAdsLiveData: LiveData<Unit> get() = _loadAdsLiveData

    /** Waiting is over. Value: true if the full-screen ad is ready to show. */
    private val _doneLiveData = MutableLiveData<Boolean>()
    val doneLiveData: LiveData<Boolean> get() = _doneLiveData

    private val _navigateLiveData = MutableLiveData<Unit>()
    val navigateLiveData: LiveData<Unit> get() = _navigateLiveData

    /** null while still loading. */
    private var isFullScreenReady: Boolean? = null
    private var hasBottomAnswered = false
    private var bottomShownAt = 0L

    private var timeoutJob: Job? = null
    private var isFinishing = false

    fun onConsentResult(canLoadAds: Boolean) {
        if (!canLoadAds) return finish(showAd = false)
        if (isLoadingAds) return

        control = AdsControlStore.current.splash
        adsStartedAt = SystemClock.elapsedRealtime()
        _loadAdsLiveData.value = Unit
        timeoutJob = viewModelScope.launch {
            delay(timeoutMillis)
            finish(showAd = isFullScreenReady == true)
        }
    }

    /** The bottom slot loaded an ad (now visible) or gave up (off, no consent, no fill). First answer wins. */
    fun onBottomAdResult(isLoaded: Boolean) {
        if (hasBottomAnswered) return
        hasBottomAnswered = true
        if (isLoaded) bottomShownAt = SystemClock.elapsedRealtime()
        finishWhenBothAnswered()
    }

    fun onFullScreenAdResult(isReady: Boolean) {
        if (isFullScreenReady != null) return
        isFullScreenReady = isReady
        finishWhenBothAnswered()
    }

    /** The full-screen ad was closed, failed, or there was none. */
    fun onAdFlowFinished() {
        _navigateLiveData.value = Unit
    }

    private fun finishWhenBothAnswered() {
        val isReady = isFullScreenReady ?: return
        if (hasBottomAnswered) finish(showAd = isReady)
    }

    private fun finish(showAd: Boolean) {
        if (isFinishing) return
        isFinishing = true
        timeoutJob?.cancel()
        viewModelScope.launch {
            val now = SystemClock.elapsedRealtime()
            // Branding stays a moment even with nothing to wait for; a loaded bottom ad stays its minimum time.
            val brandingLeft = MIN_SPLASH_MILLIS - (now - createdAt)
            val bottomLeft = if (bottomShownAt != 0L) control.bottomFirstMillis - (now - bottomShownAt) else 0L
            delay(maxOf(brandingLeft, bottomLeft, 0L))
            _doneLiveData.value = showAd
        }
    }

    private companion object {
        const val MIN_SPLASH_MILLIS = 1_500L
    }
}
