package org.kman.clearview.ui.index

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import org.json.JSONObject
import org.kman.clearview.MainActivity
import org.kman.clearview.R
import org.kman.clearview.chart.CircleChartView
import org.kman.clearview.core.BaseTimeFragment
import org.kman.clearview.core.RsNodeList
import org.kman.clearview.core.RsNodeListNode
import org.kman.clearview.core.TimePeriod
import org.kman.clearview.util.FormatFixedPoint1Percent
import org.kman.clearview.util.FormatFixedPoint2
import org.kman.clearview.util.FormatHumanDataSizePerSecond
import org.kman.clearview.view.CheckableTextView
import kotlin.math.ln


class NodeListFragment : Fragment() {

    fun setTimeMinutes(minutes: Int) {
        mMinutes = minutes
    }

    fun refresh() {
        val window = TimePeriod.buildTimeWindow(mMinutes)
        mIndexJob = mModel.startClientIndex(window, "index", null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val res = context.resources

        // setHasOptionsMenu(true)

        mModel =
            ViewModelProvider(this).get(NodeListViewModel::class.java)
        mModel.dataNodeList.observe(viewLifecycleOwner, Observer {
            onDataNodeList(it)
        })

        val root =
            inflater.inflate(org.kman.clearview.R.layout.fragment_node_list, container, false)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val itemAnimator = DefaultItemAnimator()

        mNodeListView = root.findViewById(org.kman.clearview.R.id.index_node_list_view)
        mNodeListView.setHasFixedSize(true)
        mNodeListView.layoutManager = layoutManager
        mNodeListView.itemAnimator = itemAnimator

        mNodeListAdapter = NodeListAdapter(this, context)
        mNodeListView.adapter = mNodeListAdapter

        val modeWrapperView: ViewGroup =
            root.findViewById(org.kman.clearview.R.id.index_node_mode_wrapper)

        NODE_LIST_MODE_LIST.forEach {
            val modeItemView: TextView = inflater.inflate(
                R.layout.node_list_mode_item,
                modeWrapperView, false
            ) as TextView
            modeWrapperView.addView(modeItemView)
            modeItemView.text = res.getString(it.titleId)
            modeItemView.setOnClickListener {
                onModeItemViewClick(modeItemView)
            }
        }
        mModeWrapperView = modeWrapperView

        var typeIndex = 0
        savedInstanceState?.apply {
            typeIndex = getInt(KEY_NODE_LIST_MODE)
        }
        setCheckedNodeInfoTypeIndex(typeIndex)
        mNodeListAdapter.setNodeInfoTypeIndex(typeIndex)

        return root
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mIndexJob?.cancel()
        mIndexJob = null

        mDialogConfirmDelete?.dismiss()
        mDialogConfirmDelete = null

        mDialogEditServer?.dismiss()
        mDialogEditServer = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_NODE_LIST_MODE, mNodeListAdapter.getNodeInfoTypeIndex())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_node_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.node_list_add_server -> onClickedAddServer()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun onDataNodeList(obj: RsNodeList) {
        val list = mutableListOf<RsNodeListNode>()

        list.addAll(obj.nodeList)
        list.sortWith(Comparator { o1, o2 -> o1.nodeTitle.compareTo(o2.nodeTitle) })

        mNodeListAdapter.setNodeList(list)

        obj.newNodeId?.also {
            for (i in 0 until list.size) {
                val n = list[i]
                if (obj.newNodeId == n.nodeId) {
                    mNodeListView.smoothScrollToPosition(i)
                    break
                }
            }
        }

        val activity = activity as MainActivity? ?: return
        activity.onNodeListLoaded(list)
    }

    private fun onModeItemViewClick(clicked: View) {
        val clickedItemView = clicked as CheckableTextView
        if (clickedItemView.isChecked) {
            return
        }

        for (i in 0 until NODE_LIST_MODE_LIST.size) {
            val itemView: CheckableTextView = mModeWrapperView.getChildAt(i) as CheckableTextView
            if (itemView == clickedItemView) {
                itemView.isChecked = true
                mNodeListAdapter.setNodeInfoTypeIndex(i)
            } else {
                itemView.isChecked = false
            }
        }
    }

    private fun setCheckedNodeInfoTypeIndex(i: Int) {
        for (n in 0 until mModeWrapperView.childCount) {
            (mModeWrapperView.getChildAt(n) as Checkable).isChecked = n == i
        }
    }

    private fun onClickedAddServer() {
        val window = TimePeriod.buildTimeWindow(mMinutes)
        mModel.startClientIndex(window, "createnode", null)
    }

    private fun onClickedNode(node: RsNodeListNode) {
        val activity = activity as MainActivity? ?: return
        activity.onClickedNode(node)
    }

    private fun onClickedDeleteNode(node: RsNodeListNode) {
        val context = context!!
        val dialog =
            AlertDialog.Builder(context).apply {
                setTitle(R.string.please_confirm)
                setMessage(
                    context.getString(
                        R.string.remove_server_confirm_message,
                        node.nodeTitle
                    )
                )
                setPositiveButton(android.R.string.ok) { _, _ ->
                    onClickedDeleteNodeConfirmed(node)
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                setOnDismissListener { dialog ->
                    if (mDialogConfirmDelete == dialog) {
                        mDialogConfirmDelete = null
                    }
                }
            }.create()

        dialog.show()
        mDialogConfirmDelete = dialog
    }

    private fun onClickedDeleteNodeConfirmed(node: RsNodeListNode) {
        val window = TimePeriod.buildTimeWindow(mMinutes)
        val args = JSONObject()
        args.put("node_id", node.nodeId)
        mModel.startClientIndex(window, "deletenode", args)
    }

    private fun onClickedInfoNode(node: RsNodeListNode) {
        val dialog = EditServerDialog(this, node)

        dialog.show()
        dialog.setOnDismissListener {
            if (mDialogEditServer == it) {
                mDialogEditServer = null
            }
        }
        mDialogEditServer = dialog
    }

    private fun onClickedInfoNodeConfirmed(node: RsNodeListNode, newTitle: String) {
        val window = TimePeriod.buildTimeWindow(mMinutes)
        val args = JSONObject()
        args.put("node_id", node.nodeId)
        args.put("node_title", newTitle)
        mModel.startClientIndex(window, "setnodetitle", args)
    }

    class EditServerDialog(val fragment: NodeListFragment, val node: RsNodeListNode) :
        AlertDialog(fragment.context), TextWatcher {
        override fun onCreate(savedInstanceState: Bundle?) {

            val context = fragment.context!!
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.node_list_edit_server_dialog, null, false)

            setTitle(R.string.edit_server_name_title)
            setView(view)

            setButton(
                DialogInterface.BUTTON_POSITIVE,
                context.getString(android.R.string.ok)
            ) { _, _ ->
                val text = mEdit.text.toString().trim()
                if (text.isNotEmpty()) {
                    fragment.onClickedInfoNodeConfirmed(node, text)
                }
            }
            setButton(
                DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel)
            ) { _, _ -> }

            super.onCreate(savedInstanceState)

            mButtonOK = getButton(DialogInterface.BUTTON_POSITIVE)

            mEdit = findViewById(R.id.edit_server_name_input)
            mEdit.addTextChangedListener(this)
            mEdit.setText(node.nodeTitle)
            mEdit.setSelection(mEdit.length())

            afterTextChanged(mEdit.text)

            mEdit.requestFocus()
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val value = s.toString()
            mButtonOK.isEnabled = value.trim().isNotEmpty()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        private lateinit var mButtonOK: Button
        private lateinit var mEdit: EditText
    }

    class NodeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val mWrapper: ViewGroup = view.findViewById(R.id.menu_node_list_item_wrapper)
        val mTitle: TextView = view.findViewById(R.id.node_list_item_title)
        val mIconDelete: ImageView = view.findViewById(R.id.node_list_item_delete)
        val mIconInfo: ImageView = view.findViewById(R.id.node_list_item_info)
        val mCircle: CircleChartView = view.findViewById(R.id.node_list_item_circle)
    }

    class NodeListAdapter(val fragment: NodeListFragment, val context: Context) :
        RecyclerView.Adapter<NodeViewHolder>() {

        val mInflater: LayoutInflater = LayoutInflater.from(context)
        var mList: List<RsNodeListNode> = emptyList()
        var mIsLoaded: Boolean = false
        var mNodeInfoType = NODE_LIST_MODE_LIST[0]

        init {
            setHasStableIds(false)
        }

        fun setNodeList(list: List<RsNodeListNode>) {
            mList = list
            mIsLoaded = true
            notifyDataSetChanged()
        }

        fun setNodeInfoType(n: NodeListMode) {
            if (mNodeInfoType != n) {
                mNodeInfoType = n
                notifyDataSetChanged()
            }
        }

        fun getNodeInfoTypeIndex(): Int {
            for (i in 0 until NODE_LIST_MODE_LIST.size) {
                if (mNodeInfoType == NODE_LIST_MODE_LIST[i]) {
                    return i
                }
            }
            return 0
        }

        fun setNodeInfoTypeIndex(i: Int) {
            setNodeInfoType(NODE_LIST_MODE_LIST[i])
        }

        override fun getItemCount(): Int {
            return mList.size + if (mIsLoaded) {
                1
            } else {
                0
            }
        }

        override fun getItemViewType(position: Int): Int {
            if (position < mList.size) {
                return VIEW_TYPE_NODE
            } else {
                return VIEW_TYPE_ADD
            }
        }

        override fun getItemId(position: Int): Long {
            if (getItemViewType(position) == VIEW_TYPE_NODE) {
                return super.getItemId(position)
            } else {
                return -1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
            if (viewType == VIEW_TYPE_NODE) {
                // Node
                val view = mInflater.inflate(R.layout.node_list_item, parent, false)
                val holder = NodeViewHolder(view)

                holder.mWrapper.setOnClickListener { v -> fragment.onClickedNode(getNodeForView(v)) }
                holder.mIconDelete.setOnClickListener { v ->
                    fragment.onClickedDeleteNode(
                        getNodeForView(v)
                    )
                }
                holder.mIconInfo.setOnClickListener { v ->
                    fragment.onClickedInfoNode(
                        getNodeForView(
                            v
                        )
                    )
                }

                return holder
            } else {
                // Add node
                val view = mInflater.inflate(R.layout.node_list_item_add_server, parent, false)
                val holder = NodeViewHolder(view)

                holder.mWrapper.setOnClickListener {
                    fragment.onClickedAddServer()
                }

                return holder
            }
        }

        override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
            if (holder.itemViewType == VIEW_TYPE_NODE) {
                val obj = mList.get(position)
                val title = obj.nodeTitle

                holder.mWrapper.setTag(R.id.menu_node_list_item_wrapper, obj)

                holder.mTitle.text = title

                val circle = holder.mCircle
                val n = mNodeInfoType

                val value = n.getValue(obj)
                val scaled =
                    if (value <= 0) {
                        0.0
                    } else if (value >= n.scale) {
                        1.0
                    } else if (n.isLog) {
                        ln(value + 1.0) / ln(n.scale + 1.0)
                    } else {
                        value / n.scale
                    }
                circle.setValue(scaled)
                circle.setColors(n.colorBack, n.colorFront)
                circle.setText(n.formatValue(value))
            } else {

            }
        }

        fun getNodeForView(v: View): RsNodeListNode {
            var view = v
            while (true) {
                if (view.id == R.id.menu_node_list_item_wrapper) {
                    return view.getTag(R.id.menu_node_list_item_wrapper) as RsNodeListNode
                }
                val parent: ViewParent =
                    view.parent ?: throw IllegalStateException("Can't find node itemGet wrapper")
                view = parent as View
            }
        }

        companion object {
            val VIEW_TYPE_NODE = 0
            val VIEW_TYPE_ADD = 1
        }
    }

    class NodeListMode(
        val titleId: Int,
        val colorBack: Int, val colorFront: Int,
        val scale: Double, val isLog: Boolean,
        val getValue: (node: RsNodeListNode) -> Double,
        val formatValue: (value: Double) -> String
    )

    var mIndexJob: Job? = null
    var mDialogConfirmDelete: Dialog? = null
    var mDialogEditServer: Dialog? = null

    companion object {
        val TAG = "NodeListFragment"
        val KEY_NODE_LIST_MODE = "nodeListMode"

        val NODE_LIST_MODE_LIST = listOf(
            NodeListMode(
                org.kman.clearview.R.string.node_list_fragment_mode_cpu,
                0xBBDEFB, 0x2196F3,
                800.0, true,
                object : (RsNodeListNode) -> Double {
                    override fun invoke(node: RsNodeListNode): Double {
                        return node.value_cpu
                    }
                },
                FormatFixedPoint1Percent()
            ),
            NodeListMode(
                org.kman.clearview.R.string.node_list_fragment_mode_memory,
                0xD1C4E9, 0x673AB7,
                100.0, false,
                object : (RsNodeListNode) -> Double {
                    override fun invoke(node: RsNodeListNode): Double {
                        return node.value_memory
                    }
                },
                FormatFixedPoint1Percent()
            ),
            NodeListMode(
                org.kman.clearview.R.string.node_list_fragment_mode_swap,
                0xFFCDD2, 0xF44336,
                100.0, false,
                object : (RsNodeListNode) -> Double {
                    override fun invoke(node: RsNodeListNode): Double {
                        return node.value_swap
                    }
                },
                FormatFixedPoint1Percent()
            ),
            NodeListMode(
                org.kman.clearview.R.string.node_list_fragment_mode_load,
                0xFFF9C4, 0xFFEB3B,
                40.0, true,
                object : (RsNodeListNode) -> Double {
                    override fun invoke(node: RsNodeListNode): Double {
                        return node.value_load
                    }
                },
                FormatFixedPoint2()
            ),
            NodeListMode(
                org.kman.clearview.R.string.node_list_fragment_mode_network,
                0xC8E6C9, 0x4CAF50,
                1024.0 * 1024.0 * 1024.0, true,
                object : (RsNodeListNode) -> Double {
                    override fun invoke(node: RsNodeListNode): Double {
                        return node.value_network
                    }
                },
                FormatHumanDataSizePerSecond()
            )
        )
    }

    private lateinit var mModel: NodeListViewModel

    private lateinit var mNodeListView: RecyclerView
    private lateinit var mNodeListAdapter: NodeListAdapter
    private lateinit var mModeWrapperView: ViewGroup

    private var mMinutes: Int = BaseTimeFragment.DEFAULT_MINUTES
}

