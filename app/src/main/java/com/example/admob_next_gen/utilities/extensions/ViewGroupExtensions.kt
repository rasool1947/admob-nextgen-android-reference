package com.example.admob_next_gen.utilities.extensions

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.example.admob_next_gen.utilities.utils.Constants.TAG

/**
 * Date: 1/17/2025
 *
 */

fun ViewGroup.addCleanView(view: View?) {
    if (view == null) {
        Log.e(TAG, "addCleanView: View ref is null")
        return
    }
    (view.parent as? ViewGroup)?.removeView(view)
    this.removeAllViews()
    view.let { this.addView(it) }
}