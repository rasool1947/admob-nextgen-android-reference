package com.example.admob_next_gen.ads.appOpen.screen.callbacks

/**
 * Date: 1/17/2025
 *
 */

interface AppOpenOnLoadCallBack {
    fun onResponse(successfullyLoaded: Boolean, errorMessage: String? = null)
}