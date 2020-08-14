package org.kman.clearview.ui.apache

import android.app.Application
import kotlinx.coroutines.Job
import org.kman.clearview.core.BasicAppViewModel
import org.kman.clearview.core.RqTimeWindow

class ApacheViewModel(app: Application) : BasicAppViewModel(app) {
    fun startData(window: RqTimeWindow, nodeId: String): Job {
        return super.startData(window, "app_apache", nodeId)
    }
}