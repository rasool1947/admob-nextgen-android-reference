package com.example.admob_next_gen.di

import com.example.admob_next_gen.ads.appOpen.application.AppOpenAdManager
import com.example.admob_next_gen.ads.appOpen.screen.AppOpenAdsConfig
import com.example.admob_next_gen.ads.interstitial.InterstitialAdsConfig
//import com.example.admob_next_gen.utilities.firebase.RemoteConfiguration
import com.example.admob_next_gen.utilities.manager.InternetManager
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject



class DIComponent : KoinComponent {

    // Utils
    val sharedPreferenceUtils by inject<SharedPreferenceUtils>()

    // Managers
    val internetManager by inject<InternetManager>()

    // Remote Configuration
//    val remoteConfiguration by inject<RemoteConfiguration>()
   // Admob
    val appOpenAdManager by inject<AppOpenAdManager>()
    val appOpenAdsConfig by inject<AppOpenAdsConfig>()

    val interstitialAdsConfig by inject<InterstitialAdsConfig>()
}