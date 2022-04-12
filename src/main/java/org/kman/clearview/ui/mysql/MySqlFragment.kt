package org.kman.clearview.ui.mysql

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

class MySqlFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_app_mysql
    }

    override fun getTitleId(): Int {
        return R.string.menu_app_mysql
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

        val root = inflater.inflate(R.layout.fragment_mysql, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartQueries = root.findViewById(R.id.time_chart_mysql_queries)
        mChartQueries.setController(mController)
        mChartQueries.setTitle(R.string.chart_title_queries)
        mChartQueries.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "select", fill = 0xBBDEFB, line = 0x2196F3),
                TimeChartView.DataOption(name = "insert", fill = 0x90CAF9, line = 0x2196F3),
                TimeChartView.DataOption(name = "update", fill = 0x64B5F6, line = 0x2196F3),
                TimeChartView.DataOption(name = "delete", fill = 0x42A5F5, line = 0x2196F3)
            )
        )

        mChartBytes = root.findViewById(R.id.time_chart_mysql_bytes)
        mChartBytes.setController(mController)
        mChartBytes.setTitle(R.string.chart_title_bytes)
        mChartBytes.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "received", fill = 0xA5D6A7, line = 0x4CAF50),
                TimeChartView.DataOption(name = "sent", fill = 0x81C784, line = 0x4CAF50)
            )
        )

        mChartConnections = root.findViewById(R.id.time_chart_mysql_connections)
        mChartConnections.setController(mController)
        mChartConnections.setTitle(R.string.chart_title_connections)
        mChartConnections.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "conns", fill = 0xB0BEC5, line = 0x607D8B)
            )
        )

        mChartSlow = root.findViewById(R.id.time_chart_mysql_slow)
        mChartSlow.setController(mController)
        mChartSlow.setTitle(R.string.chart_title_slow)
        mChartSlow.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "queries", fill = 0xF48FB1, line = 0xE91E63)
            )
        )

        mChartAborted = root.findViewById(R.id.time_chart_mysql_aborted)
        mChartAborted.setController(mController)
        mChartAborted.setTitle(R.string.chart_title_aborted)
        mChartAborted.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "conns", fill = 0xEF9A9A, line = 0xF44336),
                TimeChartView.DataOption(name = "clients", fill = 0xE57373, line = 0xF44336)
            )
        )

        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun onData(data: RsApp) {
        mChartQueries.setData(
            arrayOf(
                data.findSeries("app_mysql:com_select"),
                data.findSeries("app_mysql:com_insert"),
                data.findSeries("app_mysql:com_update"),
                data.findSeries("app_mysql:com_delete")
            )
        )

        mChartBytes.setData(
            arrayOf(
                data.findSeries("app_mysql:bytes_received"),
                data.findSeries("app_mysql:bytes_sent")
            )
        )

        mChartConnections.setData(
            arrayOf(
                data.findSeries("app_mysql:connections")
            )
        )

        mChartSlow.setData(
            arrayOf(
                data.findSeries("app_mysql:slow_queries")
            )
        )

        mChartAborted.setData(
            arrayOf(
                data.findSeries("app_mysql:aborted_connects"),
                data.findSeries("app_mysql:aborted_clients")
            )
        )
    }

    private val mModel: MySqlViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private lateinit var mChartQueries: TimeChartView
    private lateinit var mChartBytes: TimeChartView
    private lateinit var mChartConnections: TimeChartView
    private lateinit var mChartSlow: TimeChartView
    private lateinit var mChartAborted: TimeChartView
}
