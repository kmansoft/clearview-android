package org.kman.clearview

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import org.kman.clearview.core.*
import org.kman.clearview.ui.FragmentFactory
import org.kman.clearview.ui.index.NodeListFragment
import org.kman.clearview.ui.login.LoginFragment
import org.kman.clearview.ui.overview.OverviewFragment
import org.kman.clearview.util.MyGlobalScope
import org.kman.clearview.util.MyLog
import org.kman.clearview.util.Prefs

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MyLog.i(TAG, "onCreate")

        mHandler = Handler(Looper.getMainLooper(), this::handlerFunc)

        setContentView(R.layout.activity_main)
        mIsTwoPanel = findViewById<View>(R.id.two_panel_layout_marker) != null

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        mRefreshFab = fab
        fab.setOnClickListener {
            onClickRefresh()
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerLayout = drawerLayout

        val navView: NavigationView = findViewById(R.id.nav_view)
        mNavView = navView
        mNavHeaderView = navView.getHeaderView(0)

        navView.setNavigationItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem)
            true
        }

        updateNavDrawerItems()

        val arrowDrawable = DrawerArrowDrawable(toolbar.context)
        toolbar.navigationIcon = arrowDrawable

        savedInstanceState?.also {
            mState = it.getInt(KEY_STATE)
            mNodeId = it.getString(KEY_NODE_ID, "")
            mNodeTitle = it.getString(KEY_NODE_TITLE, "")
            mMinutes = it.getInt(KEY_MINUTES, BaseTimeFragment.DEFAULT_MINUTES)
        }

        startLoadingAuthInfo()

        postUpdateState()
    }

    private fun handlerFunc(msg: Message): Boolean {
        when (msg.what) {
            WHAT_UPDATE_UI ->
                onUpdateState()
            else ->
                return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        mLogoutConfirmDialog?.dismiss()
        mLogoutConfirmDialog = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isMenuLocked()) {
            return true
        }

        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId

        if (itemId == android.R.id.home) {
            if (!isDrawerLocked()) {
                val gravity = GravityCompat.START
                if (mDrawerLayout.isDrawerOpen(gravity)) {
                    mDrawerLayout.closeDrawer(gravity)
                } else {
                    mDrawerLayout.openDrawer(gravity)
                }
            }
            return true
        }

        if (isMenuLocked()) {
            return super.onOptionsItemSelected(item)
        }

        when (item.itemId) {
            R.id.action_log_out -> {
                confirmLoggingOut()
            }
            R.id.action_settings -> {
                val intent = Intent(this, PrefsActivity::class.java)
                mReqPrefs.launch(intent)
            }
            else -> {
                if (!onTimePeriodOption(itemId)) {
                    return super.onOptionsItemSelected(item)
                }
            }
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.also {
            it.putInt(KEY_STATE, mState)
            it.putString(KEY_NODE_ID, mNodeId)
            it.putString(KEY_NODE_TITLE, mNodeTitle)
            it.putInt(KEY_MINUTES, mMinutes)
        }
    }

    override fun onBackPressed() {
        val gravity = GravityCompat.START
        if (mDrawerLayout.isDrawerOpen(gravity)) {
            mDrawerLayout.closeDrawer(gravity)
            return
        }

        if (!mIsTwoPanel) {
            val fm = supportFragmentManager
            if (fm.findFragmentById(R.id.detail_content_frame) != null) {
                mState = STATE_SERVERS
                postUpdateState()
                return
            }
        }

        super.onBackPressed()
    }

    fun onClickedNode(node: RsNodeListNode) {
        mNodeId = node.nodeId
        mNodeTitle = node.nodeTitle

        mState = STATE_DETAIL
        postUpdateState()
    }

    fun onNodeListLoaded(list: List<RsNodeListNode>) {
        if (mIsTwoPanel) {
            val fm = supportFragmentManager
            val detail = fm.findFragmentById(R.id.detail_content_frame)

            if (detail == null) {
                if (!list.isEmpty()) {
                    val node = list[0]

                    mNodeId = node.nodeId
                    mNodeTitle = node.nodeTitle

                    mState = STATE_DETAIL
                }
            } else {
                if (list.isEmpty()) {
                    fm.beginTransaction().remove(detail).commit()
                    fm.executePendingTransactions()

                    mState = STATE_SERVERS
                    postUpdateState()
                }
            }
        }
    }

    fun onAuthInfoLoaded(authInfo: AuthInfo) {
        if (mState != STATE_DETAIL) {
            mState = STATE_SERVERS
        }

        onUpdateDrawerWithAuth(authInfo)
        postUpdateState()
    }

    private fun onAuthInfoMissing() {
        mState = STATE_LOGIN

        onUpdateDrawerWithAuth(null)
        postUpdateState()
    }

    private fun onUpdateDrawerWithAuth(authInfo: AuthInfo?) {

        val drawerTitle: TextView = mNavHeaderView.findViewById(R.id.side_nav_bar_title)

        if (authInfo == null) {
            drawerTitle.text = getText(R.string.nav_header_not_connected)
        } else {
            drawerTitle.text = authInfo.server
        }
    }

    private fun onResultPrefs(res: ActivityResult) {
        if (res.resultCode == RESULT_OK) {
            updateNavDrawerItems()
        }
    }

    private fun postUpdateState() {
        mHandler.removeMessages(WHAT_UPDATE_UI)
        mHandler.sendEmptyMessage(WHAT_UPDATE_UI)
    }

    private fun onUpdateState() {
        // Subtitle
        val actionBar = supportActionBar!!
        if (mState == STATE_DETAIL) {
            actionBar.subtitle = mNodeTitle
        } else {
            actionBar.subtitle = null
        }

        // FAB
        if (mState == STATE_SERVERS || mState == STATE_DETAIL) {
            mRefreshFab.show()
        } else {
            mRefreshFab.hide()
        }

        // Layout
        if (mIsTwoPanel) {
            onUpdateLayout_TwoPanel()
        } else {
            onUpdateLayout_SinglePanel()
        }

        // Drawer and menu
        updateDrawerLocked()
        updateMenuLocked()
    }

    private fun onUpdateLayout_SinglePanel() {
        val actionBar = supportActionBar!!
        val fm = supportFragmentManager

        val transaction = fm.beginTransaction()

        // States
        if (mState == STATE_LOGIN) {
            // We need the user to log in
            val existingLogin = fm.findFragmentById(R.id.login_content_frame)
            if (existingLogin == null) {
                transaction.add(R.id.login_content_frame, LoginFragment())
            }

            fm.findFragmentById(R.id.master_content_frame)?.also {
                transaction.remove(it)
            }

            fm.findFragmentById(R.id.detail_content_frame)?.also {
                transaction.remove(it)
            }

            actionBar.setTitle(R.string.menu_login)
        } else if (mState == STATE_SERVERS) {
            // Looking at the list of servers
            fm.findFragmentById(R.id.login_content_frame)?.also {
                transaction.remove(it)
            }

            val existingMaster = fm.findFragmentById(R.id.master_content_frame)
            if (existingMaster == null) {
                transaction.add(R.id.master_content_frame, NodeListFragment())
            }

            fm.findFragmentById(R.id.detail_content_frame)?.also {
                transaction.remove(it)
            }

            mNavView.setCheckedItem(R.id.nav_node_list)

            actionBar.setTitle(R.string.menu_node_list)
            actionBar.subtitle = null
        } else if (mState == STATE_DETAIL) {
            // User wants to look at a particular node's data in detail
            fm.findFragmentById(R.id.login_content_frame)?.also {
                transaction.remove(it)
            }

            fm.findFragmentById(R.id.master_content_frame)?.also {
                transaction.remove(it)
            }

            val existingDetail = fm.findFragmentById(R.id.detail_content_frame)
                    as BaseDetailFragment?

            if (existingDetail == null || existingDetail.getNodeId() != mNodeId) {
                val newDetail = OverviewFragment()
                newDetail.arguments = Bundle().apply {
                    putString(BaseDetailFragment.KEY_NODE_ID, mNodeId)
                    putString(BaseDetailFragment.KEY_NODE_TITLE, mNodeTitle)
                    putInt(BaseDetailFragment.KEY_MINUTES, mMinutes)
                }

                if (existingDetail == null) {
                    transaction.setCustomAnimations(R.anim.alpha_fade_in, R.anim.alpha_fade_out)
                    transaction.add(R.id.detail_content_frame, newDetail)
                } else {
                    transaction.replace(R.id.detail_content_frame, newDetail)
                }

                mNavView.setCheckedItem(newDetail.getNavigationId())

                actionBar.setTitle(R.string.menu_overview)
                actionBar.subtitle = mNodeTitle
            } else {
                mNavView.setCheckedItem(existingDetail.getNavigationId())

                actionBar.setTitle(existingDetail.getTitleId())
                actionBar.subtitle = existingDetail.getNodeTitle()
            }
        }

        if (!transaction.isEmpty) {
            transaction.commit()
            fm.executePendingTransactions()
        }
    }

    private fun onUpdateLayout_TwoPanel() {
        val actionBar = supportActionBar!!
        val fm = supportFragmentManager

        val transaction = fm.beginTransaction()

        if (mState == STATE_LOGIN) {
            // We need the user to log in
            val existingLogin = fm.findFragmentById(R.id.login_content_frame)
            if (existingLogin == null) {
                transaction.add(R.id.login_content_frame, LoginFragment())
            }

            fm.findFragmentById(R.id.master_content_frame)?.also {
                transaction.remove(it)
            }

            fm.findFragmentById(R.id.detail_content_frame)?.also {
                transaction.remove(it)
            }

            actionBar.setTitle(R.string.menu_login)
        } else if (mState == STATE_SERVERS) {
            // Looking at the list of servers
            fm.findFragmentById(R.id.login_content_frame)?.also {
                transaction.remove(it)
            }

            val existingMaster = fm.findFragmentById(R.id.master_content_frame)
            if (existingMaster == null) {
                transaction.add(R.id.master_content_frame, NodeListFragment())
            }

            fm.findFragmentById(R.id.detail_content_frame)?.also {
                transaction.remove(it)
            }

            mNavView.setCheckedItem(R.id.nav_node_list)

            actionBar.setTitle(R.string.menu_node_list)
            actionBar.subtitle = null
        } else if (mState == STATE_DETAIL) {
            // User wants to look at a particular node's data in detail
            fm.findFragmentById(R.id.login_content_frame)?.also {
                transaction.remove(it)
            }

            val existingMaster = fm.findFragmentById(R.id.master_content_frame)
            if (existingMaster == null) {
                transaction.add(R.id.master_content_frame, NodeListFragment())
            }

            val existingDetail = fm.findFragmentById(R.id.detail_content_frame)
                    as BaseDetailFragment?

            if (existingDetail == null || existingDetail.getNodeId() != mNodeId) {
                val newDetail = OverviewFragment()
                newDetail.arguments = Bundle().apply {
                    putString(BaseDetailFragment.KEY_NODE_ID, mNodeId)
                    putString(BaseDetailFragment.KEY_NODE_TITLE, mNodeTitle)
                    putInt(BaseDetailFragment.KEY_MINUTES, mMinutes)
                }

                if (existingDetail == null) {
                    transaction.setCustomAnimations(R.anim.alpha_fade_in, R.anim.alpha_fade_out)
                    transaction.add(R.id.detail_content_frame, newDetail)
                } else {
                    transaction.replace(R.id.detail_content_frame, newDetail)
                }

                mNavView.setCheckedItem(newDetail.getNavigationId())

                actionBar.setTitle(R.string.menu_overview)
                actionBar.subtitle = mNodeTitle
            } else {
                mNavView.setCheckedItem(existingDetail.getNavigationId())

                actionBar.setTitle(existingDetail.getTitleId())
                actionBar.subtitle = existingDetail.getNodeTitle()
            }
        }

        if (!transaction.isEmpty) {
            transaction.commit()
            fm.executePendingTransactions()
        }
    }

    private fun onNavigationItemSelected(item: MenuItem) {
        if (isDrawerLocked()) {
            return
        }

        val itemId = item.itemId

        if (itemId == R.id.nav_node_list) {
            mState = STATE_SERVERS
        } else {
            val fm = supportFragmentManager
            val transaction = fm.beginTransaction()

            transaction.setCustomAnimations(R.anim.alpha_fade_in, R.anim.alpha_fade_out)

            val newDetail =
                FragmentFactory.createDetailFragmentById(itemId)
            newDetail.arguments = Bundle().apply {
                putString(BaseDetailFragment.KEY_NODE_ID, mNodeId)
                putString(BaseDetailFragment.KEY_NODE_TITLE, mNodeTitle)
            }
            transaction.replace(R.id.detail_content_frame, newDetail)

            transaction.commit()
            fm.executePendingTransactions()

            mState = STATE_DETAIL
        }

        mDrawerLayout.closeDrawer(GravityCompat.START)
        mNavView.setCheckedItem(itemId)

        postUpdateState()
    }

    private fun isDrawerLocked(): Boolean {
        return mState == STATE_LOGIN || mState == STATE_SERVERS
    }

    private fun updateDrawerLocked() {
        mDrawerLayout.setDrawerLockMode(
            if (isDrawerLocked()) {
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            } else {
                DrawerLayout.LOCK_MODE_UNLOCKED
            }
        )
    }

    private fun isMenuLocked(): Boolean {
        return mState == STATE_LOGIN
    }

    private fun updateMenuLocked() {
        invalidateOptionsMenu()
    }

    private fun startLoadingAuthInfo() {
        val cached = gAuthInfoCache
        if (cached != null) {
            onAuthInfoLoaded(cached)
            return
        }

        val app = application

        MyGlobalScope.launch(SupervisorJob() + Dispatchers.Main) {
            val authInfo = withContext(Dispatchers.IO) {
                AuthInfo.loadSavedAuthInfo(app)
            }

            gAuthInfoCache = authInfo

            if (authInfo != null) {
                onAuthInfoLoaded(authInfo)
            } else {
                gAuthInfoCache = null
                onAuthInfoMissing()
            }
        }
    }

    private fun confirmLoggingOut() {
        val dialog = AlertDialog.Builder(this).apply {
            setTitle(R.string.please_confirm)
            setMessage(R.string.confirm_log_out)
            setPositiveButton(android.R.string.ok) { _, _ ->
                startLoggingOut()
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
            }
        }.create()

        dialog.show()
        mLogoutConfirmDialog = dialog
    }

    private fun startLoggingOut() {
        mState = STATE_LOGIN
        postUpdateState()

        gAuthInfoCache = null

        val app = application

        MyGlobalScope.launch(SupervisorJob() + Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                AuthInfo.clearSavedAuthInfo(app)
            }
        }
    }

    private fun onClickRefresh() {
        val fm = supportFragmentManager

        val master = fm.findFragmentById(R.id.master_content_frame)
                as NodeListFragment?
        master?.also {
            it.refresh()
        }

        val detail = fm.findFragmentById(R.id.detail_content_frame)
                as BaseTimeFragment?
        detail?.also {
            it.refresh()
        }
    }

    private fun onTimePeriodOption(itemId: Int): Boolean {

        for (i in TimePeriod.LIST) {
            if (i.itemId == itemId) {
                mMinutes = i.minutes

                val fm = supportFragmentManager

                val master = fm.findFragmentById(R.id.master_content_frame)
                        as NodeListFragment?
                master?.also {
                    it.setTimeMinutes(mMinutes)
                    it.refresh()
                }

                val detail = fm.findFragmentById(R.id.detail_content_frame)
                        as BaseTimeFragment?
                detail?.also {
                    it.setTimeMinutes(mMinutes)
                    it.refresh()
                }

                return true
            }
        }

        return false
    }

    private fun updateNavDrawerItems() {
        val menu = mNavView.menu
        val prefs = Prefs(this)
        menu.findItem(R.id.nav_app_apache).setVisible(prefs.mShowApache)
        menu.findItem(R.id.nav_app_nginx).setVisible(prefs.mShowNginx)
        menu.findItem(R.id.nav_app_mysql).setVisible(prefs.mShowMySQL)
        menu.findItem(R.id.nav_app_pgsql).setVisible(prefs.mShowPgSQL)
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val STATE_LOADING = 0
        private const val STATE_LOGIN = 1
        private const val STATE_SERVERS = 2
        private const val STATE_DETAIL = 3

        private const val WHAT_UPDATE_UI = 0

        private const val KEY_STATE = "state"
        private const val KEY_NODE_ID = "nodeId"
        private const val KEY_NODE_TITLE = "nodeTitle"
        private const val KEY_MINUTES = "minutes"

        private var gAuthInfoCache: AuthInfo? = null
    }

    private lateinit var mRefreshFab: FloatingActionButton
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mNavView: NavigationView
    private lateinit var mNavHeaderView: View
    private lateinit var mHandler: Handler

    private var mLogoutConfirmDialog: Dialog? = null

    // Model (state)
    private var mState: Int = STATE_LOADING
    private var mNodeId: String? = null
    private var mNodeTitle: String? = null
    private var mMinutes: Int = BaseTimeFragment.DEFAULT_MINUTES

    private var mIsTwoPanel = false
    private val mReqPrefs = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onResultPrefs
    )
}
