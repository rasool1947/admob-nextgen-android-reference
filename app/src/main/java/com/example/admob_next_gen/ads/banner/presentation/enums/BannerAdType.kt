package com.example.admob_next_gen.ads.banner.presentation.enums

/**
 * Date: 1/17/2025
 *
 */

enum class BannerAdType {
    ADAPTIVE,
    COLLAPSIBLE_TOP,
    COLLAPSIBLE_BOTTOM,
    INLINE_FULL,
    INLINE_SIMPLE,
    INLINE_120;

    companion object {
        /**
         * Map Remote Config Int value to BannerAdType:
         *   0 = disabled → INLINE_FULL (fallback, won't reach here — checkRemoteConfig blocks 0)
         *   1 = INLINE_FULL
         *   2 = INLINE_120
         *   3 = INLINE_SIMPLE
         *   4 = ADAPTIVE
         *   5 = COLLAPSIBLE_BOTTOM
         *   6 = COLLAPSIBLE_TOP
         * Any unknown value → INLINE_FULL
         */
        fun fromInt(value: Int): BannerAdType = when (value) {

            1    -> INLINE_FULL
            2    -> INLINE_120
            3    -> INLINE_SIMPLE
            4    -> ADAPTIVE
            5    -> COLLAPSIBLE_BOTTOM
            6    -> COLLAPSIBLE_TOP
            else -> INLINE_FULL
        }
    }
}