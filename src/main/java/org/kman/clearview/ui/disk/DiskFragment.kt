package org.kman.clearview.ui.disk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.kman.clearview.R
import org.kman.clearview.chart.TimeChartGroupController
import org.kman.clearview.chart.TimeChartView
import org.kman.clearview.core.*
import org.kman.clearview.util.FormatHumanDataSize
import java.util.*
import kotlin.collections.ArrayList

class DiskFragment : BaseDetailFragment(), AdapterView.OnItemSelectedListener {

    override fun getNavigationId(): Int {
        return R.id.nav_disk
    }

    override fun getTitleId(): Int {
        return R.string.menu_disk
    }

    override fun refresh() {
        val window = buildTimeWindow()
        val nodeId = getNodeId()

        mModel.startItemList(window, nodeId)

        if (mItemSelectorApi != emptyRsDisk()) {
            mModel.startItemGet(window, nodeId, mItemSelectorApi)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mModel.itemList.observe(viewLifecycleOwner) {
            onItemList(it)
        }
        mModel.itemGet.observe(viewLifecycleOwner) {
            onItemGet(it)
        }

        val root = inflater.inflate(R.layout.fragment_disk, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartOps = root.findViewById(R.id.time_chart_disk_ops)
        mChartOps.setController(mController)
        mChartOps.setTitle(R.string.chart_title_disk)
        mChartOps.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "read ops", fill = 0xFFCC80, line = 0xFF9800),
                TimeChartView.DataOption(name = "write ops", fill = 0xFFB74D, line = 0xFF9800)
            )
        )

        mChartSpace = root.findViewById(R.id.time_chart_disk_space)
        mChartSpace.setController(mController)
        mChartSpace.setTitle(R.string.chart_title_space)
        mChartSpace.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "bytes", fill = 0xFFAB91, line = 0xFF5722)
            )
        )
        mChartSpace.setValueFormatter(FormatHumanDataSize())

        mChartINodes = root.findViewById(R.id.time_chart_disk_inodes)
        mChartINodes.setController(mController)
        mChartINodes.setTitle(R.string.chart_title_inodes)
        mChartINodes.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "inodes", fill = 0xBCAAA4, line = 0x795548)
            )
        )

        mDiskListSpinner = root.findViewById(R.id.disk_list_selector_spinner)
        mDiskListSpinner.onItemSelectedListener = this

        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // FIXME - reset the charts
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        mItemSelectorUI = mSpinnerValues[position]
        mItemSelectorApi = mItemListSorted[position]

        val window = buildTimeWindow()
        val nodeId = getNodeId()
        mModel.startItemGet(window, nodeId, mItemSelectorApi)
    }

    private fun onItemList(items: RsDiskList) {
        if (items.disks.isEmpty()) {
            mItemListSorted = emptyList()
            mItemSelectorUI = ""
            mItemSelectorApi = emptyRsDisk()
            mModel.itemGet.value = emptyRsItem()

            mDiskListSpinner.adapter = null
            mSpinnerValues = emptyList()
        } else {
            mItemListSorted = ArrayList(items.disks).sortedWith { o1, o2 -> o1.name.compareTo(o2.name) }

            if (mItemSelectorUI.isEmpty()) {
                val disk0 = mItemListSorted[0]
                mItemSelectorUI = disk0.name
                mItemSelectorApi = disk0
            }

            val window = buildTimeWindow()
            val nodeId = getNodeId()
            mModel.startItemGet(window, nodeId, mItemSelectorApi)

            val context = requireContext()
            val list = List(mItemListSorted.size) { mItemListSorted[it].name }
            if (list != mSpinnerValues) {
                val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, list)
                adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
                mDiskListSpinner.adapter = adapter
                mSpinnerValues = list

                for (i in list.indices) {
                    if (mItemListSorted[i] == mItemSelectorApi) {
                        mDiskListSpinner.setSelection(i)
                        break
                    }
                }
            }
        }
    }

    private fun onItemGet(item: RsItemGet) {
        if (item == emptyRsItem()) {
            val empty = emptyArray<RsDataSeries>()
            mChartOps.setData(empty)
            mChartSpace.setData(empty)
            mChartINodes.setData(empty)
            return
        }

//        val context = activity ?: return
//        mChartOps.setTitle(context.getString(R.string.chart_title_disk) + ": " + item.request.item)

        mChartOps.setData(
            arrayOf(
                item.findSeries("disk_list:read_count"),
                item.findSeries("disk_list:write_count")
            )
        )

        val seriesSpaceTotal = item.findSeries("disk_list:total_bytes")
        val seriesSpaceFree = item.findSeries("disk_list:free_bytes")
        val seriesSpaceUsed = rebuildDataSeries(seriesSpaceTotal) {
            seriesSpaceTotal.points[it].v - seriesSpaceFree.points[it].v
        }
        mChartSpace.setData(
            arrayOf(
                seriesSpaceUsed
            )
        )
        mChartSpace.setMaxValue(seriesSpaceTotal.maxValue(128 * 1024 * 1024.0))

        val seriesINodesTotal = item.findSeries("disk_list:total_inodes")
        val seriesINodesFree = item.findSeries("disk_list:free_inodes")
        val seriesINodesUsed = rebuildDataSeries(seriesINodesTotal) {
            seriesINodesTotal.points[it].v - seriesINodesFree.points[it].v
        }
        mChartINodes.setMaxValue(seriesINodesTotal.maxValue(8 * 1024 * 1024.0))
        mChartINodes.setData(
            arrayOf(
                seriesINodesUsed
            )
        )
    }

    private val mModel: DiskViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private var mItemListSorted = emptyList<RsDisk>()
    private var mItemSelectorUI = ""
    private var mItemSelectorApi = emptyRsDisk()

    private lateinit var mChartOps: TimeChartView
    private lateinit var mChartSpace: TimeChartView
    private lateinit var mChartINodes: TimeChartView

    private lateinit var mDiskListSpinner: Spinner
    private var mSpinnerValues: List<String> = emptyList()
}
