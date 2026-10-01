package com.example.admob_next_gen

import android.app.Application
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.example.admob_next_gen.di.KoinModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initSdk()
        initKoin()
    }

    private fun initSdk() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                this@MainApplication,
                InitializationConfig.Builder(getString(R.string.admob_app_id)).build()
            ) {}
        }
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@MainApplication)
            modules(KoinModules().modulesList)
        }
    }
}