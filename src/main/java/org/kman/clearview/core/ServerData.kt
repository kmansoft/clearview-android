package org.kman.clearview.core

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlin.math.max

object ServerData {
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
}

// Node (server) list

@JsonClass(generateAdapter = true)
data class RsNodeListNode(
    @Json(name = "account_id")
    val accountId: String,
    @Json(name = "node_id")
    val nodeId: String,
    @Json(name = "node_title")
    val nodeTitle: String,
    val value_cpu: Double,
    val value_memory: Double,
    val value_swap: Double,
    val value_load: Double,
    val value_network: Double
)

@JsonClass(generateAdapter = true)
data class RsNodeList(
    @Json(name = "demo_mode")
    val demoMode: Boolean,
    @Json(name = "node_list")
    val nodeList: List<RsNodeListNode>,
    @Json(name = "new_node_id")
    val newNodeId: String?
)

// Time window used with below, not sent, just a convenience

data class RqTimeWindow(
    val pointCount: Int,
    val pointDuration: Int
)

// Data point: time, value, null or not

@JsonClass(generateAdapter = true)
data class RsDataPoint(
    val t: Int,
    val v: Double,
    val n: Boolean = false
)

// Data series: what it is and a list of points

@JsonClass(generateAdapter = true)
data class RsDataSeries(
    val sub: String,
    val points: List<RsDataPoint>
) {
    fun maxValue(fallback: Double): Double {
        var curr = 0.0
        var used = false
        for (pt in points) {
            if (!pt.n) {
                if (used) {
                    if (curr < pt.v) {
                        curr = pt.v
                    }
                } else {
                    curr = pt.v
                    used = true
                }
            }
        }

        return if (used) {
            curr
        } else {
            fallback
        }
    }

    companion object {
        fun createNull(
            sub: String,
            endTime: Long,
            pointCount: Int,
            pointDuration: Int
        ): RsDataSeries {
            var end = if (endTime > 0) {
                endTime
            } else {
                System.currentTimeMillis() / 1000L
            }
            end /= 60
            end *= 60
            val start = end - pointCount * pointDuration
            val points = List(
                pointCount + 1
            ) { i ->
                RsDataPoint(
                    t = (start + i * pointDuration).toInt(),
                    v = 0.0, n = true
                )
            }
            return RsDataSeries(sub, points)
        }
    }
}

// Basic data

@JsonClass(generateAdapter = true)
data class RqNodeData(
    val node_id: String,
    val series: List<String>,
    @Json(name = "end_time")
    val endTime: Long,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsNodeData(
    val request: RqNodeData,
    val series: List<RsDataSeries>
) {
    fun findSeries(s: String): RsDataSeries {
        series.forEach {
            if (it.sub == s) {
                return it
            }
        }

        // Data not there, make a synthetic series of nulls only
        return RsDataSeries.createNull(
            s,
            request.endTime,
            request.pointCount,
            request.pointDuration
        )
    }
}

// App data (apache, nginx, mysql)

@JsonClass(generateAdapter = true)
data class RqApp(
    val node_id: String,
    @Json(name = "end_time")
    val endTime: Long,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsApp(
    val request: RqApp,
    val series: List<RsDataSeries>
) {
    fun findSeries(s: String): RsDataSeries {
        series.forEach {
            if (it.sub == s) {
                return it
            }
        }

        // Data not there, make a synthetic series of nulls only
        return RsDataSeries.createNull(
            s,
            request.endTime,
            request.pointCount,
            request.pointDuration
        )
    }
}

// Indexed data

@JsonClass(generateAdapter = true)
data class RqItemList(
    val node_id: String,
    val series: List<String>,
    @Json(name = "end_time")
    val endTime: Long,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsItemList(
    val request: RqItemList,
    val items: List<String>
) {
}

@JsonClass(generateAdapter = true)
data class RqItemGet(
    val node_id: String,
    val item: String,
    val series: List<String>,
    @Json(name = "end_time")
    val endTime: Long,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsItemGet(
    val request: RqItemGet,
    val series: List<RsDataSeries>
) {
    fun findSeries(s: String): RsDataSeries {
        series.forEach {
            if (it.sub == s) {
                return it
            }
        }

        // Data not there, make a synthetic series of nulls only
        return RsDataSeries.createNull(
            s,
            request.endTime,
            request.pointCount,
            request.pointDuration
        )
    }
}

// Disk list

@JsonClass(generateAdapter = true)
data class RqDiskList(
    val node_id: String,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsDisk(
    val name: String,
    val read_ops: Long,
    val write_ops: Long,
    val space_total: Long,
    val space_free: Long,
    val inode_total: Long,
    val inode_free: Long
)

@JsonClass(generateAdapter = true)
data class RsDiskList(
    val request: RqDiskList,
    val disks: List<RsDisk>
)

// Process list

@JsonClass(generateAdapter = true)
data class RqProcessList(
    val node_id: String,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsProcess(
    val name: String,
    val user: String,
    val cpu: Float,
    val count: Int,
    val memory: Long,
    val io_total: Long
) {
    override fun equals(other: Any?): Boolean {
        val p = other as RsProcess?
        return p != null && name == p.name && user == p.user
    }

    override fun hashCode(): Int {
        return name.hashCode() + user.hashCode()
    }
}

@JsonClass(generateAdapter = true)
data class RsProcessList(
    val request: RqProcessList,
    val processes: List<RsProcess>
)

// System

@JsonClass(generateAdapter = true)
data class RqSystem(
    val node_id: String,
    @Json(name = "end_time")
    val endTime: Long,
    @Json(name = "point_count")
    val pointCount: Int,
    @Json(name = "point_duration")
    val pointDuration: Int
)

@JsonClass(generateAdapter = true)
data class RsSystemText(
    val cpu_label: String,
    val kernel_label: String,
    val os_dist_label: String,
    val os_version_label: String,
    val app_apache_version: String,
    val app_nginx_version: String,
    val app_mysql_version: String,
    val app_pgsql_version: String
)

@JsonClass(generateAdapter = true)
data class RsSystemNumeric(
    val value_cpu: Double,
    val value_memory: Int,
    val value_swap: Int,
    val value_load: Double,
    val value_network: Int,
    val value_cpun: Int,
    @Json(name = "when")
    val when_time: Int
)

@JsonClass(generateAdapter = true)
data class RsSystemMemory(
    val mem_real_size: Long,
    val mem_real_used: Long,
    val mem_swap_size: Long,
    val mem_swap_used: Long,
    val disk_total_size: Long,
    val disk_total_used: Long
)

@JsonClass(generateAdapter = true)
data class RsSystemListening(
    val name: String,
    val user: String,
    val type: String,
    val src_addr: String,
    val src_port: Int
)

@JsonClass(generateAdapter = true)
data class RsSystemActive(
    val name: String,
    val user: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class RsSystemPorts(
    val listen: List<RsSystemListening>?,
    val active: List<RsSystemActive>?
)

@JsonClass(generateAdapter = true)
data class RsSystem(
    val request: RqSystem,
    val system_text: RsSystemText,
    val system_numeric: RsSystemNumeric,
    val memory: RsSystemMemory,
    val ports: RsSystemPorts
)

// Utility

fun rebuildDataSeries(series: RsDataSeries, value: (Int) -> Double): RsDataSeries {
    return RsDataSeries(series.sub, List(
        series.points.size
    ) { index ->
        RsDataPoint(
            series.points[index].t,
            max(0.0, value(index)),
            series.points[index].n
        )
    })
}
