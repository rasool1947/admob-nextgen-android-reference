package com.example.admob_next_gen.app.splash

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgen.ads.control.AdsControlStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash timing, kept here so a rotation doesn't restart it:
 * consent -> load ads -> wait for the full-screen ad (at most the control's timeout) -> show it -> navigate.
 *
 * The ad timeout starts once consent is known: time the user spends on the consent form doesn't count.
 */
class ViewModelSplash : ViewModel() {

    private val createdAt = SystemClock.elapsedRealtime()
    private var adsStartedAt = 0L

    /** Longest the splash waits for its ads (ads control `splash.timeout_sec`, default 25 s). */
    val timeoutMillis: Long = AdsControlStore.current.splash.timeoutMillis

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

    /** Waiting is over. Value: true if a full-screen ad is ready to show. */
    private val _doneLiveData = MutableLiveData<Boolean>()
    val doneLiveData: LiveData<Boolean> get() = _doneLiveData

    private val _navigateLiveData = MutableLiveData<Unit>()
    val navigateLiveData: LiveData<Unit> get() = _navigateLiveData

    private var timeoutJob: Job? = null
    private var isFinishing = false

    fun onConsentResult(canLoadAds: Boolean) {
        if (!canLoadAds) return finish(showAd = false)
        if (isLoadingAds) return

        adsStartedAt = SystemClock.elapsedRealtime()
        _loadAdsLiveData.value = Unit
        timeoutJob = viewModelScope.launch {
            delay(timeoutMillis)
            finish(showAd = false)
        }
    }

    fun onFullScreenAdResult(isReady: Boolean) = finish(showAd = isReady)

    /** The full-screen ad was closed, failed, or there was none. */
    fun onAdFlowFinished() {
        _navigateLiveData.value = Unit
    }

    private fun finish(showAd: Boolean) {
        if (isFinishing) return
        isFinishing = true
        timeoutJob?.cancel()
        viewModelScope.launch {
            // Keep the branding on screen for a moment even when there is nothing to wait for.
            delay((MIN_SPLASH_MILLIS - (SystemClock.elapsedRealtime() - createdAt)).coerceAtLeast(0))
            _doneLiveData.value = showAd
        }
    }

    private companion object {
        const val MIN_SPLASH_MILLIS = 1_500L
    }
}
