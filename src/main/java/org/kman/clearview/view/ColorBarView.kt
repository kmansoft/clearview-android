package org.kman.clearview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class ColorBarView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mPaintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintBar = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setColor(color: Int) {
        if (mColor != color) {
            mColor = color or 0xFF_000000.toInt()
            invalidate()
        }
    }

    fun setPercent(percent: Int) {
        if (mPercent != percent) {
            mPercent = percent
            invalidate()
        }
    }

    fun setPercent(fraction: Int, total: Int) {
        setPercent(
            if (total == 0) {
                0
            } else {
                fraction * 100 / total
            }
        )
    }

    fun setPercent(fraction: Long, total: Long) {
        setPercent(
            if (total == 0L) {
                0
            } else {
                (fraction * 100L / total).toInt()
            }
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height

        mRect.set(0, 0, w, h)
        canvas.drawRect(mRect, mPaintFill)

        mPaintBar.color = mColor
        mRect.right = w * mPercent / 100
        canvas.drawRect(mRect, mPaintBar)
    }

    init {
        mPaintFill.style = Paint.Style.FILL
        mPaintFill.color = 0xFF_E0E0E0.toInt()
        mPaintBar.style = Paint.Style.FILL
    }

    private val mRect = Rect()
    private var mColor: Int = 0
    private var mPercent: Int = 0
}