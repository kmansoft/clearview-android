package org.kman.clearview.ui.index

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Job
import org.json.JSONObject
import org.kman.clearview.core.BaseViewModel
import org.kman.clearview.core.RqTimeWindow
import org.kman.clearview.core.RsNodeList
import java.io.IOException

class NodeListViewModel(app: Application) : BaseViewModel(app) {

    private val _dataNodeList: MutableLiveData<RsNodeList> = MutableLiveData()
    val dataNodeList: LiveData<RsNodeList> = _dataNodeList

    fun startClientIndex(window: RqTimeWindow, verb: String, args: JSONObject?): Job {
        return startCall(_dataNodeList) {
            val url = makeUrlBuilderBase(app)
                .addPathSegment(verb)
                .build()

            val requestJson = args ?: JSONObject()
            requestJson.put("point_count", window.pointCount)
            requestJson.put("point_duration", window.pointDuration)

            val responseString = makeCallSyncImpl(app, url, requestJson.toString())

            val adapter = mMoshi.adapter(RsNodeList::class.java)

            val obj = adapter.fromJson(responseString)
            if (obj != null) {
                return@startCall obj
            }

            throw IOException("Error making index request to server")
        }
    }

    companion object {
        val TAG = "NodeListViewModel"
    }
}