package org.kman.clearview.chart

import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class SimpleValueAnimator(val view: View) {
    fun update(): Float {
        if (mIsFirst) {
            mIsFirst = false
            mFromT = SystemClock.elapsedRealtime()
            mFromV = 0.0
            mNextV = 1.0
        }

        if (mFromT != 0L) {
            mCurrT = SystemClock.elapsedRealtime()
            val factor = (mCurrT - mFromT) / 300.0f
            if (factor >= 1.0) {
                mFromV = mNextV
                mCurrV = mNextV
                mFromT = 0
                mCurrT = 0
            } else {
                mCurrV = mFromV + mInterpolator.getInterpolation(factor) * (mNextV - mFromV)
                view.postInvalidateOnAnimation()
            }
        }

        return mCurrV.toFloat()
    }

    private var mIsFirst = true

    private val mInterpolator = AccelerateDecelerateInterpolator()

    private var mFromV: Double = 0.0
    private var mNextV: Double = 1.0
    private var mCurrV: Double = mFromV

    private var mFromT: Long = 0
    private var mCurrT: Long = mFromT
}

