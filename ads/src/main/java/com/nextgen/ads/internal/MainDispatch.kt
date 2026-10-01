package com.nextgen.ads.internal

import android.os.Handler
import android.os.Looper

/** GMA Next-Gen delivers callbacks on background threads; everything we hand to apps goes through here. */
internal object MainDispatch {
    val handler = Handler(Looper.getMainLooper())

    fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }

    fun postDelayed(delayMillis: Long, block: Runnable) = handler.postDelayed(block, delayMillis)

    fun cancel(block: Runnable) = handler.removeCallbacks(block)
}
