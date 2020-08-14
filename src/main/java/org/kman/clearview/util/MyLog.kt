package org.kman.clearview.util

import android.util.Log

object MyLog {
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun i(tag: String, msg: String, vararg obj: Any?) {
        Log.i(tag, String.format(msg, *obj))
    }

    fun w(tag: String, msg: String, x: Throwable) {
        Log.w(tag, msg, x)
    }
}