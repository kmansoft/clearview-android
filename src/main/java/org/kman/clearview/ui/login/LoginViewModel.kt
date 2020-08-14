package org.kman.clearview.ui.login

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.kman.clearview.R
import org.kman.clearview.core.AuthException
import org.kman.clearview.core.AuthInfo
import org.kman.clearview.core.BaseViewModel
import org.kman.clearview.core.ServerData
import org.kman.clearview.util.MyLog
import java.io.IOException

class LoginViewModel(app: Application) : BaseViewModel(app) {

    val progress = MutableLiveData<Boolean>(false)
    val auth = MutableLiveData<AuthInfo?>()
    val error = MutableLiveData<String?>()

    fun startAuth(authInfo: AuthInfo): Job {
        // FIXME - how can we let the client clear its job ref to null?
        return GlobalScope.launch(SupervisorJob() + Dispatchers.Main) {
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

        auth.value = null
        error.value = null

        progress.value = true
        val start = SystemClock.elapsedRealtime()
        var isError = false
        try {
            val authResult = withContext(Dispatchers.IO) {
                makeAuthCallSync(authInfo)
            }

            auth.value = authResult
            saveAuthInfo(app, authResult)
        } catch (x: AuthException) {
            MyLog.w(TAG, "Auth error", x)
            isError = true
            error.value = app.getString(R.string.connect_login_error)
        } catch (x: Throwable) {
            MyLog.w(TAG, "Throwable", x)
            isError = true
            error.value = app.getString(R.string.connect_network_error, x.message)
        } finally {
            if (!isError) {
                val elapsed = SystemClock.elapsedRealtime() - start
                val d = 500 - elapsed
                if (d > 0) {
                    delay(d)
                }
            }

            progress.value = false
        }
    }

    private fun makeAuthCallSync(authInfo: AuthInfo): AuthInfo {
        val url = makeUrlBuilderBase(authInfo)
            .addPathSegments("index")
            .build()

        val json = JSONObject()
        val requestBody = json.toString().toRequestBody(ServerData.JSON)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val responseString = makeCallSyncImpl(request)
        if (responseString != null) {
            MyLog.i(TAG, "Server response: %s", responseString)

            @Suppress("UNUSED_VARIABLE")
            val responseObj = JSONObject(responseString)

            return authInfo
        }

        throw IOException("Error making auth request to server")
    }

    companion object {
        private const val TAG = "LoginViewModel"

        fun saveAuthInfo(context: Context, authInfo: AuthInfo) {
            AuthInfo.saveAuthInfo(context, authInfo)
        }
    }
}