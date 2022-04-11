package org.kman.clearview.core

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.kman.clearview.util.MyLog
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit


open class BaseViewModel(val app: Application) : AndroidViewModel(app) {
    fun makeUrlBuilderBase(context: Context): HttpUrl.Builder {
        val authInfo = AuthInfo.loadSavedAuthInfo(context)
            ?: throw IllegalStateException("Please log in first")
        return makeUrlBuilderBase(authInfo)
    }

    fun makeUrlBuilderBase(authInfo: AuthInfo): HttpUrl.Builder {
        return HttpUrl.Builder().also {
            it.scheme(
                if (authInfo.plainHttp) "http" else "https")

            // Split server:port if present

            var server = authInfo.server
            var port = 0

            val i = authInfo.server.indexOf(':')
            if (i > 0) {
                server = authInfo.server.substring(0, i)
                port = try {
                    authInfo.server.substring(i+1).toInt()
                } catch (x: Exception) {
                    0
                }
            }

            it.host(server)
            if (port in 1..65535) {
                it.port(port)
            }

            it.addPathSegments("cv/api/v1")
        }
    }

    fun makeRequestBuilderBaseWithAuth(authInfo: AuthInfo, url: HttpUrl): Request.Builder {
        return Request.Builder().apply {
            url(url)
            val username = authInfo.username
            val password = authInfo.password
            if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                val encoded = Credentials.basic(username, password)
                addHeader("Authorization", encoded)
            }
        }
    }

    protected fun <RS> startCall(callFunc: () -> RS, setFunc: (data: RS) -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main) {
            try {
                startCallImpl(callFunc, setFunc)
            } catch (x: CancellationException) {
                // Ignore
                MyLog.w(TAG, "Cancelled", x)
            } catch (x: Throwable) {
                MyLog.w(TAG, "Top level catch", x)
            }
        }
    }

    private fun getAuthInfo(): AuthInfo {
        return AuthInfo.loadSavedAuthInfo(app)
            ?: throw IllegalStateException("Auth info is null, need to log in first")
    }

    private suspend fun <RS> startCallImpl(callFunc: () -> RS, setFunc: (data: RS) -> Unit) {

        try {
            val data: RS = withContext(Dispatchers.IO) {
                callFunc()
            }
            setFunc(data)
        } catch (x: CancellationException) {
            // Ignore
            MyLog.w(TAG, "Cancelled", x)
        } catch (x: Throwable) {
            MyLog.w(TAG, "Top level catch", x)
        }
    }

    protected inline fun <reified RQ, reified RS> makeCallSyncReified(
        context: Context,
        url: HttpUrl,
        requestObj: RQ
    ): RS {
        val moshi = Moshi.Builder().build()

        val requestAdapter = moshi.adapter(RQ::class.java)
        val responseAdapter = moshi.adapter(RS::class.java)

        val requestString = requestAdapter.toJson(requestObj)
        val responseString = makeCallSyncImpl(context, url, requestString)

        val responseObj = responseAdapter.fromJson(responseString)
        if (responseObj != null) {
            return responseObj
        }

        throw IOException("Error (1) making data request to server")
    }

    protected fun makeCallSyncImpl(context: Context, url: HttpUrl, requestString: String): String {
        val authInfo = AuthInfo.loadSavedAuthInfo(context)
            ?: throw IllegalStateException("Auth info is null, need to log in first")
        val requestBody = requestString.toRequestBody(ServerData.JSON)
        val request = makeRequestBuilderBaseWithAuth(authInfo, url)
            .post(requestBody)
            .build()

        return makeCallSyncImpl(request)
    }

    protected fun makeCallSyncImpl(request: Request): String {
        val response = mHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body
            responseBody?.also {
                return it.string()
            }
        } else if (response.code == 401) {
            throw AuthException()
        }

        throw IOException("Error making data request to server: ${response.code}")
    }

    companion object {
        val TAG = "BaseViewModel"

        val mHttpClient = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .cache(null)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .build()
    }
}
