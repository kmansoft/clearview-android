package org.kman.clearview.chart

import android.content.Context

class TimeChartGroupController(
    @Suppress("UNUSED_PARAMETER") context: Context
) :
    TimeChartController {

    override fun register(view: TimeChartView) {
        mList.add(view)
    }

    override fun onShowLegend(source: TimeChartView, index: Int) {
        for (view in mList) {
            view.showLegendView(index)
        }
    }

    override fun onHideLegend(source: TimeChartView) {
        for (view in mList) {
            view.hideLegendView()
        }
    }

    private val mList = mutableListOf<TimeChartView>()
}