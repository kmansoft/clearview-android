package org.kman.clearview.ui.system

import android.app.Application
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import org.kman.clearview.core.BaseViewModel
import org.kman.clearview.core.RqSystem
import org.kman.clearview.core.RqTimeWindow
import org.kman.clearview.core.RsSystem

class SystemViewModel(app: Application) : BaseViewModel(app) {
    val data: MutableLiveData<RsSystem> = MutableLiveData()

    fun startData(window: RqTimeWindow, nodeId: String): Job {
        return startCall<RsSystem>({
            val url = makeUrlBuilderBase(app)
                .addPathSegment("system_overview")
                .build()

            val requestObj = RqSystem(nodeId, 0L, window.pointCount, window.pointDuration)

            makeCallSyncReified(app, url, requestObj)
        }, { data -> setData(data) })
    }

    fun setData(d: RsSystem) {
        data.value = d
    }

    companion object {
        val TAG = "OverviewViewModel"
    }
}