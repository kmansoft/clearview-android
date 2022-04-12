package org.kman.clearview.ui.index

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Job
import org.json.JSONObject
import org.kman.clearview.core.BaseViewModel
import org.kman.clearview.core.RqTimeWindow
import org.kman.clearview.core.RsNodeList
import java.io.IOException

class NodeListViewModel(app: Application) : BaseViewModel(app) {

    val dataNodeList: MutableLiveData<RsNodeList> = MutableLiveData()

    fun startClientIndex(window: RqTimeWindow, verb: String, args: JSONObject?): Job {
        return startCall({
            val url = makeUrlBuilderBase(app)
                .addPathSegment(verb)
                .build()

            val requestJson = args ?: JSONObject()
            requestJson.put("point_count", window.pointCount)
            requestJson.put("point_duration", window.pointDuration)

            val responseString = makeCallSyncImpl(app, url, requestJson.toString())

            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(RsNodeList::class.java)

            val obj = adapter.fromJson(responseString)
            if (obj != null) {
                return@startCall obj
            }

            throw IOException("Error making index request to server")
        }, { data -> setDataNodeList(data) })
    }

    fun setDataNodeList(data: RsNodeList) {
        dataNodeList.value = data
    }

    companion object {
        val TAG = "NodeListViewModel"
    }
}