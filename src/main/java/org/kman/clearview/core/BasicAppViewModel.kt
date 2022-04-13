package org.kman.clearview.core

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job

open class BasicAppViewModel(app: Application) : BaseViewModel(app) {
    private val _data: MutableLiveData<RsApp> = MutableLiveData()
    val data: LiveData<RsApp> = _data

    fun startData(window: RqTimeWindow, verb: String, nodeId: String): Job {
        return startCall(_data) {
            val url = makeUrlBuilderBase(app)
                .addPathSegment(verb)
                .build()

            val requestObj = RqApp(nodeId, 0L, window.pointCount, window.pointDuration)

            makeCallSync(app, url, requestObj)
        }
    }

    companion object {
        val TAG = "OverviewViewModel"
    }
}
