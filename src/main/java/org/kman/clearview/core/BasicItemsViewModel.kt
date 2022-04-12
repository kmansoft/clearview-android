package org.kman.clearview.core

import android.app.Application
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job

open class BasicItemsViewModel(app: Application) : BaseViewModel(app) {
    val itemGet: MutableLiveData<RsItemGet> = MutableLiveData()

    fun startItemGet(window: RqTimeWindow, nodeId: String, itemSelector: String, series: List<String>): Job {
        return startCall(itemGet) {
            val url = makeUrlBuilderBase(app)
                .addPathSegment("get")
                .build()

            val requestObj =
                RqItemGet(nodeId, itemSelector, series, 0L, window.pointCount, window.pointDuration)

            makeCallSyncReified(app, url, requestObj)
        }
    }

    companion object {
        val TAG = "OverviewViewModel"
    }
}
