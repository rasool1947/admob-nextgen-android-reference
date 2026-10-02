package com.nextgen.ads.banner

/** How a banner is sized. Adaptive sizes take their width from the container the banner is placed in. */
sealed interface BannerSize {

    /** Anchored adaptive banner (top/bottom of the screen); the usual choice. ~50-90dp tall. */
    data object Anchored : BannerSize

    /** Larger anchored adaptive banner. Better fill and revenue, but taller. */
    data object LargeAnchored : BannerSize

    /**
     * Inline adaptive banner for scrolling content (lists, articles).
     * @param maxHeightDp Cap the height; null lets Google pick the best height for the width.
     */
    data class Inline(val maxHeightDp: Int? = null) : BannerSize

    /**
     * Anchored banner that can open expanded and collapse to normal size.
     * @param fromTop true if the banner is anchored at the top of the screen.
     */
    data class Collapsible(val fromTop: Boolean = false) : BannerSize

    /** Fixed 320x50, centered. */
    data object Standard : BannerSize

    /** Fixed 320x100, centered. */
    data object Large : BannerSize

    /** Fixed 300x250 ("MREC"), centered. Good inside content, like a native ad. */
    data object MediumRectangle : BannerSize
}
