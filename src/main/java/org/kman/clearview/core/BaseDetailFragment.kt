package org.kman.clearview.core

import android.os.Bundle

abstract class BaseDetailFragment() : BaseTimeFragment() {

    fun getNodeId(): String {
        return mNodeId
    }

    fun getNodeTitle(): String {
        return mNodeTitle
    }

    override fun setTimeMinutes(minutes: Int) {
        mMinutes = minutes
    }

    abstract fun getNavigationId(): Int

    abstract fun getTitleId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        args?.also {
            mNodeId = it.getString(KEY_NODE_ID, "")
            mNodeTitle = it.getString(KEY_NODE_TITLE, "")
            mMinutes = it.getInt(KEY_MINUTES, DEFAULT_MINUTES)
        }

        savedInstanceState?.also {
            mMinutes = it.getInt(KEY_MINUTES, DEFAULT_MINUTES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_MINUTES, mMinutes)
    }

    fun getTimeMinutes(): Int {
        return mMinutes
    }

    fun buildTimeWindow(): RqTimeWindow {
        return TimePeriod.buildTimeWindow(mMinutes)
    }

    companion object {
        val KEY_NODE_ID = "nodeId"
        val KEY_NODE_TITLE = "nodeTitle"
        val KEY_MINUTES = "minutes"
    }

    private var mNodeId: String = ""
    private var mNodeTitle: String = ""
    private var mMinutes: Int = DEFAULT_MINUTES
}