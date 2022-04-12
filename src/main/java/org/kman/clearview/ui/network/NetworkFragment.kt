package org.kman.clearview.ui.network

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
import org.kman.clearview.core.RsNodeData
import org.kman.clearview.util.FormatHumanDataSizePerSecond

class NetworkFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_network
    }

    override fun getTitleId(): Int {
        return R.string.menu_network
    }

    override fun refresh() {
        val window = buildTimeWindow()
        val nodeId = getNodeId()
        mModel.startData(window, nodeId, listOf("net"))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mModel.data.observe(viewLifecycleOwner) {
            onData(it)
        }

        val root = inflater.inflate(R.layout.fragment_network, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartTotals = root.findViewById(R.id.time_chart_net_totals)
        mChartTotals.setController(mController)
        mChartTotals.setTitle(R.string.chart_title_net)
        mChartTotals.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "rx", fill = 0xA5D6A7, line = 0x4CAF50),
                TimeChartView.DataOption(name = "tx", fill = 0x81C784, line = 0x4CAF50)
            )
        )
        mChartTotals.setValueFormatter(FormatHumanDataSizePerSecond())

        mChartIPv4 = root.findViewById(R.id.time_chart_net_ipv4)
        mChartIPv4.setController(mController)
        mChartIPv4.setTitle(R.string.chart_title_ipv4)
        mChartIPv4.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "rx 4", fill = 0xBBDEFB, line = 0x2196F3),
                TimeChartView.DataOption(name = "tx 4", fill = 0x64B5F6, line = 0x2196F3)
            )
        )
        mChartIPv4.setValueFormatter(FormatHumanDataSizePerSecond())

        mChartIPv6 = root.findViewById(R.id.time_chart_net_ipv6)
        mChartIPv6.setController(mController)
        mChartIPv6.setTitle(R.string.chart_title_ipv6)
        mChartIPv6.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "rx 6", fill = 0xBBDEFB, line = 0x2196F3),
                TimeChartView.DataOption(name = "tx 6", fill = 0x64B5F6, line = 0x2196F3)
            )
        )
        mChartIPv6.setValueFormatter(FormatHumanDataSizePerSecond())

        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun onData(data: RsNodeData) {
        mChartTotals.setData(
            arrayOf(
                data.findSeries("net:rx"),
                data.findSeries("net:tx")
            )
        )

        mChartIPv4.setData(
            arrayOf(
                data.findSeries("net:rx_4"),
                data.findSeries("net:tx_4")
            )
        )

        mChartIPv6.setData(
            arrayOf(
                data.findSeries("net:rx_6"),
                data.findSeries("net:tx_6")
            )
        )
    }

    private val mModel: NetworkViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private lateinit var mChartTotals: TimeChartView
    private lateinit var mChartIPv4: TimeChartView
    private lateinit var mChartIPv6: TimeChartView
}
