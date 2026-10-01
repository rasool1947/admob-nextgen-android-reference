package com.example.admob_next_gen.di

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import com.example.admob_next_gen.utilities.manager.InternetManager
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import org.koin.android.ext.koin.androidContext
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

    val modulesList = listOf(utilsModules, managerModules, firebaseModule)
}