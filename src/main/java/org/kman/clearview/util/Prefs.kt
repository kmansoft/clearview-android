package org.kman.clearview.util

import android.content.Context
import androidx.preference.PreferenceManager

class Prefs(context: Context) {
    companion object {
        private const val KEY_SHOW_APACHE = "ShowApache"
        private const val KEY_SHOW_NGINX = "ShowNginx"
        private const val KEY_SHOW_MYSQL = "ShowMySQL"
        private const val KEY_SHOW_PGSQL = "ShowPgSQL"
    }

    val mShowApache: Boolean
    val mShowNginx: Boolean
    val mShowMySQL: Boolean
    val mShowPgSQL: Boolean

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        mShowApache = prefs.getBoolean(KEY_SHOW_APACHE, true)
        mShowNginx = prefs.getBoolean(KEY_SHOW_NGINX, true)
        mShowMySQL = prefs.getBoolean(KEY_SHOW_MYSQL, true)
        mShowPgSQL = prefs.getBoolean(KEY_SHOW_PGSQL, true)
    }
}