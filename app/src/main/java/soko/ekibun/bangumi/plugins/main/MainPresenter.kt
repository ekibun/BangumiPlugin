package soko.ekibun.bangumi.plugins.main

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.download_list.view.*
import soko.ekibun.bangumi.plugins.App
import soko.ekibun.bangumi.plugins.PluginPresenter
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.bean.Subject
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.provider.book.BookProvider
import soko.ekibun.bangumi.plugins.util.AppUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil
import soko.ekibun.bangumi.plugins.util.ResourceUtil
import java.lang.ref.WeakReference

class MainPresenter(activityRef: WeakReference<Activity>) : PluginPresenter(activityRef) {
    val proxy = ReflectUtil.proxyObjectWeak(activityRef, IMainActivity::class.java)!!
    private val nav_view = ReflectUtil.proxyObject(proxy.nav_view, IMainActivity.INavigationView::class.java)!!
    private val contentFrame = proxy.content_frame

    val pluginContext = App.createThemeContext(activityRef)

    val view =
        LayoutInflater.from(pluginContext).inflate(R.layout.download_list, contentFrame, false) as SwipeRefreshLayout
    val adapter = CacheAdapter()

    private fun updateData() {
        view.isRefreshing = false
        adapter.setNewInstance(App.app.episodeCacheModel.getCacheList().toMutableList())
    }

    init {
        val emptyView =
            LayoutInflater.from(activityRef.get()!!)
                .inflate(ResourceUtil.getResId(App.app.host, "layout", "view_empty"), null)
        adapter.setEmptyView(emptyView)
        adapter.setOnItemClickListener { _, _, position ->
            activityRef.get()?.startActivity(AppUtil.parseSubjectActivityIntent(adapter.data[position].subject))
        }
        view.list_download.adapter = adapter
        view.list_download.layoutManager = LinearLayoutManager(pluginContext)
        view.setOnRefreshListener {
            updateData()
        }
        contentFrame.addView(view)
        view.visibility = View.INVISIBLE

        val menu =
            nav_view.menu.add(ResourceUtil.getId(App.app.host, "group1"), Menu.FIRST, Menu.NONE, "离线缓存")
        menu.icon = ResourceUtil.getDrawable(pluginContext, R.drawable.ic_cloud_done)
        menu.isCheckable = true

        val superListener = proxy.mainPresenter.drawerView.navigationItemSelectedListener
        proxy.mainPresenter.drawerView.navigationItemSelectedListener = {
            if (menu.itemId == it.itemId) {
                nav_view.setCheckedItem(it.itemId)
                view.visibility = View.VISIBLE
                contentFrame.bringChildToFront(view)
                activityRef.get()?.title = "离线缓存"
                updateData()
            } else {
                view.visibility = View.INVISIBLE
            }
            superListener(it)
        }
        val superBack = proxy.onBackListener
        proxy.onBackListener = {
            if (menu.isChecked) {
                proxy.mainPresenter.drawerView.select(ResourceUtil.getId(App.app.host, "nav_home"))
                view.visibility = View.INVISIBLE
                true
            } else superBack()
        }
        val airInfo = HashMap<String, List<BookProvider.AirInfo>>()
        val updateSubjectAitInfo = {
            var hitChange = false
            proxy.mainPresenter.collectionList.map {
                ReflectUtil.proxyObject(
                    it,
                    IMainActivity.IMainPresenter.ISubject::class.java
                )!!
            }.forEach { subject ->
                App.app.lineInfoModel.getInfos(Subject(id = subject.id))?.getDefaultProvider()?.let { line ->
                    airInfo[line.site]?.find { line.id == it.id }
                }?.let {
                    hitChange = true
                    Log.v("air", it.toString())
                    subject.airInfo = it.air
                }
            }
            hitChange
        }

        App.app.lineProvider.providerList.values.filter {
            it.type == Provider.TYPE_BOOK
        }.forEach { line ->
            val provider = line.provider as? BookProvider
            if (provider == null || provider.getUpdate.isNullOrEmpty()) return@forEach
            subscribeOnUiThread(
                provider.getUpdate("update_${line.site}", App.app.jsEngine),
                {
                    airInfo[line.site] = it
                    Log.v("air", it.toString())
                    if (updateSubjectAitInfo()) proxy.mainPresenter.notifyCollectionChange()
                }, {
                    Toast.makeText(App.app.host, "${line.site}: ${it.message}", Toast.LENGTH_SHORT).show()
                },
                key = "update_${line.site}"
            )
        }

        proxy.mainPresenter.mergeAirInfo = {
            updateSubjectAitInfo()
        }
    }
}