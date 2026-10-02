package com.nextgen.ads.internal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.nextgen.ads.R

/**
 * Draws its children (grey "skeleton" blocks) with a light band sweeping across them, the usual
 * loading placeholder for ads. No library needed: the band is painted only over the children.
 */
internal class ShimmerLayout(context: Context) : FrameLayout(context) {

    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) // only where the skeleton is drawn
    }
    private val bandMatrix = Matrix()
    private val highlight = ContextCompat.getColor(context, R.color.nextgen_shimmer_highlight)

    private val animator = ValueAnimator.ofFloat(-BAND_FRACTION, 1f + BAND_FRACTION).apply {
        duration = SWEEP_MILLIS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val band = w * BAND_FRACTION
        bandPaint.shader = LinearGradient(
            0f, 0f, band, 0f,
            intArrayOf(0x00FFFFFF and highlight, highlight, 0x00FFFFFF and highlight),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return super.dispatchDraw(canvas)

        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)
        val position = animator.animatedValue as Float
        bandMatrix.setTranslate(position * width - width * BAND_FRACTION, 0f)
        bandPaint.shader?.setLocalMatrix(bandMatrix)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bandPaint)
        canvas.restoreToCount(layer)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    /** Runs only while actually on screen. */
    private fun updateAnimation() {
        val shouldRun = isAttachedToWindow && isShown
        if (shouldRun && !animator.isStarted) animator.start()
        if (!shouldRun && animator.isStarted) animator.cancel()
    }

    private companion object {
        const val BAND_FRACTION = 0.4f
        const val SWEEP_MILLIS = 1_300L
    }
}
