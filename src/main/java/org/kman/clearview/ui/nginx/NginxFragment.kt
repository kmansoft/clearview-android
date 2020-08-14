package org.kman.clearview.ui.nginx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.kman.clearview.R
import org.kman.clearview.chart.TimeChartGroupController
import org.kman.clearview.chart.TimeChartView
import org.kman.clearview.core.BaseDetailFragment
import org.kman.clearview.core.RsApp

class NginxFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_app_nginx
    }

    override fun getTitleId(): Int {
        return R.string.menu_app_nginx
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
        mModel =
            ViewModelProvider(this).get(NginxViewModel::class.java)
        mModel.data.observe(viewLifecycleOwner, Observer {
            onData(it)
        })

        val root = inflater.inflate(R.layout.fragment_nginx, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartRequests = root.findViewById(R.id.time_chart_nginx_requests)
        mChartRequests.setController(mController)
        mChartRequests.setTitle(R.string.chart_title_requests)
        mChartRequests.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "requests", fill = 0xE6EE9C, line = 0xCDDC39)
            )
        )

        mChartConnections = root.findViewById(R.id.time_chart_nginx_connections)
        mChartConnections.setController(mController)
        mChartConnections.setTitle(R.string.chart_title_connections)
        mChartConnections.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "accepted", fill = 0xB0BEC5, line = 0x607D8B),
                TimeChartView.DataOption(name = "handled", fill = 0x90A4AE, line = 0x607D8B)
            )
        )

        mChartWorkers = root.findViewById(R.id.time_chart_nginx_workers)
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

    private fun onData(data: RsApp) {
        mChartRequests.setData(
            arrayOf(
                data.findSeries("app_nginx:access")
            )
        )

        mChartConnections.setData(
            arrayOf(
                data.findSeries("app_nginx:accepted"),
                data.findSeries("app_nginx:handled")
            )
        )

        mChartWorkers.setData(
            arrayOf(
                data.findSeries("app_nginx:workers_waiting"),
                data.findSeries("app_nginx:workers_reading"),
                data.findSeries("app_nginx:workers_writing")
            )
        )
    }

    private lateinit var mModel: NginxViewModel

    private lateinit var mController: TimeChartGroupController

    private lateinit var mChartRequests: TimeChartView
    private lateinit var mChartConnections: TimeChartView
    private lateinit var mChartWorkers: TimeChartView
}
