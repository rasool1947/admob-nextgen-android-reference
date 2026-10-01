package com.nextgen.ads.internal

import android.util.Log

internal object AdsLog {
    private const val TAG = "NextGenAds"

    var verbose = false

    fun d(message: String) {
        if (verbose) Log.d(TAG, message)
    }

    fun i(message: String) = Log.i(TAG, message)
    fun w(message: String) = Log.w(TAG, message)
    fun e(message: String, throwable: Throwable? = null) = Log.e(TAG, message, throwable)
}
