package org.kman.clearview.core

import android.content.Context
import androidx.preference.PreferenceManager

data class AuthInfo(
    val server: String,
    val username: String?,
    val password: String?
) {
    companion object {
        private const val PREFS_KEY_SERVER = "auth_server"
        private const val PREFS_KEY_USERNAME = "auth_username"
        private const val PREFS_KEY_PASSWORD = "auth_password"

        fun loadSavedAuthInfo(context: Context): AuthInfo? {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.let {
                val server = it.getString(PREFS_KEY_SERVER, null)
                val username = it.getString(PREFS_KEY_USERNAME, null)
                val password = it.getString(PREFS_KEY_PASSWORD, null)

                if (server.isNullOrEmpty()) {
                    null
                } else {
                    AuthInfo(server, username, password)
                }
            }
        }

        fun clearSavedAuthInfo(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().also {
                it.remove(PREFS_KEY_SERVER)
                it.remove(PREFS_KEY_USERNAME)
                it.remove(PREFS_KEY_PASSWORD)
            }.apply()
        }

        fun saveAuthInfo(context: Context, authInfo: AuthInfo) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().also {
                it.putString(PREFS_KEY_SERVER, authInfo.server)
                it.putString(PREFS_KEY_USERNAME, authInfo.username)
                it.putString(PREFS_KEY_PASSWORD, authInfo.password)
            }.apply()
        }
    }
}

