package org.kman.clearview.core

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job

open class BasicDataViewModel(app: Application) : BaseViewModel(app) {
    private val _data: MutableLiveData<RsNodeData> = MutableLiveData()
    val data: LiveData<RsNodeData> = _data

    fun startData(window: RqTimeWindow, nodeId: String, series: List<String>): Job {
        return startCall(_data) {
            val url = makeUrlBuilderBase(app)
                .addPathSegment("get")
                .build()

            val requestObj = RqNodeData(nodeId, series, 0L, window.pointCount, window.pointDuration)

            makeCallSync(app, url, requestObj)
        }
    }

    companion object {
        private const val TAG = "OverviewViewModel"
    }
}