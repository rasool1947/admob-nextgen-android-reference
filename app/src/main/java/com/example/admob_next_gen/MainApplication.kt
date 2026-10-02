package com.example.admob_next_gen

import android.app.Application
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.ads.LocalAdsControl
import com.example.admob_next_gen.di.KoinModules
import com.example.admob_next_gen.utilities.firebase.FirebaseAdRevenue
import com.example.admob_next_gen.utilities.firebase.RemoteAdsControl
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.config.AdsConfig
import com.nextgen.ads.control.AdsControlStore
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin()
        initAds()
        AdsControlStore.update(LocalAdsControl.JSON, source = "local")
        RemoteAdsControl.fetch(this, BuildConfig.DEBUG)
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@MainApplication)
            modules(KoinModules().modulesList)
        }
    }

    private fun initAds() {
        val prefs = get<SharedPreferenceUtils>()
        AdsSdk.configure(
            application = this,
            config = AdsConfig(
                appId = getString(R.string.admob_app_id),
                placements = AppAdPlacements.create(this, prefs),
                isPremium = { prefs.isAppPurchased },
                testDeviceIds = TEST_DEVICE_IDS,
                isDebug = BuildConfig.DEBUG,
                debugGeographyEea = true,
                onAdPaid = { revenue -> FirebaseAdRevenue.log(this, revenue) },
            )
        )
    }

    private companion object {
        /**
         * Hashed ids of your own test phones: they always get test ads and, in debug builds,
         * behave as if in the EEA so the consent form can be tested. Logcat prints a device's id
         * on first launch (tag "UserMessagingPlatform": "...addTestDeviceHashedId("<id>")").
         */
        val TEST_DEVICE_IDS = listOf(
            "BB0B9CB8CA2E98224237F3AF175435F8",
        )
    }
}
