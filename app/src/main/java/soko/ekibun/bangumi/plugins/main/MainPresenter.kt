package soko.ekibun.bangumi.plugins.main

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.download_list.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil

class MainPresenter(val activity: Activity, val pluginContext: Context) {
    val app by lazy { App.from(pluginContext) }
    val proxy = ReflectUtil.proxyObject(activity, IMainActivity::class.java)!!
    private val nav_view = ReflectUtil.proxyObject(proxy.nav_view, IMainActivity.INavigationView::class.java)!!
    private val contentFrame = proxy.content_frame
    val view =
        LayoutInflater.from(pluginContext).inflate(R.layout.download_list, contentFrame, false) as SwipeRefreshLayout
    val adapter = CacheAdapter(activity)

    private fun updateData() {
        view.isRefreshing = false
        adapter.setNewData(app.videoCacheModel.getCacheList())
    }

    init {
        val emptyView =
            LayoutInflater.from(activity).inflate(ResourceUtil.getResId(activity, "layout", "view_empty"), null)
        adapter.emptyView = emptyView
        adapter.setOnItemClickListener { _, _, position ->
            activity.startActivity(AppUtil.parseSubjectActivityIntent(adapter.data[position].subject))
        }
        view.list_download.adapter = adapter
        view.list_download.layoutManager = LinearLayoutManager(pluginContext)
        view.setOnRefreshListener {
            updateData()
        }
        contentFrame.addView(view)
        view.visibility = View.INVISIBLE

        val menu =
            nav_view.menu.add(ResourceUtil.getId(activity, "group1"), Menu.FIRST, Menu.NONE, "离线缓存")
        menu.icon = ResourceUtil.getDrawable(pluginContext, R.drawable.ic_cloud_done)
        menu.isCheckable = true
        Log.v("plugin", "group ${menu.groupId}")
        val superListener = proxy.mainPresenter.drawerView.navigationItemSelectedListener
        proxy.mainPresenter.drawerView.navigationItemSelectedListener = {
            if (menu.itemId == it.itemId) {
                nav_view.setCheckedItem(it.itemId)
                view.visibility = View.VISIBLE
                contentFrame.bringChildToFront(view)
                activity.title = "离线缓存"
                updateData()
            } else {
                view.visibility = View.INVISIBLE
            }
            superListener(it)
        }
        val superBack = proxy.onBackListener
        proxy.onBackListener = {
            if (menu.isChecked) {
                proxy.mainPresenter.drawerView.select(ResourceUtil.getId(activity, "nav_home"))
                view.visibility = View.INVISIBLE
                true
            } else superBack()
        }
    }
}