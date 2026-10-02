package com.nextgen.ads.fullscreen

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import com.nextgen.ads.R

/** "Loading ad…" shown for a moment before a full-screen ad. Not cancelable: the ad follows right after. */
internal class AdLoadingDialog(private val activity: Activity) : Dialog(activity) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.nextgen_dialog_ad_loading)
        setCancelable(false)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }

    /** Safe to call at any time, also after the activity is gone. */
    fun dismissSafely() {
        if (isShowing && !activity.isFinishing && !activity.isDestroyed) dismiss()
    }
}
