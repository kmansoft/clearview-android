package org.kman.clearview.ui.system

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import org.kman.clearview.core.BaseViewModel
import org.kman.clearview.core.RqSystem
import org.kman.clearview.core.RqTimeWindow
import org.kman.clearview.core.RsSystem

class SystemViewModel(app: Application) : BaseViewModel(app) {
    private val _data: MutableLiveData<RsSystem> = MutableLiveData()
    val data: LiveData<RsSystem> = _data

    fun startData(window: RqTimeWindow, nodeId: String): Job {
        return startCall(_data) {
            val url = makeUrlBuilderBase(app)
                .addPathSegment("system_overview")
                .build()

            val requestObj = RqSystem(nodeId, 0L, window.pointCount, window.pointDuration)

            makeCallSync(app, url, requestObj)
        }
    }

    companion object {
        val TAG = "OverviewViewModel"
    }
}