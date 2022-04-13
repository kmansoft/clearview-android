package org.kman.clearview.ui.login

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.kman.clearview.R
import org.kman.clearview.core.AuthException
import org.kman.clearview.core.AuthInfo
import org.kman.clearview.core.BaseViewModel
import org.kman.clearview.core.ServerData
import org.kman.clearview.util.MyLog

class LoginViewModel(app: Application) : BaseViewModel(app) {

    private val _progress = MutableLiveData<Boolean>(false)
    private val _auth = MutableLiveData<AuthInfo?>()
    private val _error = MutableLiveData<String?>()

    val progress: LiveData<Boolean> = _progress
    val auth: LiveData<AuthInfo?> = _auth
    val error: LiveData<String?> = _error

    fun startAuth(authInfo: AuthInfo): Job {
        return viewModelScope.launch(Dispatchers.Main) {
            try {
                startAuthFromAuthInfo(authInfo)
            } catch (x: CancellationException) {
                // Ignore
                MyLog.w(TAG, "Cancelled", x)
            } catch (x: Throwable) {
                // Ignore, already reported
            }
        }
    }

    private suspend fun startAuthFromAuthInfo(authInfo: AuthInfo) {

        _auth.value = null
        _error.value = null

        _progress.value = true
        val start = SystemClock.elapsedRealtime()
        try {
            val authResult = withContext(Dispatchers.IO) {
                makeAuthCallSync(authInfo)
            }

            _auth.value = authResult
            AuthInfo.saveAuthInfo(app, authResult)
        } catch (x: AuthException) {
            MyLog.w(TAG, "Auth error", x)
            _error.value = app.getString(R.string.connect_login_error)
        } catch (x: Throwable) {
            MyLog.w(TAG, "Throwable", x)
            _error.value = app.getString(R.string.connect_network_error, x.message)
        } finally {
            if (error.value == null) {
                val elapsed = SystemClock.elapsedRealtime() - start
                val d = 500 - elapsed
                if (d > 0) {
                    delay(d)
                }
            }

            _progress.value = false
        }
    }

    private fun makeAuthCallSync(authInfo: AuthInfo): AuthInfo {
        val url = makeUrlBuilderBase(authInfo)
            .addPathSegments("index")
            .build()

        val json = JSONObject()
        val requestBody = json.toString().toRequestBody(ServerData.JSON)
        val request = makeRequestBuilderBaseWithAuth(authInfo, url)
            .post(requestBody)
            .build()

        val responseString = makeCallSyncImpl(request)
        MyLog.i(TAG, "Server response: %s", responseString)

        @Suppress("UNUSED_VARIABLE")
        val responseObj = JSONObject(responseString)

        return authInfo
    }

    companion object {
        private const val TAG = "LoginViewModel"
    }
}
