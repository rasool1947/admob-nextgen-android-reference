package com.example.admob_next_gen.app.entrance

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Date: 2/10/2025
 *
 */

class ViewModelEntrance : ViewModel() {

    /* ----------------------------------- Remote Config ----------------------------------- */

    private val _remoteConfigResponseLiveData = MutableLiveData<Unit>()
    val remoteConfigResponseLiveData: LiveData<Unit> get() = _remoteConfigResponseLiveData

    fun onRemoteConfigResponse() {
        _remoteConfigResponseLiveData.value = Unit
    }

    /* ----------------------------------- Consent & Ads ----------------------------------- */

    private val _loadAdsLiveData = MutableLiveData<Unit>()
    val loadAdsLiveData: LiveData<Unit> get() = _loadAdsLiveData

    private val _navigateLiveData = MutableLiveData<Unit>()
    val navigateLiveData: LiveData<Unit> get() = _navigateLiveData

    /** Survives rotation, so consent is gathered once per launch. */
    var isConsentRequested = false

    private var jobAds: Job? = null
    private val adsTimeout = 8000L

    fun onConsentResult(canLoadAds: Boolean) {
        when (canLoadAds) {
            true -> startAdTimer()
            false -> _navigateLiveData.value = Unit
        }
    }

    private fun startAdTimer() {
        if (jobAds != null) return
        Log.i("AdsInformation", "Ads -> startAdTimer: Started 8 seconds for Ads")

        _loadAdsLiveData.value = Unit
        jobAds = viewModelScope.launch {
            delay(adsTimeout)
            _navigateLiveData.value = Unit
        }
    }

    private fun cancelAdsJob() {
        if (jobAds?.isActive == true) {
            Log.e("AdsInformation", "Ads -> cancelAdsJob: Cancelled 8 seconds for Ads")
            jobAds?.cancel()
        }
    }

    /* ----------------------------------- Ads Responses ----------------------------------- */

    private val totalAds = 2
    private val loadedAdsCounter = AtomicInteger(0)

    fun onAdResponse() {
        if (loadedAdsCounter.incrementAndGet() >= totalAds) {
            cancelAdsJob()
            _navigateLiveData.postValue(Unit)
        }
    }
}
