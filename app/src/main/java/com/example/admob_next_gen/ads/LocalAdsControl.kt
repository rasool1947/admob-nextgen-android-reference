package com.example.admob_next_gen.ads

/**
 * Default ads control of this app, used until Remote Config answers (and whenever it can't).
 *
 * In a real app, publish this same JSON as the Firebase Remote Config parameter [REMOTE_CONFIG_KEY]
 * and pass the fetched string to AdsControlStore.update(). Format: see AdsControlParser.
 */
object LocalAdsControl {

    const val REMOTE_CONFIG_KEY = "ads_config"

    val JSON = """
        {
          "ads_enabled": true,
          "app_open_resume": true,

          "splash": {
            "fullscreen": "inter",
            "timeout_sec": 25,
            "bottom_first_ms": 2000,
            "bottom": { "type": "banner", "style": "adaptive" }
          },

          "language": {
            "bottom": { "type": "native", "style": "medium" }
          },

          "onboarding": {
            "mode": "per_page",
            "pages": [
              { "type": "banner", "style": "inline_adaptive" },
              { "type": "banner", "style": "inline_adaptive" },
              { "type": "banner", "style": "inline_adaptive" },
              { "type": "banner", "style": "inline_adaptive" }
            ],
            "shared": { "type": "banner", "style": "inline_adaptive" },
            "get_started_inter": true
          },

          "main": {
            "bottom": { "type": "banner", "style": "adaptive" },
            "tabs": {
              "home": { "type": "native", "style": "medium" },
              "explore": { "type": "banner", "style": "inline_adaptive" },
              "history": { "type": "native", "style": "small" },
              "settings": { "type": "banner", "style": "large" }
            },
            "inter": {
              "enabled": true,
              "every_nth": 3,
              "show_on_first_click": false,
              "min_interval_sec": 30
            }
          },

          "cache": {
            "reuse_shown_sec": 30,
            "max_age_min": 50
          }
        }
    """.trimIndent()
}
