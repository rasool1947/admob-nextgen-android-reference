package com.nextgen.ads.control

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads [AdsControl] from JSON. Every field is optional: a missing or invalid value keeps its
 * default (and is reported in [Result.warnings]), so a typo in Remote Config never breaks the app.
 *
 * ```json
 * {
 *   "ads_enabled": true,
 *   "app_open_resume": true,
 *   "splash": {
 *     "fullscreen": "app_open",            // app_open | inter | off
 *     "timeout_sec": 25,
 *     "bottom_first_ms": 2000,
 *     "bottom": { "type": "native", "style": "medium" }
 *   },
 *   "language": { "bottom": { "type": "banner", "style": "adaptive" } },
 *   "onboarding": {
 *     "mode": "per_page",                  // per_page | shared
 *     "pages": [ { "type": "native", "style": "large" }, { "type": "off" } ],
 *     "shared": { "type": "native", "style": "small" },
 *     "get_started_inter": true
 *   },
 *   "main": {
 *     "bottom": { "type": "banner", "style": "collapsible_bottom" },
 *     "tabs": { "home": { "type": "native", "style": "medium" } },
 *     "inter": { "enabled": true, "every_nth": 3, "show_on_first_click": false, "min_interval_sec": 30 }
 *   }
 * }
 * ```
 *
 * Slot `type`: native | banner | off. Native `style`: small | medium | large.
 * Banner `style`: standard | large | medium_rectangle | adaptive | inline_adaptive |
 * collapsible_top | collapsible_bottom. A slot can also be written as just `"off"`.
 */
object AdsControlParser {

    class Result(val control: AdsControl, val warnings: List<String>)

    /** @throws JSONException if [json] is not a JSON object at all. */
    fun parse(json: String): Result {
        val root = JSONObject(json)
        val warnings = mutableListOf<String>()
        val defaults = AdsControl()

        val control = AdsControl(
            adsEnabled = root.bool("ads_enabled", defaults.adsEnabled, "", warnings),
            appOpenOnResume = root.bool("app_open_resume", defaults.appOpenOnResume, "", warnings),
            splash = root.obj("splash", "", warnings)?.let { splash(it, defaults.splash, warnings) } ?: defaults.splash,
            language = root.obj("language", "", warnings)?.let { language(it, defaults.language, warnings) } ?: defaults.language,
            onboarding = root.obj("onboarding", "", warnings)?.let { onboarding(it, defaults.onboarding, warnings) } ?: defaults.onboarding,
            main = root.obj("main", "", warnings)?.let { main(it, defaults.main, warnings) } ?: defaults.main,
        )
        return Result(control, warnings)
    }

    private fun splash(json: JSONObject, default: SplashControl, warnings: MutableList<String>) = SplashControl(
        fullScreen = json.enum("fullscreen", default.fullScreen, SplashFullScreen.entries, { it.jsonName }, "splash", warnings),
        bottom = json.slot("bottom", default.bottom, "splash", warnings),
        timeoutMillis = json.nonNegativeLong("timeout_sec", default.timeoutMillis / 1000, "splash", warnings) * 1000,
        bottomFirstMillis = json.nonNegativeLong("bottom_first_ms", default.bottomFirstMillis, "splash", warnings),
    )

    private fun language(json: JSONObject, default: LanguageControl, warnings: MutableList<String>) = LanguageControl(
        bottom = json.slot("bottom", default.bottom, "language", warnings),
    )

    private fun onboarding(json: JSONObject, default: OnboardingControl, warnings: MutableList<String>): OnboardingControl {
        val pages = when (val array = json.opt("pages")) {
            null -> default.pages
            is JSONArray -> List(array.length()) { i -> slot(array.opt(i), AdSlot.Off, "onboarding.pages[$i]", warnings) }
            else -> default.pages.also { warnings += "onboarding.pages: expected an array" }
        }
        return OnboardingControl(
            mode = json.enum("mode", default.mode, OnboardingAdMode.entries, { it.jsonName }, "onboarding", warnings),
            pages = pages,
            shared = json.slot("shared", default.shared, "onboarding", warnings),
            getStartedInter = json.bool("get_started_inter", default.getStartedInter, "onboarding", warnings),
        )
    }

    private fun main(json: JSONObject, default: MainControl, warnings: MutableList<String>): MainControl {
        val tabs = json.obj("tabs", "main", warnings)?.let { tabsJson ->
            tabsJson.keys().asSequence().associateWith { key -> slot(tabsJson.opt(key), AdSlot.Off, "main.tabs.$key", warnings) }
        } ?: default.tabs
        return MainControl(
            bottom = json.slot("bottom", default.bottom, "main", warnings),
            tabs = tabs,
            inter = json.obj("inter", "main", warnings)?.let { inter(it, default.inter, warnings) } ?: default.inter,
        )
    }

    private fun inter(json: JSONObject, default: MainInterControl, warnings: MutableList<String>) = MainInterControl(
        enabled = json.bool("enabled", default.enabled, "main.inter", warnings),
        everyNth = json.nonNegativeLong("every_nth", default.everyNth.toLong(), "main.inter", warnings).toInt().coerceAtLeast(1),
        showOnFirstClick = json.bool("show_on_first_click", default.showOnFirstClick, "main.inter", warnings),
        minIntervalMillis = json.nonNegativeLong("min_interval_sec", default.minIntervalMillis / 1000, "main.inter", warnings) * 1000,
    )

    /* ---------- slots ---------- */

    private fun JSONObject.slot(name: String, default: AdSlot, path: String, warnings: MutableList<String>): AdSlot =
        if (has(name)) slot(opt(name), default, "${prefix(path)}$name", warnings) else default

    private fun slot(value: Any?, default: AdSlot, path: String, warnings: MutableList<String>): AdSlot {
        if (value is String && value.equals("off", ignoreCase = true)) return AdSlot.Off
        if (value !is JSONObject) {
            warnings += "$path: expected an object like {\"type\":\"native\",\"style\":\"medium\"}"
            return default
        }
        val style = value.optString("style").lowercase()
        return when (val type = value.optString("type").lowercase()) {
            "off" -> AdSlot.Off
            "native" -> AdSlot.Native(
                NativeStyle.entries.firstOrNull { it.jsonName == style }
                    ?: NativeStyle.MEDIUM.also { if (style.isNotEmpty()) warnings += "$path.style: unknown native style '$style'" }
            )
            "banner" -> AdSlot.Banner(
                BannerStyle.entries.firstOrNull { it.jsonName == style }
                    ?: BannerStyle.ADAPTIVE.also { if (style.isNotEmpty()) warnings += "$path.style: unknown banner style '$style'" }
            )
            else -> default.also { warnings += "$path.type: unknown type '$type'" }
        }
    }

    /* ---------- primitives ---------- */

    private fun JSONObject.obj(name: String, path: String, warnings: MutableList<String>): JSONObject? {
        if (!has(name)) return null
        return optJSONObject(name) ?: null.also { warnings += "${prefix(path)}$name: expected an object" }
    }

    private fun JSONObject.bool(name: String, default: Boolean, path: String, warnings: MutableList<String>): Boolean {
        if (!has(name)) return default
        return when (val value = opt(name)) {
            is Boolean -> value
            else -> default.also { warnings += "${prefix(path)}$name: expected true/false, got '$value'" }
        }
    }

    private fun JSONObject.nonNegativeLong(name: String, default: Long, path: String, warnings: MutableList<String>): Long {
        if (!has(name)) return default
        val value = opt(name)
        return (value as? Number)?.toLong()?.takeIf { it >= 0 }
            ?: default.also { warnings += "${prefix(path)}$name: expected a number >= 0, got '$value'" }
    }

    private fun <T> JSONObject.enum(
        name: String,
        default: T,
        entries: List<T>,
        jsonName: (T) -> String,
        path: String,
        warnings: MutableList<String>,
    ): T {
        if (!has(name)) return default
        val value = optString(name).lowercase()
        return entries.firstOrNull { jsonName(it) == value }
            ?: default.also { warnings += "${prefix(path)}$name: unknown value '$value', expected ${entries.map(jsonName)}" }
    }

    private fun prefix(path: String) = if (path.isEmpty()) "" else "$path."
}
