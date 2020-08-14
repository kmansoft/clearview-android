package org.kman.clearview.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import android.widget.TextView

class CheckableTextView(context: Context, attrs: AttributeSet?) : TextView(context, attrs), Checkable {
    constructor(context: Context) : this(context, null)

    override fun isChecked(): Boolean {
        return mIsChecked
    }

    override fun setChecked(checked: Boolean) {
        if (mIsChecked != checked) {
            mIsChecked = checked
            refreshDrawableState()
            invalidate()
        }
    }

    override fun toggle() {
        mIsChecked = !mIsChecked
        refreshDrawableState()
        invalidate()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState: IntArray = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    companion object {
        val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }

    var mIsChecked: Boolean = false
}