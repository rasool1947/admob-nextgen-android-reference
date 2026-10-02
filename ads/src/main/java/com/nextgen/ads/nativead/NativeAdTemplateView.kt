package com.nextgen.ads.nativead

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.nextgen.ads.R
import com.nextgen.ads.internal.ShimmerLayout

/**
 * Ready-made native ad layout. Use it in XML and fill it with [NativeAds.loadInto]:
 * ```
 * <com.nextgen.ads.nativead.NativeAdTemplateView
 *     android:id="@+id/nativeAd"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:nativeTemplate="medium" />   <!-- small (no media) | medium | large -->
 * ```
 * While loading, the layout keeps its full size (no jump when the ad arrives) behind a shimmer
 * placeholder. For a custom design, inflate your own NativeAdView and use [NativeAds.load] instead.
 */
class NativeAdTemplateView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    explicitTemplate: Template?,
) : FrameLayout(context, attrs, defStyleAttr) {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this(context, attrs, defStyleAttr, null)

    /** Creates the view from code, e.g. inside an ad slot. */
    constructor(context: Context, template: Template) : this(context, null, 0, template)

    enum class Template(internal val layout: Int, internal val skeleton: Int) {
        /** Icon, headline, body and button; no media. */
        SMALL(R.layout.nextgen_native_small, R.layout.nextgen_skeleton_native_small),
        /** Adds a 130dp media view. */
        MEDIUM(R.layout.nextgen_native_medium, R.layout.nextgen_skeleton_native_medium),
        /** Two lines of body, 200dp media, price and store. */
        LARGE(R.layout.nextgen_native_large, R.layout.nextgen_skeleton_native_large),
    }

    var template: Template = Template.MEDIUM
        private set

    private val nativeAdView: NativeAdView
    private val headline: TextView
    private val body: TextView
    private val advertiser: TextView
    private val rating: RatingBar
    private val icon: ImageView
    private val callToAction: Button
    private val price: TextView?
    private val store: TextView?
    private val media: MediaView?
    private val placeholder: ShimmerLayout

    init {
        template = explicitTemplate ?: Template.MEDIUM
        if (explicitTemplate == null) {
            context.withStyledAttributes(attrs, R.styleable.NativeAdTemplateView) {
                template = Template.entries[getInt(R.styleable.NativeAdTemplateView_nativeTemplate, Template.MEDIUM.ordinal)]
            }
        }
        val inflater = LayoutInflater.from(context)
        inflater.inflate(template.layout, this, true)

        nativeAdView = findViewById(R.id.nextgen_native_ad_view)
        headline = findViewById(R.id.nextgen_native_headline)
        body = findViewById(R.id.nextgen_native_body)
        advertiser = findViewById(R.id.nextgen_native_advertiser)
        rating = findViewById(R.id.nextgen_native_rating)
        icon = findViewById(R.id.nextgen_native_icon)
        callToAction = findViewById(R.id.nextgen_native_cta)
        price = findViewById(R.id.nextgen_native_price)
        store = findViewById(R.id.nextgen_native_store)
        media = findViewById(R.id.nextgen_native_media)

        nativeAdView.headlineView = headline
        nativeAdView.bodyView = body
        nativeAdView.advertiserView = advertiser
        nativeAdView.starRatingView = rating
        nativeAdView.iconView = icon
        nativeAdView.callToActionView = callToAction
        nativeAdView.priceView = price
        nativeAdView.storeView = store

        placeholder = ShimmerLayout(context).apply { inflater.inflate(template.skeleton, this, true) }
        addView(placeholder, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        if (isInEditMode) showSample() else showPlaceholder()
    }

    /** Reserves the ad's space and shows the shimmer placeholder. */
    fun showPlaceholder() {
        visibility = VISIBLE
        nativeAdView.visibility = INVISIBLE // still measured, so the final size is reserved
        placeholder.visibility = VISIBLE
    }

    /** Collapses the view (no ad). */
    fun hide() {
        visibility = GONE
    }

    /** Shows [ad]. Normally called by [NativeAds.loadInto]. */
    fun bind(ad: NativeAd) {
        headline.text = ad.headline
        body.setOptionalText(ad.body)
        advertiser.setOptionalText(ad.advertiser)
        price?.setOptionalText(ad.price)
        store?.setOptionalText(ad.store)
        callToAction.setOptionalText(ad.callToAction)

        val iconDrawable = ad.icon?.drawable
        icon.setImageDrawable(iconDrawable)
        icon.isVisible = iconDrawable != null

        val stars = ad.starRating
        rating.isVisible = stars != null && stars > 0
        if (stars != null) rating.rating = stars.toFloat()

        // Registers clicks/impressions; AdChoices is added by the SDK.
        nativeAdView.registerNativeAd(ad, media)

        placeholder.visibility = GONE
        nativeAdView.visibility = VISIBLE
        visibility = VISIBLE
    }

    private fun TextView.setOptionalText(value: String?) {
        text = value
        isVisible = !value.isNullOrBlank()
    }

    @SuppressLint("SetTextI18n") // Android Studio layout preview only
    private fun showSample() {
        headline.text = "Sample ad headline"
        body.text = "Sample body text of the native ad."
        callToAction.text = "Install"
        placeholder.visibility = View.GONE
    }
}
