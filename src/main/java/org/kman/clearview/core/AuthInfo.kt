package org.kman.clearview.core

import android.content.Context
import androidx.preference.PreferenceManager

data class AuthInfo(
    val server: String,
    val username: String?,
    val password: String?,
    val plainHttp: Boolean
) {
    companion object {
        private const val PREFS_KEY_SERVER = "auth_server"
        private const val PREFS_KEY_USERNAME = "auth_username"
        private const val PREFS_KEY_PASSWORD = "auth_password"
        private const val PREFS_KEY_PLAIN_HTTP = "auth_plain_http"

        fun loadSavedAuthInfo(context: Context): AuthInfo? {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.let {
                val server = it.getString(PREFS_KEY_SERVER, null)
                val username = it.getString(PREFS_KEY_USERNAME, null)
                val password = it.getString(PREFS_KEY_PASSWORD, null)
                val plainHttp = it.getBoolean(PREFS_KEY_PLAIN_HTTP, false)

                if (server.isNullOrEmpty()) {
                    null
                } else {
                    AuthInfo(server, username, password, plainHttp)
                }
            }
        }

        fun clearSavedAuthInfo(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().also {
                it.remove(PREFS_KEY_SERVER)
                it.remove(PREFS_KEY_USERNAME)
                it.remove(PREFS_KEY_PASSWORD)
                it.remove(PREFS_KEY_PLAIN_HTTP)
            }.apply()
        }

        fun saveAuthInfo(context: Context, authInfo: AuthInfo) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().also {
                it.putString(PREFS_KEY_SERVER, authInfo.server)
                it.putString(PREFS_KEY_USERNAME, authInfo.username)
                it.putString(PREFS_KEY_PASSWORD, authInfo.password)
                it.putBoolean(PREFS_KEY_PLAIN_HTTP, authInfo.plainHttp)
            }.apply()
        }
    }
}

