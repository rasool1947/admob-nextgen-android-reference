package com.nextgen.ads.fullscreen

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.nextgen.ads.R

/** Shown for a moment before a full-screen ad. Not cancelable: the ad follows right after. */
internal class AdWaitDialog private constructor(private val activity: Activity, layout: Int, fullScreen: Boolean) : Dialog(activity) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(layout)
        setCancelable(false)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        if (fullScreen) window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    /** Safe to call at any time, also after the activity is gone. */
    fun dismissSafely() {
        if (isShowing && !activity.isFinishing && !activity.isDestroyed) dismiss()
    }

    companion object {
        /** Small "Loading ad…" card, e.g. before an interstitial. */
        fun loading(activity: Activity) = AdWaitDialog(activity, R.layout.nextgen_dialog_ad_loading, fullScreen = false)

        /** Full-screen "Welcome back" with the app icon, before the App Open ad on returning to the app. */
        fun welcomeBack(activity: Activity) = AdWaitDialog(activity, R.layout.nextgen_dialog_welcome_back, fullScreen = true).apply {
            findViewById<ImageView>(R.id.nextgen_welcome_icon).setImageDrawable(activity.applicationInfo.loadIcon(activity.packageManager))
        }
    }
}
