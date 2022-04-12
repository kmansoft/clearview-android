package org.kman.clearview.core

import android.app.Application
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job

open class BasicAppViewModel(app: Application) : BaseViewModel(app) {
    val data: MutableLiveData<RsApp> = MutableLiveData()

    fun startData(window: RqTimeWindow, verb: String, nodeId: String): Job {
        return startCall(data) {
            val url = makeUrlBuilderBase(app)
                .addPathSegment(verb)
                .build()

            val requestObj = RqApp(nodeId, 0L, window.pointCount, window.pointDuration)

            makeCallSyncReified(app, url, requestObj)
        }
    }

    companion object {
        val TAG = "OverviewViewModel"
    }
}
