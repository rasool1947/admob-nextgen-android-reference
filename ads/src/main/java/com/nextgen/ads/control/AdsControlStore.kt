package com.nextgen.ads.control

import com.nextgen.ads.internal.AdsLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException

/**
 * Holds the [AdsControl] the app is running with.
 *
 * Call [update] with the local default JSON in `Application.onCreate()`, then again with the
 * Remote Config JSON once it is fetched. Screens read [current] when they decide which ad to load.
 */
object AdsControlStore {

    private val _control = MutableStateFlow(AdsControl())

    /** Current switches, with the master switch applied: everything is off when `ads_enabled` is false. */
    val current: AdsControl get() = _control.value

    /** Same as [current], for code that wants to react to a Remote Config update. */
    val control: StateFlow<AdsControl> = _control.asStateFlow()

    /**
     * Parses [json] and makes it current. Invalid fields fall back to defaults and are logged;
     * if [json] can't be parsed at all, the previous control stays and false is returned.
     *
     * @param source Where the JSON came from, for the log (e.g. "local", "remote config").
     */
    fun update(json: String, source: String): Boolean {
        val result = try {
            AdsControlParser.parse(json)
        } catch (e: JSONException) {
            AdsLog.e("Ads control from $source is not valid JSON, keeping the previous one", e)
            return false
        }
        result.warnings.forEach { AdsLog.w("Ads control ($source): $it") }
        set(result.control)
        AdsLog.d("Ads control from $source: ${_control.value}")
        return true
    }

    fun set(control: AdsControl) {
        _control.value = if (control.adsEnabled) control else control.allOff()
    }
}
