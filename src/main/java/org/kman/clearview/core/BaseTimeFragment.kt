package org.kman.clearview.core

import androidx.fragment.app.Fragment

abstract class BaseTimeFragment() :  Fragment() {
    abstract fun setTimeMinutes(minutes: Int)
    abstract fun refresh()

    companion object {
        val DEFAULT_MINUTES = 30
    }
}