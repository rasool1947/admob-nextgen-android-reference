package com.example.admob_next_gen.ads.natives.presentation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
 import com.example.admob_next_gen.databinding.LayoutNativeLargeBinding

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd



class AdNativeLargeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: LayoutNativeLargeBinding

    init {
        initView()
    }

    private fun initView() {
        binding = LayoutNativeLargeBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setNativeAd(nativeAd: NativeAd) {
        binding.mtvLoadingAds.visibility = GONE
        binding.adAttribute.visibility = VISIBLE
        binding.adCallToAction.visibility = VISIBLE

        // Assigning views
        binding.nativeAdView.headlineView = binding.adHeadline
        binding.nativeAdView.bodyView = binding.adBody
        binding.nativeAdView.callToActionView = binding.adCallToAction
        binding.nativeAdView.iconView = binding.adAppIcon

        // Filling up views
        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction
        binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)

        // Validating views
        binding.adAppIcon.isVisible = nativeAd.icon?.drawable != null
        binding.adCallToAction.isVisible = nativeAd.callToAction.isNullOrEmpty().not()

        visibility = VISIBLE

        // Register the native ad with the media view
        binding.nativeAdView.registerNativeAd(nativeAd, binding.adMediaView)
    }

    fun clearView() {
        binding.mtvLoadingAds.visibility = VISIBLE
        binding.adAttribute.visibility = GONE
        binding.adCallToAction.visibility = GONE
    }
}