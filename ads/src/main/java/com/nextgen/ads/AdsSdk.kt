package com.nextgen.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.nextgen.ads.config.AdPlacement
import com.nextgen.ads.config.AdsConfig
import com.nextgen.ads.consent.ConsentManager
import com.nextgen.ads.internal.AdsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Entry point of the ads module.
 *
 * Startup flow:
 * 1. `Application.onCreate()` → [configure].
 * 2. First screen → [gatherConsent]. Shows the consent form only when required, initializes the
 *    SDK once consent allows it, and reports whether ads can be loaded.
 * 3. Before every ad request, formats check [canLoadAds].
 */
object AdsSdk {

    private lateinit var application: Application
    private lateinit var consentManager: ConsentManager

    lateinit var config: AdsConfig
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Accessed on the main thread only.
    private var initStarted = false
    private val onInitializedCallbacks = mutableListOf<() -> Unit>()

    private val _isInitialized = MutableStateFlow(false)

    /** True once the Mobile Ads SDK is initialized and ads may be requested (if consent allows). */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    val isConfigured: Boolean get() = ::config.isInitialized

    /** Consent allows requesting ads. Stored by UMP, so it's valid from the previous session too. */
    val canRequestAds: Boolean get() = isConfigured && consentManager.canRequestAds

    /** Consent allows ads, the SDK is initialized and the user isn't premium. Check before every load. */
    val canLoadAds: Boolean get() = canRequestAds && _isInitialized.value && !config.isPremium()

    /** Show a "Privacy settings" entry in the app (e.g. Settings) when this is true. Required for GDPR. */
    val isPrivacyOptionsRequired: Boolean get() = isConfigured && consentManager.isPrivacyOptionsRequired

    @MainThread
    fun configure(application: Application, config: AdsConfig) {
        check(!isConfigured) { "AdsSdk.configure() must be called only once" }
        this.application = application
        this.config = config
        AdsLog.verbose = config.isDebug
        consentManager = ConsentManager(application, config)
    }

    fun placement(key: String): AdPlacement = config.placement(key)

    /**
     * Requests the latest consent info and shows the consent form if required. Call it from the
     * first screen on every launch.
     *
     * Returning users who already consented don't wait for the consent network request: the SDK
     * starts right away and [onComplete] fires as soon as it's ready, while UMP finishes in the
     * background (and still shows the form on top if consent has to be renewed).
     *
     * @param onComplete Main thread, called once. `true` when ads can be loaded (consent allows it
     *                   and the SDK is initialized), `false` when the app should continue without ads.
     */
    @MainThread
    fun gatherConsent(activity: Activity, onComplete: (canLoadAds: Boolean) -> Unit) {
        checkConfigured()
        var isDelivered = false
        val deliver = { canLoad: Boolean ->
            if (!isDelivered) {
                isDelivered = true
                onComplete(canLoad)
            }
        }

        consentManager.gather(activity) { error ->
            error?.let { AdsLog.w("Consent: error ${it.errorCode}: ${it.message}") }
            when (consentManager.canRequestAds) {
                true -> initializeSdk { deliver(canLoadAds) }
                false -> {
                    AdsLog.i("Consent: ads can't be requested")
                    deliver(false)
                }
            }
        }

        // Consent stored in a previous session is readable once the request has started.
        if (consentManager.canRequestAds) initializeSdk { deliver(canLoadAds) }
    }

    /** Re-opens the consent form so the user can change their choice. */
    @MainThread
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        checkConfigured()
        consentManager.showPrivacyOptionsForm(activity) { error ->
            error?.let { AdsLog.w("Privacy options: error ${it.errorCode}: ${it.message}") }
            // Consent may have just been granted.
            if (consentManager.canRequestAds) initializeSdk()
            onDismissed()
        }
    }

    /** Opens the Ad Inspector (debug tool). Works only on test devices and after initialization. */
    fun openAdInspector() {
        if (!_isInitialized.value) {
            AdsLog.w("Ad Inspector: SDK not initialized yet")
            return
        }
        MobileAds.openAdInspector { error -> error?.let { AdsLog.w("Ad Inspector: ${it.message}") } }
    }

    @MainThread
    private fun initializeSdk(onInitialized: (() -> Unit)? = null) {
        if (_isInitialized.value) {
            onInitialized?.invoke()
            return
        }
        onInitialized?.let { onInitializedCallbacks += it }
        if (initStarted) return
        initStarted = true

        val initConfig = InitializationConfig.Builder(config.appId)
            .setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(config.testDeviceIds)
                    .build()
            )
            .build()

        AdsLog.d("SDK: initializing")
        // initialize() blocks while the SDK starts, so it must run off the main thread.
        // Once it returns, ads can be requested; adapter (mediation) init continues in the background.
        scope.launch {
            val success = runCatching {
                MobileAds.initialize(application, initConfig) { AdsLog.d("SDK: adapters initialized") }
            }.onFailure { AdsLog.e("SDK: initialization failed", it) }.isSuccess

            mainHandler.post {
                if (success) {
                    AdsLog.i("SDK: initialized")
                    _isInitialized.value = true
                } else {
                    initStarted = false // allow a retry on the next gatherConsent()
                }
                val callbacks = onInitializedCallbacks.toList()
                onInitializedCallbacks.clear()
                callbacks.forEach { it() }
            }
        }
    }

    private fun checkConfigured() = check(isConfigured) { "Call AdsSdk.configure() in Application.onCreate() first" }
}
