package com.example.admob_next_gen.di

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.example.admob_next_gen.ads.appOpen.application.AppOpenAdManager
import com.example.admob_next_gen.ads.appOpen.screen.AppOpenAdsConfig
import com.example.admob_next_gen.ads.banner.data.dataSources.local.DataSourceLocalBanner
import com.example.admob_next_gen.ads.banner.data.dataSources.remote.DataSourceRemoteBanner
import com.example.admob_next_gen.ads.banner.data.repositories.RepositoryBannerImpl
import com.example.admob_next_gen.ads.banner.domain.useCases.UseCaseBanner
import com.example.admob_next_gen.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.admob_next_gen.ads.interstitial.InterstitialAdsConfig
import com.example.admob_next_gen.ads.natives.data.dataSources.local.DataSourceLocalNative
import com.example.admob_next_gen.ads.natives.data.dataSources.remote.DataSourceRemoteNative
import com.example.admob_next_gen.ads.natives.data.repositories.RepositoryNativeImpl
import com.example.admob_next_gen.ads.natives.domain.useCases.UseCaseNative
import com.example.admob_next_gen.ads.natives.presentation.viewModels.ViewModelNative
import com.example.admob_next_gen.utilities.manager.InternetManager
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module



class KoinModules {

    private val managerModules = module {
        single { InternetManager(androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) }
    }

    private val utilsModules = module {
        single { SharedPreferenceUtils(androidContext().getSharedPreferences("app_preferences", Application.MODE_PRIVATE)) }
    }

    private val firebaseModule = module {
//        single { RemoteConfiguration(get(), get()) }
    }

    /* -------------------------------------- Ads -------------------------------------- */

    private val appOpenAdModule = module {
        single { AppOpenAdManager(get(), get(), get()) }
        single { AppOpenAdsConfig(get(), get(), get()) }
    }

    private val bannerAdModule = module {
        single { DataSourceLocalBanner() }
        single { DataSourceRemoteBanner(context = get()) }
        single { RepositoryBannerImpl(get(), get()) }
        single { UseCaseBanner(get(), get(), get(), get()) }
        viewModel { ViewModelBanner(get()) }
    }

    private val interAdModule = module {
        single { InterstitialAdsConfig(get(), get(), get()) }
        single { com.example.admob_next_gen.ads.rewarded.RewardedAdsConfig(androidContext(), get(), get()) }
        single { com.example.admob_next_gen.ads.rewarded.RewardedInterAdsConfig(androidContext(), get(), get()) }
    }

    private val nativeAdModule = module {
        single { DataSourceLocalNative() }
        single { DataSourceRemoteNative() }
        single { RepositoryNativeImpl(get(), get()) }
        single { UseCaseNative(get(), get(), get(), get()) }
        viewModel { ViewModelNative(get()) }
    }

    val modulesList = listOf(utilsModules, managerModules, firebaseModule, appOpenAdModule, bannerAdModule, interAdModule, nativeAdModule)
}