package org.kman.clearview.ui.process

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.kman.clearview.R
import org.kman.clearview.chart.TimeChartGroupController
import org.kman.clearview.chart.TimeChartView
import org.kman.clearview.core.*
import org.kman.clearview.util.FormatHumanDataSize
import org.kman.clearview.util.FormatHumanDataSizePerSecond
import org.kman.clearview.util.formatFractional
import org.kman.clearview.util.formatHumanDataSize
import org.kman.clearview.view.CheckableTextView

class ProcessFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_process
    }

    override fun getTitleId(): Int {
        return R.string.menu_process
    }

    override fun refresh() {
        val window = buildTimeWindow()

        val nodeId = getNodeId()
        mModel.startItemList(window, nodeId)
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

        val root = inflater.inflate(R.layout.fragment_process, container, false)
        val context = requireContext()

        mController = TimeChartGroupController(context)

        mChartCpu = root.findViewById(R.id.time_chart_process_cpu)
        mChartCpu.setController(mController)
        mChartCpu.setTitle(R.string.chart_title_cpu)
        mChartCpu.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "cpu", fill = 0x90CAF9, line = 0x2196F3)
            )
        )

        mChartMemory = root.findViewById(R.id.time_chart_process_memory)
        mChartMemory.setController(mController)
        mChartMemory.setTitle(R.string.chart_title_mem)
        mChartMemory.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "memory", fill = 0xCE93D8, line = 0x9C27B0)
            )
        )
        mChartMemory.setValueFormatter(FormatHumanDataSize())

        mChartCount = root.findViewById(R.id.time_chart_process_count)
        mChartCount.setController(mController)
        mChartCount.setTitle(R.string.chart_title_count)
        mChartCount.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "count", fill = 0xE6EE9C, line = 0xCDDC39)
            )
        )

        mChartIOBytes = root.findViewById(R.id.time_chart_process_iobytes)
        mChartIOBytes.setController(mController)
        mChartIOBytes.setTitle(R.string.chart_title_iobytes)
        mChartIOBytes.setDataOptions(
            arrayOf(
                TimeChartView.DataOption(name = "read (req)", fill = 0xFFE0B2, line = 0xFF9800),
                TimeChartView.DataOption(name = "write (req)", fill = 0xFFCC80, line = 0xFF9800),
                TimeChartView.DataOption(name = "read (blk)", fill = 0xFFB74D, line = 0xFF9800),
                TimeChartView.DataOption(name = "write (blk)", fill = 0xFFA726, line = 0xFF9800)
            )
        )
        mChartIOBytes.setValueFormatter(FormatHumanDataSizePerSecond())

        mProcessListButton = root.findViewById(R.id.process_list_selector_spinner)
        mProcessListButton.setOnClickListener {
            onClickSelectDialog()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mDialogProcessSelect?.dismiss()
        mDialogProcessSelect = null
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun setSortOrder(order: Comparator<RsProcess>, selection: RsProcess) {
        mSortOrder = order
        mItemSelectorApi = selection

        val selector = mItemSelectorApi
        if (selector != null) {
            mItemSelectorUI =
                getString(R.string.process_item_ui, selector.name, selector.user)

            val window = buildTimeWindow()
            val nodeId = getNodeId()
            mModel.startItemGet(window, nodeId, selector)
        } else {
            mItemSelectorUI = ""
        }

        mProcessListButton.text = mItemSelectorUI
    }

    private fun onItemList(items: RsProcessList) {
        mProcessList = ArrayList(items.processes)
        mProcessList.sortWith(mSortOrder)

        if (mProcessList.isEmpty()) {
            mItemSelectorUI = ""
            mItemSelectorApi = null
            onItemGet(null)
        } else {
            val selector = mItemSelectorApi ?: mProcessList.first()
            if (mItemSelectorUI.isEmpty()) {
                mItemSelectorApi = selector
                mItemSelectorUI = getString(
                    R.string.process_item_ui,
                    selector.name,
                    selector.user
                )
            }

            val window = buildTimeWindow()
            val nodeId = getNodeId()
            mModel.startItemGet(window, nodeId, selector)
        }

        mProcessListButton.text = mItemSelectorUI
    }

    private fun onItemGet(item: RsItemGet?) {
        if (item == null) {
            val empty = emptyArray<RsDataSeries>()
            mChartCpu.setData(empty)
            mChartMemory.setData(empty)
            mChartCount.setData(empty)
            mChartIOBytes.setData(empty)
            return
        }

        mChartCpu.setData(
            arrayOf(
                item.findSeries("process_list:cpu")
            )
        )

        mChartMemory.setData(
            arrayOf(
                item.findSeries("process_list:rss")
            )
        )

        mChartCount.setData(
            arrayOf(
                item.findSeries("process_list:instance_count")
            )
        )

        mChartIOBytes.setData(
            arrayOf(
                item.findSeries("process_list:io_chars_read"),
                item.findSeries("process_list:io_chars_write"),
                item.findSeries("process_list:io_bytes_read"),
                item.findSeries("process_list:io_bytes_write")
            )
        )
    }

    private fun onClickSelectDialog() {
        if (mProcessList.isNotEmpty() && mItemSelectorApi != null) {
            val dialog = SelectProcessDialog(this)
            dialog.show()
            dialog.setOnDismissListener {
                if (mDialogProcessSelect == it) {
                    mDialogProcessSelect = null
                }
            }
            mDialogProcessSelect = dialog
        }
    }

    private class SelectProcessDialog(val fragment: ProcessFragment) :
        AlertDialog(fragment.context), View.OnClickListener, AdapterView.OnItemClickListener {

        @SuppressLint("InflateParams")
        override fun onCreate(savedInstanceState: Bundle?) {

            val context = fragment.requireContext()
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.process_list_select_dialog, null, false)

            setTitle(R.string.process_list_select_title)
            setView(view)

            val processListSource = fragment.mProcessList
            mProcessList = ArrayList(processListSource)

            mSelectedProcess = requireNotNull(fragment.mItemSelectorApi)

            mListView = view.findViewById(android.R.id.list)
            mListAdapter = SelectProcessAdapter(inflater, mProcessList, mSelectedProcess)
            mListView.adapter = mListAdapter
            mListView.onItemClickListener = this

            for (i in 0 until mProcessList.size) {
                if (mSelectedProcess == mProcessList[i]) {
                    mListView.setSelection(i)
                }
            }

            mSortName = view.findViewById(R.id.process_list_sort_name)
            mSortIO = view.findViewById(R.id.process_list_sort_io)
            mSortCPU = view.findViewById(R.id.process_list_sort_cpu)
            mSortMemory = view.findViewById(R.id.process_list_sort_memory)

            mSortName.isChecked = true
            mSortName.setOnClickListener(this)
            mSortIO.setOnClickListener(this)
            mSortCPU.setOnClickListener(this)
            mSortMemory.setOnClickListener(this)

            setButton(
                DialogInterface.BUTTON_POSITIVE,
                context.getString(android.R.string.ok)
            ) { _, _ ->
                fragment.setSortOrder(mSortOrder, mSelectedProcess)
            }
            setButton(
                DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel)
            ) { _, _ -> }

            super.onCreate(savedInstanceState)
        }

        override fun onClick(v: View) {
            val list = arrayOf(mSortName, mSortIO, mSortCPU, mSortMemory)
            for (view in list) {
                if (view == v) {
                    if (view.isChecked) {
                        return
                    }
                }
            }

            for (view in list) {
                view.isChecked = view == v
            }

            mSortOrder = when (v) {
                mSortName -> SORT_BY_NAME
                mSortIO -> SORT_BY_IO
                mSortCPU -> SORT_BY_CPU
                mSortMemory -> SORT_BY_MEMORY
                else -> return
            }

            mProcessList.sortWith(mSortOrder)
            mListAdapter.notifyDataSetChanged()

            mSelectedProcess = mProcessList.first()
            mListView.setSelection(0)

            mListAdapter.setSelection(mSelectedProcess)
        }

        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            mSelectedProcess = mProcessList[position]
            mListAdapter.setSelection(mSelectedProcess)
            mListAdapter.notifyDataSetChanged()
        }

        private lateinit var mSortName: CheckableTextView
        private lateinit var mSortIO: CheckableTextView
        private lateinit var mSortCPU: CheckableTextView
        private lateinit var mSortMemory: CheckableTextView

        private lateinit var mListView: ListView
        private lateinit var mListAdapter: SelectProcessAdapter
        private lateinit var mSelectedProcess: RsProcess

        private var mProcessList: ArrayList<RsProcess> = ArrayList()
        private var mSortOrder: Comparator<RsProcess> = SORT_BY_NAME
    }

    private class SelectProcessAdapter(
        val inflater: LayoutInflater,
        var list: MutableList<RsProcess>,
        selection: RsProcess
    ) : BaseAdapter() {

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Any {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return -1
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView
                ?: inflater.inflate(R.layout.process_list_select_item, parent, false)
            val item = list[position]

            val viewName: CheckableTextView = view.findViewById(R.id.process_item_name)

            viewName.text = item.name
            viewName.isChecked = mSelection == item

            view.findViewById<TextView>(R.id.process_item_user).text = item.user
            view.findViewById<TextView>(R.id.process_item_count).text = item.count.toString()
            view.findViewById<TextView>(R.id.process_item_io).text = item.io_total.toString()
            view.findViewById<TextView>(R.id.process_item_cpu).text = formatFractional(item.cpu)
            view.findViewById<TextView>(R.id.process_item_memory).text =
                formatHumanDataSize(item.memory)

            return view
        }

        fun setSelection(selection: RsProcess) {
            mSelection = selection
        }

        private var mSelection = selection
    }

    companion object {
        val TAG = "ProcessFragment"

        val SORT_BY_NAME: Comparator<RsProcess> = Comparator { o1, o2 ->
            o1.name.compareTo(o2.name)
        }
        val SORT_BY_IO: Comparator<RsProcess> = Comparator { o1, o2 ->
            o2.io_total.compareTo(o1.io_total)
        }
        val SORT_BY_CPU: Comparator<RsProcess> = Comparator { o1, o2 ->
            o2.cpu.compareTo(o1.cpu)
        }
        val SORT_BY_MEMORY: Comparator<RsProcess> = Comparator { o1, o2 ->
            o2.memory.compareTo(o1.memory)
        }
    }

    private val mModel: ProcessViewModel by viewModels()

    private lateinit var mController: TimeChartGroupController

    private var mProcessList = ArrayList<RsProcess>()
    private var mSortOrder: Comparator<RsProcess> = SORT_BY_NAME

    private var mItemSelectorUI = ""
    private var mItemSelectorApi: RsProcess? = null

    private lateinit var mChartCpu: TimeChartView
    private lateinit var mChartMemory: TimeChartView
    private lateinit var mChartCount: TimeChartView
    private lateinit var mChartIOBytes: TimeChartView

    private lateinit var mProcessListButton: TextView

    private var mDialogProcessSelect: Dialog? = null
}
