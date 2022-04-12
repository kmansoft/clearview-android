package org.kman.clearview.ui.overview

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
import org.kman.clearview.core.rebuildDataSeries
import org.kman.clearview.util.FormatHumanDataSize
import org.kman.clearview.util.FormatHumanDataSizePerSecond

class OverviewFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_overview
    }

    override fun getTitleId(): Int {
        return R.string.menu_overview
    }

    override fun refresh() {
        val window = buildTimeWindow()
        val nodeId = getNodeId()
        mModel.startData(window, nodeId, listOf("cpu", "mem", "net", "disk"))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mModel.data.observe(viewLifecycleOwner) {
            onData(it)
        }

        val root = inflater.inflate(R.layout.fragment_overview, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartCpu = root.findViewById(R.id.time_chart_overview_cpu)
        mChartCpu.setController(mController)
        mChartCpu.setTitle(R.string.chart_title_cpu)
        mChartCpu.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "wait", fill = 0xBBDEFB, line = 0x2196F3),
                TimeChartView.DataOption(name = "user", fill = 0x90CAF9, line = 0x2196F3),
                TimeChartView.DataOption(name = "system", fill = 0x64B5F6, line = 0x2196F3)
            )
        )

        mChartLoad = root.findViewById(R.id.time_chart_overview_load)
        mChartLoad.setController(mController)
        mChartLoad.setTitle(R.string.chart_title_load)
        mChartLoad.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "load avg", fill = 0xFFF176, line = 0xFFEB3B)
            )
        )

        mChartMem = root.findViewById(R.id.time_chart_overview_mem)
        mChartMem.setController(mController)
        mChartMem.setTitle(R.string.chart_title_mem)
        mChartMem.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "used", fill = 0xE1BEE7, line = 0x9C27B0),
                TimeChartView.DataOption(name = "cache", fill = 0xCE93D8, line = 0x9C27B0),
                TimeChartView.DataOption(name = "buffers", fill = 0xBA68C8, line = 0x9C27B0),
                TimeChartView.DataOption(name = "swap", fill = 0xE57373, line = 0xF44336)
            )
        )
        mChartMem.setValueFormatter(FormatHumanDataSize())

        mChartNet = root.findViewById(R.id.time_chart_overview_net)
        mChartNet.setController(mController)
        mChartNet.setTitle(R.string.chart_title_net)
        mChartNet.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "rx", fill = 0xA5D6A7, line = 0x4CAF50),
                TimeChartView.DataOption(name = "tx", fill = 0x81C784, line = 0x4CAF50)
            )
        )
        mChartNet.setValueFormatter(FormatHumanDataSizePerSecond())

        mChartDisk = root.findViewById(R.id.time_chart_overview_disk)
        mChartDisk.setController(mController)
        mChartDisk.setTitle(R.string.chart_title_disk)
        mChartDisk.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "read ops", fill = 0xFFCC80, line = 0xFF9800),
                TimeChartView.DataOption(name = "write ops", fill = 0xFFB74D, line = 0xFF9800)
            )
        )

        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun onData(data: RsNodeData) {
        mChartCpu.setData(
            arrayOf(
                data.findSeries("cpu:wait"),
                data.findSeries("cpu:user"),
                data.findSeries("cpu:system")
            )
        )

        mChartLoad.setData(
            arrayOf(
                data.findSeries("cpu:load_avg")
            )
        )

        val seriesMemUsedSource = data.findSeries("mem:real_used")
        val seriesMemCache = data.findSeries("mem:real_cache")
        val seriesMemBuffers = data.findSeries("mem:real_buffers")
        val seriesMemUsed = rebuildDataSeries(seriesMemUsedSource) {
            seriesMemUsedSource.points[it].v -
                    (seriesMemCache.points[it].v + seriesMemBuffers.points[it].v)
        }
        mChartMem.setData(
            arrayOf(
                seriesMemUsed,
                data.findSeries("mem:real_cache"),
                data.findSeries("mem:real_buffers"),
                data.findSeries("mem:swap_used")
            )
        )

        mChartNet.setData(
            arrayOf(
                data.findSeries("net:rx"),
                data.findSeries("net:tx")
            )
        )

        mChartDisk.setData(
            arrayOf(
                data.findSeries("disk:combined_read_count"),
                data.findSeries("disk:combined_write_count")
            )
        )
    }

    private val mModel: OverviewViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private lateinit var mChartCpu: TimeChartView
    private lateinit var mChartLoad: TimeChartView
    private lateinit var mChartMem: TimeChartView
    private lateinit var mChartNet: TimeChartView
    private lateinit var mChartDisk: TimeChartView
}
