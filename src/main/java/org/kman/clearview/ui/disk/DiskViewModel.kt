package org.kman.clearview.ui.disk

import android.app.Application
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import org.kman.clearview.core.*

class DiskViewModel(app: Application) : BasicItemsViewModel(app) {
    val itemList: MutableLiveData<RsDiskList> = MutableLiveData()

    fun setItemList(d: RsDiskList) {
        itemList.value = d
    }

    fun startItemList(window: RqTimeWindow, nodeId: String): Job {
        return startCall<RsDiskList>({
            val url = makeUrlBuilderBase(app)
                .addPathSegment("disk_overview")
                .build()

            val requestObj = RqDiskList(nodeId, window.pointCount, window.pointDuration)

            makeCallSyncReified(app, url, requestObj)
        }, { itemList -> setItemList(itemList) })
    }

    fun startItemGet(window: RqTimeWindow,nodeId: String, item: String): Job {
        return super.startItemGet(window, nodeId, item, listOf("disk_list"))
    }

    fun startItemGet(window: RqTimeWindow,nodeId: String, item: RsDisk): Job {
        return super.startItemGet(window, nodeId, item.name, listOf("disk_list"))
    }
}