package com.example.admob_next_gen.di

import android.app.Application
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module



class KoinModules {

    private val utilsModules = module {
        single { SharedPreferenceUtils(androidContext().getSharedPreferences("app_preferences", Application.MODE_PRIVATE)) }
    }

    val modulesList = listOf(utilsModules)
}
