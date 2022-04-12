package org.kman.clearview.core

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job

open class BasicItemsViewModel(app: Application) : BaseViewModel(app) {
    private val _itemGet: MutableLiveData<RsItemGet> = MutableLiveData()
    val itemGet: LiveData<RsItemGet> = _itemGet

    fun startItemGet(window: RqTimeWindow, nodeId: String, itemSelector: String, series: List<String>): Job {
        return startCall(_itemGet) {
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
