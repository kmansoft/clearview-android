package org.kman.clearview.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import org.kman.clearview.R
import org.kman.clearview.util.LayoutCompat

class CircleChartView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    constructor(context: Context) : this(context, null) {
    }

    fun setColors(colorBack: Int, colorFront: Int) {
        if (mPaintBack.color != colorBack || mPaintFront.color != colorFront) {
            mPaintBack.color = (colorBack or 0xFF_000000.toInt())
            mPaintFront.color = (colorFront or 0xFF_000000.toInt())
            invalidate()
        }
    }

    fun setText(text: String) {
        if (!mText.equals(text)) {
            mText = text
            mTextLayout = null
            invalidate()
        }
    }

    fun setValue(value: Double) {
        val clamped =
            if (value > 1.0) {
                1.0
            } else if (value < 0.0) {
                0.0
            } else {
                value
            }
        if (mValue != clamped) {
            mValue = clamped
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mTextLayout = null
    }

    override fun onDraw(canvas: Canvas) {
        val context = context
        val res = context.resources
        val thickness = res.getDimension(R.dimen.circle_chart_line_thickness)

        mPaintBack.strokeWidth = thickness
        mPaintFront.strokeWidth = thickness

        val w = width
        val h = height
        val r = mScratchRect

        val value = mValue * mAppearAnimator.update()

        r.set(0.0f, 0.0f, w.toFloat(), h.toFloat())
        r.inset((thickness + 1) / 2, (thickness + 1) / 2)
        canvas.drawArc(r, 270.0f, 360.0f, false, mPaintBack)
        canvas.drawArc(r, 270.0f, (value * 360.0).toFloat(), false, mPaintFront)

        mPaintText.textSize = res.getDimension(R.dimen.circle_chart_text_size)

        if (!mText.isEmpty()) {
            val textWidth = (w * 2) / 3
            var textLayout = mTextLayout
            if (textLayout == null) {
                textLayout = mLayoutCompat.create(mText, mPaintText, textWidth)
                mTextLayout = textLayout
            }

            val lineWidth = textLayout.getLineWidth(0).toInt()
            val textX = (w - lineWidth) / 2 + res.getDimensionPixelSize(R.dimen.circle_chart_text_shift)
            val textY = (h - textLayout.height) / 2
            canvas.save()
            canvas.translate(textX.toFloat(), textY.toFloat())
            textLayout.draw(canvas)
            canvas.restore()
        }
    }

    val mPaintBack = Paint(Paint.ANTI_ALIAS_FLAG)
    val mPaintFront = Paint(Paint.ANTI_ALIAS_FLAG)
    val mPaintText = TextPaint(Paint.ANTI_ALIAS_FLAG)

    var mText: String = ""
    var mValue: Double = 0.0

    var mTextLayout: StaticLayout? = null

    val mScratchRect = RectF()
    val mLayoutCompat = LayoutCompat.factory()

    val mAppearAnimator = SimpleValueAnimator(this)

    init {
        mPaintFront.style = Paint.Style.STROKE
        mPaintBack.style = Paint.Style.STROKE

        mValue = Math.random()
    }
}
