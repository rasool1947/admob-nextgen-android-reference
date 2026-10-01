package com.example.admob_next_gen.ads.banner.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.example.admob_next_gen.ads.banner.domain.useCases.UseCaseBanner
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdKey
import kotlinx.coroutines.launch

/**
 * Date: 1/17/2025
 *
 */

class ViewModelBanner(private val useCaseBanner: UseCaseBanner) : ViewModel() {

    private val _adViewLiveData = MutableLiveData<AdView>()
    val adViewLiveData: LiveData<AdView> get() = _adViewLiveData

    private val _loadFailedLiveData = MutableLiveData<Unit>()
    val loadFailedLiveData: LiveData<Unit> get() = _loadFailedLiveData

    private val _clearViewLiveData = MutableLiveData<Unit>()
    val clearViewLiveData: LiveData<Unit> get() = _clearViewLiveData

    fun loadBannerAd(adView: AdView, bannerAdKey: BannerAdKey, instanceTag: String? = null) = viewModelScope.launch {
        useCaseBanner.loadBannerAd(adView, bannerAdKey, instanceTag) { itemBannerAd ->
            itemBannerAd?.let {
                _adViewLiveData.postValue(it.adView)
            } ?: run {
                _loadFailedLiveData.postValue(Unit)
            }
        }
    }

    fun destroyBanner(bannerAdKey: BannerAdKey, instanceTag: String? = null) = viewModelScope.launch {
        if (useCaseBanner.destroyBanner(bannerAdKey, instanceTag)) {
            _clearViewLiveData.postValue(Unit)
        }
    }
}