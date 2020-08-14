package org.kman.clearview.core

import android.app.Application
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job

open class BasicDataViewModel(app: Application) : BaseViewModel(app) {
    val data: MutableLiveData<RsNodeData> = MutableLiveData()

    fun startData(window: RqTimeWindow, nodeId: String, series: List<String>): Job {
        return startCall<RsNodeData>({
            val url = makeUrlBuilderBase(app)
                .addPathSegment("get")
                .build()

            val requestObj = RqNodeData(nodeId, series, 0L, window.pointCount, window.pointDuration)

            makeCallSyncReified(app, url, requestObj)
        }, { data -> setData(data) })
    }

    fun setData(d: RsNodeData) {
        data.value = d
    }

    companion object {
        private const val TAG = "OverviewViewModel"
    }
}