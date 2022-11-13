package org.kman.clearview.chart

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.*
import org.kman.clearview.R
import org.kman.clearview.core.RsDataPoint
import org.kman.clearview.core.RsDataSeries
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class TimeChartView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    constructor(context: Context) : this(context, null) {

    }

    data class DataOption(val name: String, val fill: Int, val line: Int)

    fun setController(controller: TimeChartController) {
        mController = controller
        controller.register(this)
    }

    fun setTitle(title: Int) {
        mTitle = context.getString(title)
        invalidate()
    }

    fun setTitle(title: String) {
        mTitle = title
        invalidate()
    }

    fun setDataOptions(options: Array<DataOption>) {
        mOptions = options

        invalidate()
    }

    fun setData(series: Array<RsDataSeries>) {
        mSeries = series
        mMaxValue = -1.0

        val seriesSize = mSeries.size
        if (seriesSize > 0) {
            if (mValuesBelow.size < series[0].points.size) {
                mValuesBelow = Array(series[0].points.size) { 0.0 }
            }
            if (mPaintDataFill.size < seriesSize) {
                mPaintDataFill = Array(seriesSize) { Paint(Paint.ANTI_ALIAS_FLAG) }
            }
            if (mPaintDataLine.size < seriesSize) {
                mPaintDataLine = Array(seriesSize) { Paint(Paint.ANTI_ALIAS_FLAG) }
            }
            if (mStrokeSave.size < seriesSize) {
                mStrokeSave = Array(seriesSize) { mutableListOf<Int>() }
            }
            mIsPathCacheValid = false
        }

        invalidate()
    }

    fun setValueFormatter(formatter: (Double) -> String) {
        mValueFormatter = formatter
    }

    fun setMaxValue(maxValue: Double) {
        mMaxValueSet = maxValue
        mIsMaxValueSet = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width - mPaddingLeft - mPaddingRight) / 3 + mPaddingTop + mPaddingBottom

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                child.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                )
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                child.layout(0, 0, width, height)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN) {
            if (mLegendIndex != -1) {
                val xPosLine = mMapperX.map(mLegendIndex.toDouble())
                val x = event.x
                if (abs(x - xPosLine) <= mViewTouchSlop * 2) {
                    mIsMovingLegend = true
                    mMovingLegendStartX = x.toInt()
                    requestDisallowInterceptTouchEvent(true)

                    return true
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mIsMovingLegend) {
                val x = event.x
                val index = mMapperX.reverse(x.toInt()).roundToInt()
                if (index >= 0 && !mSeries.isEmpty() && index < mSeries[0].points.size) {
                    mController?.onShowLegend(this, index)
                }
                return true
            }
        }

        if (mIsMovingLegend) {
            requestDisallowInterceptTouchEvent(false)
            mIsMovingLegend = false
        }

        if (mGestureDetector.onTouchEvent(event)) {
            return true
        }

        return super.onTouchEvent(event)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (mSeries.isEmpty() || mSeries[0].points.isEmpty()) {
            return
        }
        if (mMaxValue < 0) {
            mMaxValue =
                if (mIsMaxValueSet) {
                    mMaxValueSet
                } else {
                    maxOfAllSeries()
                }
        }

        val viewWidth = width
        val viewHeight = height

        // Occasionally we get a short series from the back-end, pick the smallest length to avoid crashing
        var posCount = mSeries[0].points.size
        for (seriesIndex in mSeries.size - 1 downTo 0) {
            val series = mSeries[seriesIndex]
            val pos = series.points.size
            if (posCount > pos) {
                posCount = pos
            }
        }

        mMapperX.set(0.0, (posCount - 1).toDouble(), mPaddingLeft, viewWidth - mPaddingRight)
        mMapperY.set(0.0, mMaxValue, mPaddingTop, viewHeight - mPaddingBottom)
        mValuesBelow.fill(0.0)

        // Axis labels
        val axisXLabels = getAxisXLabels(posCount)
        val axisYLabels = getAxisYLabels(mMaxValue)

        // Horizontal lines
        for (value in axisYLabels) {
            val vy = mMapperY.map(value)
            canvas.drawLine(
                mPaddingLeft.toFloat(), vy.toFloat(),
                (viewWidth - mPaddingRight).toFloat(), vy.toFloat(), mPaintDecorLines
            )
        }

        // Clip animation
        val clipValue = mClipAnimator.update()
        val clipSaved =
            if (clipValue < 1.0) {
                val clipRight = mMapperX.toMin + (mMapperX.toMax - mMapperX.toMin) * clipValue
                canvas.save()
                canvas.clipRect(0, 0, clipRight.toInt(), height)
                true
            } else {
                false
            }

        // Use cache if we can
        if (mIsPathCacheValid) {
            for (seriesIndex in mSeries.size - 1 downTo 0) {
                val paint = mPaintDataFill[seriesIndex]

                for (path in mPathCacheFill[seriesIndex]) {
                    canvas.drawPath(path, paint)
                }
            }

            for (seriesIndex in mSeries.size - 1 downTo 0) {
                val paint = mPaintDataLine[seriesIndex]

                for (path in mPathCacheStroke[seriesIndex]) {
                    canvas.drawPath(path, paint)
                }
            }
        } else {
            // Prepare for saving paths as caches
            val seriesSize = mSeries.size
            val pathCacheFill = Array<MutableList<Path>>(seriesSize) { mutableListOf() }
            val pathCacheStroke = Array<MutableList<Path>>(seriesSize) { mutableListOf() }

            // Fill the areas
            for (seriesIndex in mSeries.size - 1 downTo 0) {
                val series = mSeries[seriesIndex]

                val stroke = mStrokeSave[seriesIndex]
                stroke.clear()

                var nextSpanStart = 0
                while (nextSpanStart < posCount) {
                    val nextSpanPair = getNextValueSpan(nextSpanStart, posCount)
                    if (nextSpanPair.first >= posCount) {
                        break
                    }
                    val start = nextSpanPair.first
                    val end = nextSpanPair.second

                    val path = Path()
                    for (i in start until end) {
                        val x = mMapperX.map(i.toDouble())
                        val y = mMapperY.map(
                            series.points[i].v + mValuesBelow[i]
                        )
                        if (i == start) {
                            path.moveTo(
                                x.toFloat(),
                                mMapperY.map(mValuesBelow[i]).toFloat()
                            )
                        }
                        path.lineTo(
                            x.toFloat(),
                            y.toFloat()
                        )
                        if (i == end - 1) {
                            path.lineTo(
                                x.toFloat(),
                                mMapperY.map(mValuesBelow[i]).toFloat()
                            )
                            if (seriesIndex != mSeries.size - 1) {
                                for (j in end - 2 downTo start) {
                                    path.lineTo(
                                        mMapperX.map(j.toDouble()).toFloat(),
                                        mMapperY.map(mValuesBelow[j]).toFloat()
                                    )
                                }
                            }
                            val paint = mPaintDataFill[seriesIndex]
                            paint.style = Paint.Style.FILL
                            paint.color = mOptions[seriesIndex].fill or 0xC0000000.toInt()
                            canvas.drawPath(path, paint)
                        }
                        stroke.add(x)
                        stroke.add(y)
                    }
                    stroke.add(-1)

                    pathCacheFill[seriesIndex].add(path)

                    for (i in start until end) {
                        mValuesBelow[i] += series.points[i].v
                    }

                    nextSpanStart = nextSpanPair.second
                }
            }

            // Stroke the areas as a separate step
            for (seriesIndex in mSeries.size - 1 downTo 0) {
                val stroke = mStrokeSave[seriesIndex]
                var i = 0
                while (i < stroke.size) {
                    var x = stroke[i++]
                    if (x >= 0) {
                        val path = Path()
                        path.moveTo(x.toFloat(), stroke[i++].toFloat())

                        var j = i
                        while (j < stroke.size) {
                            x = stroke[j++]
                            if (x < 0) {
                                break
                            }
                            path.lineTo(x.toFloat(), stroke[j++].toFloat())
                        }

                        val paint = mPaintDataLine[seriesIndex]
                        paint.strokeWidth = mStrokeWidth
                        paint.style = Paint.Style.STROKE
                        paint.color = mOptions[seriesIndex].line or 0xFF000000.toInt()
                        canvas.drawPath(path, paint)

                        pathCacheStroke[seriesIndex].add(path)
                    }
                }
            }

            // Cache
            mPathCacheFill = pathCacheFill
            mPathCacheStroke = pathCacheStroke
            mIsPathCacheValid = true
        }

        if (clipSaved) {
            canvas.restore()
        }

        // Draw ticks and time labels (X axis)

        val isDateFormat = (mSeries[0].points[1].t - mSeries[0].points[0].t) >= 8 * 1440
        for (i in 0 until axisXLabels.size) {
            val index = axisXLabels[i]

            if (i != 0 && i != axisXLabels.size - 1) {
                val tx = mMapperX.map(index.toDouble()).toFloat()
                val ty = mMapperY.map(0.0).toFloat()
                canvas.drawLine(
                    tx, ty - mTickSize,
                    tx, ty + mTickSize, mPaintDecorLines
                )
            }

            val dt = mSeries[0].points[index].t.toLong() * 1000L
            val dv = DateUtils.formatDateTime(
                context, dt,
                if (isDateFormat) {
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
                } else {
                    DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_TIME
                }
            )

            val dw = mTextPaintLabel.measureText(dv)
            val dx = mMapperX.map(index.toDouble()) - dw * 2 / 3 - mMetricsLabel.descent
            val dy = mMapperY.map(0.0) + dw + mMetricsLabel.descent

            canvas.save()
            canvas.translate(dx, dy)
            canvas.rotate(-36.0f)
            canvas.drawText(dv, 0.0f, 0.0f, mTextPaintLabel)
            canvas.restore()
        }

        // Draw value labels (Y axis)
        var prevLabel = ""
        for (value in axisYLabels) {
            val rounded = roundAxisYValue(value)
            val label = mValueFormatter(rounded)
            if (prevLabel != label) {
                prevLabel = label
                val vw = mTextPaintLabel.measureText(label)
                val vx = mPaddingLeft - vw - mMetricsLabel.bottom
                val vy = mMapperY.map(value).toFloat() + mMetricsLabel.bottom
                canvas.drawText(label, vx, vy, mTextPaintLabel)
            }
        }

        // Title
        if (mTitle.isNotEmpty()) {
            val textWidth = mTextPaintTitle.measureText(mTitle)
            // val textHeight = mMetricsTitle.bottom - mMetricsTitle.top
            val xTitle = viewWidth - textWidth
            val yTitle = -mMetricsTitle.ascent.toFloat()
            canvas.drawText(mTitle, xTitle, yTitle, mTextPaintTitle)
        }
    }

    fun showLegendView(index: Int) {
        showLegendViewImpl(index)
    }

    fun hideLegendView() {
        hideLegendViewImpl()
    }

    private fun maxOfAllSeries(): Double {
        val getValue: (a: Array<RsDataSeries>, i: Int) -> Double =
            when (mSeries.size) {
                1 -> {
                    { a, i ->
                        a[0].points[i].v
                    }
                }
                2 -> {
                    { a, i ->
                        a[0].points[i].v + a[1].points[i].v
                    }
                }
                3 -> {
                    { a, i ->
                        a[0].points[i].v + a[1].points[i].v + a[2].points[i].v
                    }
                }
                4 -> {
                    { a, i ->
                        a[0].points[i].v + a[1].points[i].v + a[2].points[i].v + a[3].points[i].v
                    }
                }
                else -> {
                    { a, i ->
                        var s = a[0].points[i].v
                        for (j in 1 until a.size) {
                            s += a[j].points[i].v
                        }
                        s
                    }

                }
            }

        var max = getValue(mSeries, 0)
        for (i in 1 until mSeries[0].points.size) {
            val sum = getValue(mSeries, i)
            if (max < sum) {
                max = sum
            }
        }
        return max
    }

    private fun getNextValueSpan(start: Int, end: Int): Pair<Int, Int> {
        val points = mSeries[0].points
        val len = end
        for (s in start until len) {
            if (!points[s].n) {
                for (e in s + 1 until len) {
                    if (points[e].n) {
                        return Pair(s, e)
                    }
                }
                return Pair(s, len)
            }
        }
        return Pair(len, len)
    }

    private fun getAxisXLabels(posCount: Int): List<Int> {
        val posStep = when (posCount) {
            30 + 1 -> 5
            36 + 1 -> 6
            else -> 4
        }
        var start = 0
        for (i in posCount - 1 downTo 0 step posStep) {
            start = i
        }
        mAxisXLabels.clear()
        for (i in start until posCount step posStep) {
            mAxisXLabels.add(i)
        }
        return mAxisXLabels
    }

    private fun getAxisYLabels(max: Double): List<Double> {
        mAxisYLabels.clear()
        var prev = -1.0
        for (i in 0..4) {
            val curr = max * i / 4.0
            if (prev != curr) {
                prev = curr
                mAxisYLabels.add(curr)
            }
        }
        return mAxisYLabels
    }

    private fun roundAxisYValue(value: Double): Double {
        if (value >= 10) {
            return (value).roundToLong().toDouble()
        }
        return ((value * 100) / 100).roundToLong().toDouble()
    }

    private fun onLegendClick(x: Int) {
        if (mSeries.isEmpty()) {
            mController?.onHideLegend(this)
            return
        }

        val index = mMapperX.reverse(x).roundToInt()
        if (index < 0 || index >= mSeries[0].points.size) {
            mController?.onHideLegend(this)
            return
        }

        mController?.onShowLegend(this, index)
    }

    private fun onLegendDoubleTap(x: Int) {
        if (mLegendIndex == -1) {
            val index = mMapperX.reverse(x).roundToInt()
            if (index >= 0 && !mSeries.isEmpty() && index < mSeries[0].points.size) {
                mController?.onShowLegend(this, index)
            }
        } else {
            mController?.onHideLegend(this)
        }
    }

    private fun showLegendViewImpl(index: Int) {
        if (index < 0 || mSeries.isEmpty() || index > mSeries[0].points.size) {
            return
        }

        if (mLegendIndex == -1) {
            mLegendView.visibility = View.VISIBLE

            mShowHideAnimator?.cancel()

            val showHideAnimator = mLegendView.animate().alpha(1.0f)
            showHideAnimator.setListener(null)
            showHideAnimator.setDuration(500L).start()
            mShowHideAnimator = showHideAnimator

            mMoveAnimator = null
        }

        mLegendIndex = index
        mLegendView.invalidate()
    }

    private fun hideLegendViewImpl() {
        if (mLegendIndex != -1) {
            mShowHideAnimator?.cancel()

            val showHideAnimator = mLegendView.animate().alpha(0.0f)
            showHideAnimator.setListener(object : AnimatorListenerAdapter() {
                var mIsCanceled: Boolean = false
                override fun onAnimationCancel(animation: Animator) {
                    mIsCanceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!mIsCanceled) {
                        mLegendView.visibility = View.GONE
                    }
                }

            })
            showHideAnimator.setDuration(500L).start()
            mShowHideAnimator = showHideAnimator

            mMoveAnimator = null
        }

        mLegendIndex = -1
        invalidate()
    }

    private fun onDrawLegendView(canvas: Canvas) {
        canvas.save()

        val context = context
        val res = context.resources
        val w = width
        val h = height
        val padding = res.getDimensionPixelSize(R.dimen.line_chart_legend_padding)
        val blockWidth = res.getDimensionPixelSize(R.dimen.line_chart_legend_color_block_width)
        val blockHeight = res.getDimensionPixelSize(R.dimen.line_chart_legend_color_block_height)
        val legendWidthAdd = res.getDimensionPixelSize(R.dimen.line_chart_legend_width_add)

        val r = mTempRect

        var maxWidthValue = 0
        for (i in 0 until mSeries.size) {
            val series = mSeries[i]
            val point = series.points[mLegendIndex]
            val value = formatValueWithNull(point)

            val tw = mTextPaintLegend.measureText(value)
            if (maxWidthValue < tw) {
                maxWidthValue = tw.toInt()
            }
        }

        val legendWidth = legendWidthAdd + maxWidthValue + padding
        val legendHeight = padding + mSeries.size * (padding + blockHeight)

        val xPosLine = mMapperX.map(mLegendIndex.toDouble())

        canvas.drawLine(
            xPosLine.toFloat(), mPaddingTop.toFloat(), xPosLine.toFloat(),
            (h - mPaddingBottom).toFloat(), mPaintDecorTicks
        )
        var xPosLegend = xPosLine - (padding + legendWidth)
        if (xPosLegend < mPaddingLeft) {
            xPosLegend = xPosLine + padding
        } else if (xPosLegend > w - padding - legendWidth) {
            xPosLegend = xPosLine - padding - legendWidth
        }

        val positionAnimator = PositionAnimator.check(mMoveAnimator, mLegendView, xPosLegend)
        mMoveAnimator = positionAnimator
        val xPosLegendCurrent = positionAnimator.update()

        canvas.translate(xPosLegendCurrent.toFloat(), mPaddingTop.toFloat())

        mLegendFillPaint.color = 0xF0FFFFFF.toInt()
        r.set(0, 0, legendWidth, legendHeight)
        canvas.drawRect(r, mLegendFillPaint)

        for (i in 0 until mSeries.size) {
            val options = mOptions[i]
            val series = mSeries[i]

            val bx = padding
            val by = padding + i * (blockHeight + padding)
            mLegendFillPaint.color = options.fill or 0xFF000000.toInt()
            r.set(bx, by, bx + blockWidth, by + blockHeight)
            canvas.drawRect(r, mLegendFillPaint)

            val point = series.points[mLegendIndex]
            val value = formatValueWithNull(point)

            val th = mMetricsLegend.bottom - mMetricsLegend.top
            val tw = mTextPaintLegend.measureText(value)
            val tx = legendWidth - padding - tw
            val ty = by + (blockHeight - th.toFloat()) / 2 - mMetricsLegend.top

            canvas.drawText(options.name, 2 * padding + blockWidth.toFloat(), ty, mTextPaintLegend)
            canvas.drawText(value, tx, ty, mTextPaintLegend)
        }

        canvas.restore()
    }

    private fun formatValueWithNull(p: RsDataPoint): String {
        return if (p.n) {
            "n/a"
        } else {
            mValueFormatter(p.v)
        }
    }

    private class Mapper(val flipped: Boolean) {
        fun set(fromMin: Double, fromMax: Double, toMin: Int, toMax: Int) {
            this.fromMin = fromMin
            this.fromMax = fromMax
            this.toMin = toMin
            this.toMax = toMax

            if (this.fromMax < this.fromMin + 0.001) {
                this.fromMax = this.fromMin + 0.001
            }
        }

        fun map(value: Double): Int {
            return if (flipped) {
                (toMax - (value - fromMin) * (toMax - toMin) / (fromMax - fromMin)).toInt()
            } else {
                (toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin)).toInt()
            }
        }

        fun reverse(r: Int): Double {
            return fromMin + (r - toMin) * (fromMax - fromMin) / (toMax - toMin)
        }

        var fromMin: Double = 0.0
        var fromMax: Double = 1.0
        var toMin: Int = 0
        var toMax: Int = 1
    }

    private class LegendView(context: Context, val parent: TimeChartView) : View(context) {
        override fun onDraw(canvas: Canvas) {
            parent.onDrawLegendView(canvas)
        }
    }

    private class GestureListener(val parent: TimeChartView) :
        GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            parent.onLegendClick(e.x.toInt())
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            parent.onLegendClick(e.x.toInt())
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            parent.onLegendDoubleTap(e.x.toInt())
            return true
        }
    }

    private class PositionAnimator(val view: View, x: Int) {
        fun setNext(x: Int) {
            if (mNextX != x) {
                mFromX = mCurrX
                mNextX = x
                mFromT = SystemClock.elapsedRealtime()
                view.postInvalidateOnAnimation()
            }
        }

        fun update(): Int {
            if (mFromT != 0L) {
                mCurrT = SystemClock.elapsedRealtime()
                val factor = (mCurrT - mFromT) / 200.0
                if (factor >= 1.0) {
                    mFromX = mNextX
                    mCurrX = mNextX
                    mFromT = 0
                    mCurrT = 0
                } else {
                    mCurrX = (mFromX + factor * (mNextX - mFromX)).toInt()
                    view.postInvalidateOnAnimation()
                }
            }

            return mCurrX
        }

        companion object {
            fun check(animator: PositionAnimator?, view: View, x: Int): PositionAnimator {
                if (animator == null) {
                    return PositionAnimator(view, x)
                }

                animator.setNext(x)
                return animator
            }
        }

        private var mFromX: Int = x
        private var mNextX: Int = mFromX
        private var mCurrX: Int = mFromX

        private var mFromT: Long = 0
        private var mCurrT: Long = mFromT
    }

    companion object {
        val COLOR_LINES = 0xFFc0c0c0.toInt()
        val COLOR_TICKS = 0xFF202020.toInt()
        val COLOR_LEGEND = 0xE0FFFFFF.toInt()
        val TAG = "TimeChartView"
    }

    private var mController: TimeChartController? = null

    private var mSeries: Array<RsDataSeries> = emptyArray()
    private var mMaxValue: Double = -1.0
    private var mMaxValueSet: Double = 0.0
    private var mIsMaxValueSet: Boolean = false
    private var mOptions: Array<DataOption> = emptyArray()
    private var mMapperX = Mapper(false)
    private var mMapperY = Mapper(true)
    private var mValuesBelow: Array<Double> = emptyArray()
    private var mStrokeSave: Array<MutableList<Int>> = emptyArray()
    private var mTitle: String = ""

    private var mPaintDataFill: Array<Paint> = emptyArray()
    private var mPaintDataLine: Array<Paint> = emptyArray()

    private val mStrokeWidth: Float
    private val mPaddingLeft: Int
    private val mPaddingRight: Int
    private val mPaddingTop: Int
    private val mPaddingBottom: Int

    private val mPaintDecorLines = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintDecorTicks = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mTextPaintTitle = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mMetricsTitle = Paint.FontMetricsInt()

    private val mTextPaintLabel = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mMetricsLabel = Paint.FontMetricsInt()

    private val mAxisXLabels = mutableListOf<Int>()
    private val mTickSize: Int
    private val mAxisYLabels = mutableListOf<Double>()

    private val mGestureListener: GestureListener
    private val mGestureDetector: GestureDetector

    private val mLegendView: LegendView
    private val mLegendFillPaint: Paint
    private var mLegendIndex: Int = -1
    private val mTextPaintLegend = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mMetricsLegend = mTextPaintLegend.fontMetricsInt

    private val mTempRect = Rect()

    private var mMoveAnimator: PositionAnimator? = null
    private var mShowHideAnimator: ViewPropertyAnimator? = null

    private var mIsMovingLegend: Boolean = false
    private var mMovingLegendStartX: Int = 0
    private val mViewTouchSlop: Int

    private var mClipAnimator = SimpleValueAnimator(this)

    private var mIsPathCacheValid = false
    private var mPathCacheFill: Array<MutableList<Path>> = arrayOf()
    private var mPathCacheStroke: Array<MutableList<Path>> = arrayOf()

    private var mValueFormatter: (Double) -> String = {
        val l = it.toLong()
        if (it == l.toDouble()) {
            l.toString()
        } else {
            String.format(Locale.US, "%.2f", it)
        }
    }

    init {
        val res = context.resources
        mStrokeWidth = res.getDimension(R.dimen.line_chart_line_width)

        mPaddingLeft = res.getDimensionPixelSize(R.dimen.line_chart_padding_left)
        mPaddingRight = res.getDimensionPixelSize(R.dimen.line_chart_padding_right)
        mPaddingTop = res.getDimensionPixelSize(R.dimen.line_chart_padding_top)
        mPaddingBottom = res.getDimensionPixelSize(R.dimen.line_chart_padding_bottom)

        mPaintDecorLines.style = Paint.Style.STROKE
        mPaintDecorLines.color = COLOR_LINES
        mPaintDecorTicks.style = Paint.Style.STROKE
        mPaintDecorTicks.color = COLOR_TICKS

        mTextPaintTitle.textSize = res.getDimension(R.dimen.line_chart_font_size_title)
        mTextPaintTitle.color = COLOR_TICKS
        mTextPaintTitle.getFontMetricsInt(mMetricsTitle)
        mTextPaintTitle.typeface = Typeface.SANS_SERIF

        mTextPaintLabel.textSize = res.getDimension(R.dimen.line_chart_font_size_label)
        mTextPaintLabel.color = COLOR_TICKS
        mTextPaintLabel.getFontMetricsInt(mMetricsLabel)
//        mTextPaintLabel.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)

        mTickSize = res.getDimensionPixelSize(R.dimen.line_chart_tick_size)

        mGestureListener = GestureListener(this)
        mGestureDetector = GestureDetector(context, mGestureListener)
        mGestureDetector.setOnDoubleTapListener(mGestureListener)

        setWillNotDraw(false)
        mLegendView = LegendView(context, this)
        mLegendView.visibility = View.GONE
        mLegendView.alpha = 0.0f
        addView(mLegendView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        mLegendFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLegendFillPaint.color = COLOR_LEGEND
        mLegendFillPaint.style = Paint.Style.FILL
        mTextPaintLegend.textSize = res.getDimension(R.dimen.line_chart_font_size_legend)

        val viewConfig = ViewConfiguration.get(context)
        mViewTouchSlop = viewConfig.scaledTouchSlop
    }
}