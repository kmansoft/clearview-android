package org.kman.clearview.core

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kman.clearview.BuildConfig
import org.kman.clearview.util.MyLog
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


open class BaseViewModel(val app: Application) : AndroidViewModel(app) {
    fun makeUrlBuilderBase(context: Context): HttpUrl.Builder {
        val authInfo = AuthInfo.loadSavedAuthInfo(context)
            ?: throw IllegalStateException("Please log in first")
        return makeUrlBuilderBase(authInfo)
    }

    fun makeUrlBuilderBase(authInfo: AuthInfo): HttpUrl.Builder {
        return HttpUrl.Builder().also {
            it.scheme("https")
            it.host(authInfo.server)

            if (BuildConfig.DEBUG) {
                if (authInfo.server.startsWith("192.")) {
                    it.scheme("http")
                    it.port(63001)
                }
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
                val combined = "$username:$password"
                val encoded = Base64.encodeToString(combined.toByteArray(), Base64.NO_WRAP)
                addHeader("Authorization", "Basic $encoded")
            }
        }
    }

    protected fun <RS> startCall(callFunc: () -> RS, setFunc: (data: RS) -> Unit): Job {
        // FIXME - how can we let the client clear its job ref to null?
        return GlobalScope.launch(SupervisorJob() + Dispatchers.Main) {
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
        if (responseString != null) {
            val responseObj = responseAdapter.fromJson(responseString)
            if (responseObj != null) {
                return responseObj
            }
        }

        throw IOException("Error (1) making data request to server")
    }

    protected fun makeCallSyncImpl(context: Context, url: HttpUrl, requestString: String): String? {
        val authInfo = AuthInfo.loadSavedAuthInfo(context)
            ?: throw IllegalStateException("Auth info is null, need to log in first")
        val requestBody = requestString.toRequestBody(ServerData.JSON)
        val request = makeRequestBuilderBaseWithAuth(authInfo, url)
            .post(requestBody)
            .build()

        return makeCallSyncImpl(request)
    }

    protected fun makeCallSyncImpl(request: Request): String? {
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
