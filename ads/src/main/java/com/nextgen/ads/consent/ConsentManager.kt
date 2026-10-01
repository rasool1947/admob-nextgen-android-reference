package com.nextgen.ads.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.nextgen.ads.config.AdsConfig
import com.nextgen.ads.internal.AdsLog

/**
 * Thin wrapper over the User Messaging Platform (UMP) SDK, following Google's recommended flow:
 * request a consent info update on every launch, then show the form only if it's required.
 * All UMP callbacks are delivered on the main thread.
 */
internal class ConsentManager(context: Context, private val config: AdsConfig) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    private var hasResetThisProcess = false

    /** Persisted across launches: true once the user consented (or consent isn't required). */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /** UMP consent status: UNKNOWN, REQUIRED, NOT_REQUIRED or OBTAINED (for logging). */
    val consentStatusName: String
        get() = when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
            ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
            else -> "UNKNOWN"
        }

    /** True when the app must offer a way to change consent later (GDPR). */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gather(activity: Activity, onComplete: (FormError?) -> Unit) {
        if (config.isDebug && config.resetConsentOnLaunch && !hasResetThisProcess) {
            AdsLog.w("Consent: resetting stored consent (debug, resetConsentOnLaunch = true)")
            consentInformation.reset()
            hasResetThisProcess = true
        }

        val params = ConsentRequestParameters.Builder()
            .setAdMobAppId(config.appId)
            .apply { if (config.isDebug) setConsentDebugSettings(debugSettings(activity)) }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                AdsLog.d("Consent: info updated, form will show only if required")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError -> onComplete(formError) }
            },
            { requestError -> onComplete(requestError) }
        )
        // UMP exposes the stored consent only after a request has started (it reads UNKNOWN in Application.onCreate).
        AdsLog.d("Consent: stored status = $consentStatusName, canRequestAds = $canRequestAds")
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (FormError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError -> onDismissed(formError) }
    }

    private fun debugSettings(context: Context): ConsentDebugSettings =
        ConsentDebugSettings.Builder(context)
            .apply {
                if (config.debugGeographyEea) setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                config.testDeviceIds.forEach { addTestDeviceHashedId(it) }
            }
            .build()
}
