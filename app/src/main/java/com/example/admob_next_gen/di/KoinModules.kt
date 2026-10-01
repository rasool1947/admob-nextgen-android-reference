package com.example.admob_next_gen.di

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
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

    private val nativeAdModule = module {
        single { DataSourceLocalNative() }
        single { DataSourceRemoteNative() }
        single { RepositoryNativeImpl(get(), get()) }
        single { UseCaseNative(get(), get(), get(), get()) }
        viewModel { ViewModelNative(get()) }
    }

    val modulesList = listOf(utilsModules, managerModules, firebaseModule, nativeAdModule)
}