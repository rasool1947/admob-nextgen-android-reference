package com.nextgen.ads.internal

/**
 * "Tell me when this preload id has an ad (or failed), but don't wait longer than X" for the
 * preloader-based formats. Main thread only.
 */
internal class PreloadWaiters {

    private val waiters = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()

    fun await(preloadId: String, timeoutMillis: Long, isReadyNow: () -> Boolean, onResult: (isReady: Boolean) -> Unit) {
        val list = waiters.getOrPut(preloadId) { mutableListOf() }
        var isDone = false
        lateinit var timeout: Runnable
        val waiter: (Boolean) -> Unit = { ready ->
            if (!isDone) {
                isDone = true
                MainDispatch.cancel(timeout)
                onResult(ready)
            }
        }
        timeout = Runnable {
            list.remove(waiter)
            waiter(isReadyNow())
        }
        list += waiter
        MainDispatch.postDelayed(timeoutMillis, timeout)
    }

    fun resolve(preloadId: String, isReady: Boolean) {
        waiters.remove(preloadId)?.forEach { it(isReady) }
    }
}
