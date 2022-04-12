package org.kman.clearview.ui.apache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.kman.clearview.R
import org.kman.clearview.chart.TimeChartGroupController
import org.kman.clearview.chart.TimeChartView
import org.kman.clearview.core.BaseDetailFragment
import org.kman.clearview.core.RsApp
import org.kman.clearview.util.FormatHumanDataSizePerSecond

class ApacheFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_app_apache
    }

    override fun getTitleId(): Int {
        return R.string.menu_app_apache
    }

    override fun refresh() {
        val window = buildTimeWindow()
        val nodeId = getNodeId()
        mModel.startData(window, nodeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mModel.data.observe(viewLifecycleOwner) {
            onData(it)
        }

        val root = inflater.inflate(R.layout.fragment_apache, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartRequests = root.findViewById(R.id.time_chart_apache_requests)
        mChartRequests.setController(mController)
        mChartRequests.setTitle(R.string.chart_title_requests)
        mChartRequests.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "requests", fill = 0xE6EE9C, line = 0xCDDC39)
            )
        )

        mChartBytes = root.findViewById(R.id.time_chart_apache_bytes)
        mChartBytes.setController(mController)
        mChartBytes.setTitle(R.string.chart_title_bytes)
        mChartBytes.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "bytes", fill = 0xA5D6A7, line = 0x4CAF50)
            )
        )
        mChartBytes.setValueFormatter(FormatHumanDataSizePerSecond())

        mChartWorkers = root.findViewById(R.id.time_chart_apache_workers)
        mChartWorkers.setController(mController)
        mChartWorkers.setTitle(R.string.chart_title_workers)
        mChartWorkers.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "waiting", fill = 0xD1C4E9, line = 0x9C27B0),
                TimeChartView.DataOption(name = "reading", fill = 0xCE93D8, line = 0x9C27B0),
                TimeChartView.DataOption(name = "writing", fill = 0xBA68C8, line = 0x9C27B0)
            )
        )

        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun onData(data: RsApp) {
        mChartRequests.setData(
            arrayOf(
                data.findSeries("app_apache:access")
            )
        )

        mChartBytes.setData(
            arrayOf(
                data.findSeries("app_apache:bytes")
            )
        )

        mChartWorkers.setData(
            arrayOf(
                data.findSeries("app_apache:workers_waiting"),
                data.findSeries("app_apache:workers_reading"),
                data.findSeries("app_apache:workers_writing")
            )
        )
    }

    private val mModel: ApacheViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private lateinit var mChartRequests: TimeChartView
    private lateinit var mChartBytes: TimeChartView
    private lateinit var mChartWorkers: TimeChartView
}
