package org.kman.clearview.ui.pgsql

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

class PgSqlFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_app_pgsql
    }

    override fun getTitleId(): Int {
        return R.string.menu_app_pgsql
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

        val root = inflater.inflate(R.layout.fragment_pgsql, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartQueries = root.findViewById(R.id.time_chart_pgsql_queries)
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

        mChartScans = root.findViewById(R.id.time_chart_pgsql_scans)
        mChartScans.setController(mController)
        mChartScans.setTitle(R.string.chart_title_scans)
        mChartScans.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "indexed", fill = 0xA5D6A7, line = 0x4CAF50),
                TimeChartView.DataOption(name = "sequential", fill = 0x81C784, line = 0x4CAF50)
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
                data.findSeries("app_pgsql:rows_select"),
                data.findSeries("app_pgsql:rows_insert"),
                data.findSeries("app_pgsql:rows_update"),
                data.findSeries("app_pgsql:rows_delete")
            )
        )

        mChartScans.setData(
            arrayOf(
                data.findSeries("app_pgsql:idx_scan"),
                data.findSeries("app_pgsql:seq_scan")
            )
        )
    }

    private val mModel: PgSqlViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private lateinit var mChartQueries: TimeChartView
    private lateinit var mChartScans: TimeChartView
}
