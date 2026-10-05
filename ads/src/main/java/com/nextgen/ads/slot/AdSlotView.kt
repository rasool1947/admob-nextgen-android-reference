package com.nextgen.ads.slot

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.nextgen.ads.AdsSdk
import com.nextgen.ads.R
import com.nextgen.ads.banner.BannerAdListener
import com.nextgen.ads.banner.BannerAds
import com.nextgen.ads.banner.BannerSize
import com.nextgen.ads.config.forScreen
import com.nextgen.ads.control.AdSlot
import com.nextgen.ads.control.BannerStyle
import com.nextgen.ads.control.NativeStyle
import com.nextgen.ads.internal.AdsFlowLog
import com.nextgen.ads.internal.ShimmerLayout
import com.nextgen.ads.internal.ShownAds
import com.nextgen.ads.nativead.NativeAdListener
import com.nextgen.ads.nativead.NativeAdTemplateView
import com.nextgen.ads.nativead.NativeAds

/**
 * One ad position whose content comes from the ads control: a native ad (small / medium / large),
 * a banner (any [BannerStyle]) or nothing. Put it in a layout once:
 * ```
 * <com.nextgen.ads.slot.AdSlotView
 *     android:id="@+id/adSlot"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 * and load it with the slot from [com.nextgen.ads.control.AdsControlStore]:
 * ```
 * binding.adSlot.load(viewLifecycleOwner, control.language.bottom, "native_language", "banner_language")
 * ```
 * A shimmer placeholder of the ad's size shows while loading; the view hides itself when the
 * slot is off or no ad can be shown. Calling [load] again replaces the ad (the old one is destroyed).
 */
class AdSlotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** The ad shown right now lives in here; destroyed on the next [load] or with the screen. */
    private var current: SlotLifecycle? = null

    init {
        if (isInEditMode) visibility = VISIBLE
    }

    /**
     * @param lifecycleOwner      Screen lifecycle (a Fragment's `viewLifecycleOwner`); ads die with it.
     * @param slot                What to show, from the ads control.
     * @param nativePlacementKey  Placement used when [slot] is native.
     * @param bannerPlacementKey  Placement used when [slot] is a banner.
     * @param screen              Log label (tag `AdsFlow`) when not the placement's own, e.g. "OB2".
     */
    @MainThread
    fun load(
        lifecycleOwner: LifecycleOwner,
        slot: AdSlot,
        nativePlacementKey: String,
        bannerPlacementKey: String,
        listener: AdSlotListener? = null,
        screen: String? = null,
    ) {
        clear()
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return

        when (slot) {
            AdSlot.Off -> {
                visibility = GONE
                AdsFlowLog.slotOff(AdsSdk.placement(nativePlacementKey).forScreen(screen), nativePlacementKey, bannerPlacementKey)
                listener?.onAdFailedToLoad("slot is off")
            }
            is AdSlot.Native -> loadNative(SlotLifecycle(lifecycleOwner), slot.style, nativePlacementKey, listener, screen)
            is AdSlot.Banner -> loadBanner(SlotLifecycle(lifecycleOwner), slot.style, bannerPlacementKey, listener, screen)
        }
    }

    /**
     * Call when the slot's screen becomes visible again without being recreated, e.g. a tab shown with
     * FragmentTransaction.show() (a recreated screen goes through [load] and gets this for free). If its
     * ad was seen longer than `cache.reuse_shown_sec` ago, a new one is loaded and swapped in; the old
     * ad stays on screen until then. Otherwise nothing happens (no request).
     */
    @MainThread
    fun onShownAgain() {
        current?.let { ShownAds.refreshIfSeenLongAgo(it) }
    }

    /** Removes the current ad (destroying it) and hides the slot. */
    @MainThread
    fun clear() {
        current?.destroy()
        current = null
        removeAllViews()
        visibility = GONE
    }

    private fun loadNative(
        owner: SlotLifecycle,
        style: NativeStyle,
        placementKey: String,
        listener: AdSlotListener?,
        screen: String?,
    ) {
        current = owner
        visibility = VISIBLE
        val template = NativeAdTemplateView(context, style.toTemplate())
        addView(template, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        NativeAds.loadInto(template, owner, placementKey, screen = screen, listener = object : NativeAdListener {
            private var isLoaded = false

            override fun onAdLoaded(ad: NativeAd) {
                // Called again when a refreshed ad replaces a kept one; the slot was already loaded.
                if (!isLoaded) listener?.onAdLoaded()
                isLoaded = true
            }

            override fun onAdFailedToLoad(reason: String) {
                if (current === owner) visibility = GONE
                listener?.onAdFailedToLoad(reason)
            }

            override fun onAdClicked() {
                listener?.onAdClicked()
            }
        })
    }

    private fun loadBanner(
        owner: SlotLifecycle,
        style: BannerStyle,
        placementKey: String,
        listener: AdSlotListener?,
        screen: String?,
    ) {
        current = owner
        visibility = VISIBLE
        val placeholderHeight = style.placeholderHeightPx()
        val container = FrameLayout(context).apply { minimumHeight = placeholderHeight }
        val shimmer = ShimmerLayout(context).apply { LayoutInflater.from(context).inflate(R.layout.nextgen_skeleton_banner, this, true) }
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        // Fixed height (a MATCH_PARENT placeholder would stretch a wrap_content slot to the whole screen);
        // follows the container once BannerAds reserves the banner's exact height.
        addView(shimmer, LayoutParams(LayoutParams.MATCH_PARENT, placeholderHeight))
        container.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val height = bottom - top
            if (height > 0 && shimmer.layoutParams.height != height) {
                shimmer.post { shimmer.layoutParams = shimmer.layoutParams.apply { this.height = height } } // not during layout
            }
        }

        BannerAds.load(container, owner, placementKey, style.toBannerSize(), screen = screen, listener = object : BannerAdListener {
            override fun onAdLoaded(isCollapsible: Boolean) {
                removeView(shimmer)
                listener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(reason: String) {
                if (current === owner) visibility = GONE
                listener?.onAdFailedToLoad(reason)
            }

            override fun onAdClicked() {
                listener?.onAdClicked()
            }
        })
    }

    private fun NativeStyle.toTemplate() = when (this) {
        NativeStyle.SMALL -> NativeAdTemplateView.Template.SMALL
        NativeStyle.MEDIUM -> NativeAdTemplateView.Template.MEDIUM
        NativeStyle.LARGE -> NativeAdTemplateView.Template.LARGE
    }

    /** Height reserved before the banner's real size is known (BannerAds sets the exact one when loading starts). */
    private fun BannerStyle.placeholderHeightPx(): Int {
        val dp = when (this) {
            BannerStyle.STANDARD -> 50
            BannerStyle.LARGE -> 100
            BannerStyle.MEDIUM_RECTANGLE -> 250
            BannerStyle.INLINE_ADAPTIVE -> INLINE_MAX_HEIGHT_DP
            BannerStyle.ADAPTIVE, BannerStyle.COLLAPSIBLE_TOP, BannerStyle.COLLAPSIBLE_BOTTOM -> 60
        }
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * Lifecycle of one loaded ad: ends when the screen ends, or earlier when the slot loads
     * another ad. The ads module destroys the ad when this lifecycle is destroyed.
     */
    private class SlotLifecycle(private val parent: LifecycleOwner) : LifecycleOwner {

        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry

        private val parentObserver = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = destroy()
        }

        init {
            registry.currentState = Lifecycle.State.RESUMED
            parent.lifecycle.addObserver(parentObserver)
        }

        fun destroy() {
            if (registry.currentState == Lifecycle.State.DESTROYED) return
            parent.lifecycle.removeObserver(parentObserver)
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    companion object {
        /**
         * Starts loading [slot]'s ad in the background (native or banner, per the ads control) so an
         * AdSlotView on the next screen shows it at once. Nothing happens for an [AdSlot.Off] slot.
         */
        @MainThread
        fun preload(
            slot: AdSlot,
            nativePlacementKey: String,
            bannerPlacementKey: String,
            screen: String? = null,
            bufferSize: Int = 1,
        ) {
            when (slot) {
                AdSlot.Off -> Unit
                is AdSlot.Native -> NativeAds.preload(nativePlacementKey, screen, bufferSize)
                is AdSlot.Banner -> BannerAds.preload(bannerPlacementKey, slot.style.toBannerSize(), screen, bufferSize)
            }
        }

        /** Stops both preloads of a slot (e.g. once its screen has shown its ad). */
        @MainThread
        fun stopPreload(nativePlacementKey: String, bannerPlacementKey: String) {
            NativeAds.stop(nativePlacementKey)
            BannerAds.stopPreload(bannerPlacementKey)
        }
    }
}

/** Inline banners in content: about the height of a medium rectangle. */
private const val INLINE_MAX_HEIGHT_DP = 250

private fun BannerStyle.toBannerSize(): BannerSize = when (this) {
    BannerStyle.STANDARD -> BannerSize.Standard
    BannerStyle.LARGE -> BannerSize.Large
    BannerStyle.MEDIUM_RECTANGLE -> BannerSize.MediumRectangle
    BannerStyle.ADAPTIVE -> BannerSize.Anchored
    BannerStyle.INLINE_ADAPTIVE -> BannerSize.Inline(maxHeightDp = INLINE_MAX_HEIGHT_DP)
    BannerStyle.COLLAPSIBLE_TOP -> BannerSize.Collapsible(fromTop = true)
    BannerStyle.COLLAPSIBLE_BOTTOM -> BannerSize.Collapsible(fromTop = false)
}

/** Ad slot events, on the main thread. */
interface AdSlotListener {
    fun onAdLoaded() {}

    /** Off, not allowed (consent / premium / disabled) or no fill. The slot hides itself. */
    fun onAdFailedToLoad(reason: String) {}

    fun onAdClicked() {}
}
