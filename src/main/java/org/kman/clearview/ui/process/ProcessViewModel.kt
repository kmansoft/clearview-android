package org.kman.clearview.ui.process

import android.app.Application
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import org.kman.clearview.core.*

class ProcessViewModel(app: Application) : BasicItemsViewModel(app) {
    val itemList: MutableLiveData<RsProcessList> = MutableLiveData()

    fun setItemList(d: RsProcessList) {
        itemList.value = d
    }

    fun startItemList(window: RqTimeWindow, nodeId: String): Job {
        return startCall<RsProcessList>({
            val url = makeUrlBuilderBase(app)
                .addPathSegment("process_overview")
                .build()

            val requestObj = RqProcessList(nodeId, window.pointCount, window.pointDuration)

            makeCallSyncReified(app, url, requestObj)
        }, { itemList -> setItemList(itemList) })
    }

    fun startItemGet(window: RqTimeWindow, nodeId: String, item: String): Job {
        return super.startItemGet(window, nodeId, item, listOf("process_list"))
    }

    fun startItemGet(window: RqTimeWindow, nodeId: String, item: RsProcess): Job {
        return super.startItemGet(window, nodeId, "${item.name}|${item.user}", listOf("process_list"))
    }
}