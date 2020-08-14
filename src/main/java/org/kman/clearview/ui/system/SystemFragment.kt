package org.kman.clearview.ui.system

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.kman.clearview.R
import org.kman.clearview.core.BaseDetailFragment
import org.kman.clearview.core.RsSystem
import org.kman.clearview.util.formatHumanDataSize
import org.kman.clearview.view.ColorBarView
import kotlin.math.roundToInt

class SystemFragment : BaseDetailFragment() {

    override fun getNavigationId(): Int {
        return R.id.nav_system
    }

    override fun getTitleId(): Int {
        return R.string.menu_system
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
            ViewModelProvider(this).get(SystemViewModel::class.java)
        mModel.data.observe(viewLifecycleOwner, Observer {
            onData(it)
        })

        val root = inflater.inflate(R.layout.fragment_system, container, false)
        val context = requireContext()

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val itemAnimator = DefaultItemAnimator()

        mListView = root.findViewById(R.id.system_list_view)
        mListView.setHasFixedSize(true)
        mListView.layoutManager = layoutManager
        mListView.itemAnimator = itemAnimator

        mAdapter = SystemAdapter(context)
        mListView.adapter = mAdapter

        return root
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }


    private fun onData(data: RsSystem) {
        val list = ArrayList<SystemItem>()

        list.add(SystemItem(VIEW_TYPE_VERSIONS))
        list.add(SystemItem(VIEW_TYPE_CPU))
        list.add(SystemItem(VIEW_TYPE_MEMORY))
        list.add(SystemItem(VIEW_TYPE_DISK))

        list.add(SystemItem(VIEW_TYPE_LISTENING_HEADER))

        data.ports.listen?.also {
            var index = 0
            for (listening in it) {
                list.add(SystemItem(VIEW_TYPE_LISTENING_DATA, index++))
            }
        }

        list.add(SystemItem(VIEW_TYPE_ACTIVE_HEADER))

        data.ports.active?.also {
            var index = 0
            for (listening in it) {
                list.add(SystemItem(VIEW_TYPE_ACTIVE_DATA, index++))
            }
        }

        mAdapter.mData = data
        mAdapter.mList = list
        mAdapter.notifyDataSetChanged()
    }

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Version etc.
        val versionOs: TextView? = view.findViewById(R.id.system_os_version)
        val versionKernel: TextView? = view.findViewById(R.id.system_kernel_version)
        val versionApache: TextView? = view.findViewById(R.id.system_apache_version)
        val versionNginx: TextView? = view.findViewById(R.id.system_nginx_version)
        val versionMySql: TextView? = view.findViewById(R.id.system_mysql_version)
        val versionPgSql: TextView? = view.findViewById(R.id.system_pgsql_version)

        // CPU info
        val cpuLabel: TextView? = view.findViewById(R.id.system_cpu_label)
        val cpuCoreCount: TextView? = view.findViewById(R.id.system_cpu_core_count)
        val cpuLoadBar: ColorBarView? = view.findViewById(R.id.system_cpu_load_bar)

        // Memory
        val memReal: TextView? = view.findViewById(R.id.system_mem_real)
        val memRealBar: ColorBarView? = view.findViewById(R.id.system_mem_real_bar)
        val memSwap: TextView? = view.findViewById(R.id.system_mem_swap)
        val memSwapBar: ColorBarView? = view.findViewById(R.id.system_mem_swap_bar)

        // Disk
        val diskSize: TextView? = view.findViewById(R.id.system_disk_size)
        val diskUsed: TextView? = view.findViewById(R.id.system_disk_used)
        val diskBar: ColorBarView? = view.findViewById(R.id.system_disk_bar)

        // Listening or active process
        val processName: TextView? = view.findViewById(R.id.system_process_name)
        val processUser: TextView? = view.findViewById(R.id.system_process_user)
        val processType: TextView? = view.findViewById(R.id.system_process_type)
        val processPort: TextView? = view.findViewById(R.id.system_process_port)
        val processAddr: TextView? = view.findViewById(R.id.system_process_address)
        val processCount: TextView? = view.findViewById(R.id.system_process_count)
    }

    class SystemItem(val type: Int, val index: Int = -1) {
    }

    class SystemAdapter(val context: Context) : RecyclerView.Adapter<SystemViewHolder>() {
        override fun getItemCount(): Int {
            return mList.size
        }

        override fun getItemViewType(position: Int): Int {
            val item = mList[position]
            return item.type
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SystemViewHolder {
            val view: View = mInflater.inflate(
                when (viewType) {
                    VIEW_TYPE_VERSIONS -> R.layout.system_list_versions
                    VIEW_TYPE_CPU -> R.layout.system_list_cpu
                    VIEW_TYPE_MEMORY -> R.layout.system_list_memory
                    VIEW_TYPE_DISK -> R.layout.system_list_disk
                    VIEW_TYPE_LISTENING_HEADER -> R.layout.system_list_listening_header
                    VIEW_TYPE_LISTENING_DATA -> R.layout.system_list_listening_data
                    VIEW_TYPE_ACTIVE_HEADER -> R.layout.system_list_active_header
                    VIEW_TYPE_ACTIVE_DATA -> R.layout.system_list_active_data
                    else -> throw IllegalStateException("Unknown view type")
                }, parent, false
            )

            return SystemViewHolder(view)
        }

        override fun onBindViewHolder(holder: SystemViewHolder, position: Int) {
            val data = mData!!
            val res = context.resources

            val item = mList[position]
            when (item.type) {
                VIEW_TYPE_VERSIONS -> {
                    holder.versionOs!!.text = res.getString(
                        R.string.system_os_label_and_version,
                        data.system_text.os_dist_label, data.system_text.os_version_label
                    )

                    holder.versionKernel!!.text = data.system_text.kernel_label

                    val apache = holder.versionApache!!
                    if (data.system_text.app_apache_version.isEmpty()) {
                        apache.visibility = View.GONE
                    } else {
                        apache.visibility = View.VISIBLE
                        apache.text = data.system_text.app_apache_version
                    }

                    val nginx = holder.versionNginx!!
                    if (data.system_text.app_nginx_version.isEmpty()) {
                        nginx.visibility = View.GONE
                    } else {
                        nginx.visibility = View.VISIBLE
                        nginx.text = data.system_text.app_nginx_version
                    }

                    val mysql = holder.versionMySql!!
                    if (data.system_text.app_mysql_version.isEmpty()) {
                        mysql.visibility = View.GONE
                    } else {
                        mysql.visibility = View.VISIBLE
                        var text = data.system_text.app_mysql_version
                        if (!text.startsWith("MySql")) {
                            text = "MySql " + text
                        }
                        mysql.text = text
                    }

                    val pgsql = holder.versionPgSql!!
                    if (data.system_text.app_pgsql_version.isEmpty()) {
                        pgsql.visibility = View.GONE
                    } else {
                        pgsql.visibility = View.VISIBLE
                        var text = data.system_text.app_pgsql_version
                        if (!text.startsWith("PostgreSQL")) {
                            text = "PostgreSQL " + text
                        }
                        pgsql.text = text
                    }
                }

                VIEW_TYPE_CPU -> {
                    holder.cpuLabel!!.text = data.system_text.cpu_label
                    holder.cpuCoreCount!!.text = res.getQuantityString(
                        R.plurals.system_cpu_core_count,
                        data.system_numeric.value_cpun,
                        data.system_numeric.value_cpun
                    )

                    val bar = holder.cpuLoadBar!!
                    bar.setColor(0x2196F3)
                    bar.setPercent(data.system_numeric.value_cpu.roundToInt())
                }

                VIEW_TYPE_MEMORY -> {
                    holder.memReal!!.text =
                        res.getString(
                            R.string.system_memory_real,
                            formatHumanDataSize(data.memory.mem_real_size)
                        )
                    holder.memSwap!!.text =
                        res.getString(
                            R.string.system_memory_swap,
                            formatHumanDataSize(data.memory.mem_swap_size)
                        )

                    val barReal = holder.memRealBar!!
                    barReal.setColor(0xBA68C8)
                    barReal.setPercent(data.memory.mem_real_used, data.memory.mem_real_size)

                    val barSwap = holder.memSwapBar!!
                    barSwap.setColor(0xE57373)
                    barSwap.setPercent(data.memory.mem_swap_used, data.memory.mem_swap_size)
                }

                VIEW_TYPE_DISK -> {
                    holder.diskSize!!.text =
                        res.getString(
                            R.string.system_disk_size,
                            formatHumanDataSize(data.memory.disk_total_size)
                        )
                    holder.diskUsed!!.text =
                        res.getString(
                            R.string.system_disk_used,
                            formatHumanDataSize(data.memory.disk_total_used)
                        )

                    val bar = holder.diskBar!!
                    bar.setColor(0xFFB74D)
                    bar.setPercent(data.memory.disk_total_used, data.memory.disk_total_size)
                }

                VIEW_TYPE_LISTENING_DATA -> {
                    val process = data.ports.listen!![item.index]

                    holder.processName!!.text = process.name
                    holder.processUser!!.text = process.user
                    holder.processType!!.text = process.type
                    holder.processPort!!.text = process.src_port.toString()
                    holder.processAddr!!.text = process.src_addr
                }

                VIEW_TYPE_ACTIVE_DATA -> {
                    val process = data.ports.active!![item.index]

                    holder.processName!!.text = process.name
                    holder.processUser!!.text = process.user
                    holder.processCount!!.text = process.count.toString()
                }
            }
        }

        val mInflater: LayoutInflater = LayoutInflater.from(context)
        var mData: RsSystem? = null
        var mList: List<SystemItem> = emptyList()
    }

    companion object {
        val VIEW_TYPE_VERSIONS = 0
        val VIEW_TYPE_CPU = 1
        val VIEW_TYPE_MEMORY = 2
        val VIEW_TYPE_DISK = 3
        val VIEW_TYPE_LISTENING_HEADER = 4
        val VIEW_TYPE_LISTENING_DATA = 5
        val VIEW_TYPE_ACTIVE_HEADER = 6
        val VIEW_TYPE_ACTIVE_DATA = 7
    }

    private lateinit var mModel: SystemViewModel

    private lateinit var mListView: RecyclerView
    private lateinit var mAdapter: SystemAdapter
}
