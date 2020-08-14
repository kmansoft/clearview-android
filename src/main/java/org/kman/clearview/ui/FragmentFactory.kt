package org.kman.clearview.ui
import org.kman.clearview.R
import org.kman.clearview.core.BaseDetailFragment
import org.kman.clearview.ui.apache.ApacheFragment
import org.kman.clearview.ui.disk.DiskFragment
import org.kman.clearview.ui.mysql.MySqlFragment
import org.kman.clearview.ui.network.NetworkFragment
import org.kman.clearview.ui.nginx.NginxFragment
import org.kman.clearview.ui.overview.OverviewFragment
import org.kman.clearview.ui.pgsql.PgSqlFragment
import org.kman.clearview.ui.process.ProcessFragment
import org.kman.clearview.ui.system.SystemFragment

object FragmentFactory {
    fun createDetailFragmentById(id: Int): BaseDetailFragment {
        return when (id) {
            R.id.nav_overview -> OverviewFragment()
            R.id.nav_network -> NetworkFragment()
            R.id.nav_disk -> DiskFragment()
            R.id.nav_process -> ProcessFragment()
            R.id.nav_app_apache -> ApacheFragment()
            R.id.nav_app_nginx -> NginxFragment()
            R.id.nav_app_mysql -> MySqlFragment()
            R.id.nav_app_pgsql -> PgSqlFragment()
            R.id.nav_system -> SystemFragment()
            else -> throw IllegalArgumentException("Unknown fragment id")
        }
    }
}