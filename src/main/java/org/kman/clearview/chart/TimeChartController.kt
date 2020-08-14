package org.kman.clearview.chart

interface TimeChartController {
    fun register(view: TimeChartView)

    fun onShowLegend(source: TimeChartView, index: Int)

    fun onHideLegend(source: TimeChartView)
}